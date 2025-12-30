package io.github.ryamal4.passengerflow.service.telegram;

import io.github.ryamal4.passengerflow.enums.NotificationType;
import io.github.ryamal4.passengerflow.model.BotUserSettings;
import io.github.ryamal4.passengerflow.repository.BotUserSettingsRepository;
import io.github.ryamal4.passengerflow.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@ConditionalOnExpression("!'${telegram.bot.token:}'.isEmpty()")
public class TelegramBotService implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private static final String CMD_START = "/start";
    private static final String CMD_LOGIN = "/login";
    private static final String CMD_LOGOUT = "/logout";
    private static final String CMD_SUBSCRIBE_LOGIN = "/subscribe_login";
    private static final String CMD_UNSUBSCRIBE_LOGIN = "/unsubscribe_login";

    private final String botToken;
    private final TelegramNotificationService notificationService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final BotUserSettingsRepository botUserSettingsRepository;

    private final Map<Long, LoginState> loginStates = new ConcurrentHashMap<>();

    public TelegramBotService(
            @Value("${telegram.bot.token}") String botToken,
            TelegramNotificationService notificationService,
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            BotUserSettingsRepository botUserSettingsRepository) {
        this.botToken = botToken;
        this.notificationService = notificationService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.botUserSettingsRepository = botUserSettingsRepository;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        log.info("Telegram bot started, running: {}", botSession.isRunning());
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        var chatId = update.getMessage().getChatId();
        var text = update.getMessage().getText().trim();

        if (loginStates.containsKey(chatId)) {
            handleLoginFlow(chatId, text);
            return;
        }

        var command = text.split(" ")[0];
        switch (command) {
            case CMD_START -> handleStartCommand(chatId);
            case CMD_LOGIN -> handleLoginCommand(chatId);
            case CMD_LOGOUT -> handleLogoutCommand(chatId);
            case CMD_SUBSCRIBE_LOGIN -> handleSubscribeLoginCommand(chatId);
            case CMD_UNSUBSCRIBE_LOGIN -> handleUnsubscribeLoginCommand(chatId);
            default -> handleUnknownCommand(chatId);
        }
    }

    private void handleStartCommand(Long chatId) {
        var settingsOpt = botUserSettingsRepository.findByTelegramChatId(chatId);
        if (settingsOpt.isPresent()) {
            var username = settingsOpt.get().getUser().getUsername();
            notificationService.sendMessage(chatId, "Вы вошли как " + username + "\n\n" + getAuthorizedHelp());
        } else {
            notificationService.sendMessage(chatId, getUnauthorizedHelp());
        }
    }

    private void handleLoginCommand(Long chatId) {
        var existingSettings = botUserSettingsRepository.findByTelegramChatId(chatId);
        if (existingSettings.isPresent()) {
            var username = existingSettings.get().getUser().getUsername();
            notificationService.sendMessage(chatId, "Вы уже вошли как " + username + ". Используйте /logout для выхода.");
            return;
        }

        loginStates.put(chatId, new LoginState());
        notificationService.sendMessage(chatId, "Введите ваш логин:");
    }

    private void handleLoginFlow(Long chatId, String text) {
        var state = loginStates.get(chatId);

        if (state.username == null) {
            state.username = text;
            notificationService.sendMessage(chatId, "Введите ваш пароль:");
        } else {
            completeLogin(chatId, state.username, text);
            loginStates.remove(chatId);
        }
    }

    private void completeLogin(Long chatId, String username, String password) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (AuthenticationException e) {
            notificationService.sendMessage(chatId, "Неверный логин или пароль. Попробуйте снова: /login");
            return;
        }

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            notificationService.sendMessage(chatId, "Пользователь не найден.");
            return;
        }

        var user = userOpt.get();

        var existingSettings = botUserSettingsRepository.findByUser(user);
        if (existingSettings.isPresent()) {
            var settings = existingSettings.get();
            settings.setTelegramChatId(chatId);
            botUserSettingsRepository.save(settings);
        } else {
            var settings = BotUserSettings.builder()
                    .user(user)
                    .telegramChatId(chatId)
                    .build();
            botUserSettingsRepository.save(settings);
        }

        notificationService.sendMessage(chatId, "✅ Вы вошли как " + username + "\n\n" + getAuthorizedHelp());
    }

    private void handleLogoutCommand(Long chatId) {
        var settingsOpt = botUserSettingsRepository.findByTelegramChatId(chatId);
        if (settingsOpt.isEmpty()) {
            notificationService.sendMessage(chatId, "Вы не авторизованы. Используйте /login для входа.");
            return;
        }

        botUserSettingsRepository.delete(settingsOpt.get());
        notificationService.sendMessage(chatId, "Вы вышли из аккаунта.");
    }

    private void handleSubscribeLoginCommand(Long chatId) {
        var settingsOpt = botUserSettingsRepository.findByTelegramChatId(chatId);
        if (settingsOpt.isEmpty()) {
            notificationService.sendMessage(chatId, "Сначала войдите: /login");
            return;
        }

        var settings = settingsOpt.get();
        if (settings.getEnabledNotifications().contains(NotificationType.LOGIN)) {
            notificationService.sendMessage(chatId, "Вы уже подписаны на уведомления о входе.");
            return;
        }

        settings.getEnabledNotifications().add(NotificationType.LOGIN);
        botUserSettingsRepository.save(settings);
        notificationService.sendMessage(chatId, "✅ Вы подписались на уведомления о входе.");
    }

    private void handleUnsubscribeLoginCommand(Long chatId) {
        var settingsOpt = botUserSettingsRepository.findByTelegramChatId(chatId);
        if (settingsOpt.isEmpty()) {
            notificationService.sendMessage(chatId, "Сначала войдите: /login");
            return;
        }

        var settings = settingsOpt.get();
        if (!settings.getEnabledNotifications().contains(NotificationType.LOGIN)) {
            notificationService.sendMessage(chatId, "Вы не подписаны на уведомления о входе.");
            return;
        }

        settings.getEnabledNotifications().remove(NotificationType.LOGIN);
        botUserSettingsRepository.save(settings);
        notificationService.sendMessage(chatId, "Вы отписались от уведомлений о входе.");
    }

    private void handleUnknownCommand(Long chatId) {
        var settingsOpt = botUserSettingsRepository.findByTelegramChatId(chatId);
        if (settingsOpt.isEmpty()) {
            notificationService.sendMessage(chatId, "Сначала войдите: /login");
        } else {
            notificationService.sendMessage(chatId, getAuthorizedHelp());
        }
    }

    private String getUnauthorizedHelp() {
        return """
                Добро пожаловать в PassengerFlow Bot!
                
                Для начала работы войдите в систему:
                /login - войти в аккаунт
                """;
    }

    private String getAuthorizedHelp() {
        return """
                Доступные команды:
                /subscribe_login - подписаться на уведомления о входе
                /unsubscribe_login - отписаться от уведомлений о входе
                /logout - выйти из аккаунта
                """;
    }

    private static class LoginState {
        String username;
    }
}
