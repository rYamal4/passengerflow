package io.github.ryamal4.passengerflow.service.telegram;

import io.github.ryamal4.passengerflow.model.BotUserSettings;
import io.github.ryamal4.passengerflow.repository.BotUserSettingsRepository;
import io.github.ryamal4.passengerflow.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@ConditionalOnProperty(name = "telegram.bot.token")
public class TelegramBotService implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private static final String CMD_START = "/start";
    private static final String CMD_SUBSCRIBE = "/subscribe";
    private static final String CMD_UNSUBSCRIBE = "/unsubscribe";

    private final String botToken;
    private final TelegramClient telegramClient;
    private final TelegramNotificationService notificationService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final BotUserSettingsRepository botUserSettingsRepository;

    private final Map<Long, SubscriptionState> subscriptionStates = new ConcurrentHashMap<>();

    public TelegramBotService(
            @Value("${telegram.bot.token}") String botToken,
            TelegramClient telegramClient,
            TelegramNotificationService notificationService,
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            BotUserSettingsRepository botUserSettingsRepository) {
        this.botToken = botToken;
        this.telegramClient = telegramClient;
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

        if (subscriptionStates.containsKey(chatId)) {
            handleSubscriptionFlow(chatId, text);
            return;
        }

        if (text.startsWith(CMD_START) || text.startsWith(CMD_SUBSCRIBE)) {
            handleSubscribeCommand(chatId);
        } else if (text.startsWith(CMD_UNSUBSCRIBE)) {
            handleUnsubscribeCommand(chatId);
        } else {
            sendHelpMessage(chatId);
        }
    }

    private void handleSubscribeCommand(Long chatId) {
        if (botUserSettingsRepository.existsByTelegramChatId(chatId)) {
            notificationService.sendMessage(chatId, "Вы уже подписаны на уведомления.");
            return;
        }

        subscriptionStates.put(chatId, new SubscriptionState());
        notificationService.sendMessage(chatId, "Введите ваш логин в системе PassengerFlow:");
    }

    private void handleSubscriptionFlow(Long chatId, String text) {
        var state = subscriptionStates.get(chatId);

        if (state.username == null) {
            state.username = text;
            notificationService.sendMessage(chatId, "Введите ваш пароль:");
        } else {
            var password = text;
            completeSubscription(chatId, state.username, password);
            subscriptionStates.remove(chatId);
        }
    }

    private void completeSubscription(Long chatId, String username, String password) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (AuthenticationException e) {
            notificationService.sendMessage(chatId, "Неверный логин или пароль. Попробуйте снова: /subscribe");
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

        notificationService.sendMessage(chatId,
                "✅ Вы успешно подписались на уведомления о входе в аккаунт " + username);
    }

    private void handleUnsubscribeCommand(Long chatId) {
        var settingsOpt = botUserSettingsRepository.findByTelegramChatId(chatId);
        if (settingsOpt.isEmpty()) {
            notificationService.sendMessage(chatId, "Вы не подписаны на уведомления.");
            return;
        }

        botUserSettingsRepository.delete(settingsOpt.get());
        notificationService.sendMessage(chatId, "Вы отписались от уведомлений.");
    }

    private void sendHelpMessage(Long chatId) {
        var help = """
                Доступные команды:
                /subscribe - подписаться на уведомления о входе
                /unsubscribe - отписаться от уведомлений
                """;
        notificationService.sendMessage(chatId, help);
    }

    private static class SubscriptionState {
        String username;
    }
}
