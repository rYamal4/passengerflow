package io.github.ryamal4.passengerflow.service.telegram;

import io.github.ryamal4.passengerflow.enums.NotificationType;
import io.github.ryamal4.passengerflow.model.User;
import io.github.ryamal4.passengerflow.repository.BotUserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "telegram.bot.token")
public class TelegramNotificationService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final TelegramClient telegramClient;
    private final BotUserSettingsRepository botUserSettingsRepository;

    public void sendLoginNotification(User user, LocalDateTime timestamp) {
        var settingsOpt = botUserSettingsRepository.findByUser(user);
        if (settingsOpt.isEmpty()) {
            return;
        }

        var settings = settingsOpt.get();
        if (!settings.getEnabledNotifications().contains(NotificationType.LOGIN)) {
            return;
        }

        var message = formatLoginMessage(user.getUsername(), timestamp);
        sendMessage(settings.getTelegramChatId(), message);
    }

    public void sendMessage(Long chatId, String text) {
        var message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message to chat {}: {}", chatId, e.getMessage());
        }
    }

    private String formatLoginMessage(String username, LocalDateTime timestamp) {
        return """
                🔐 Вход в систему PassengerFlow

                Пользователь: %s
                Время: %s
                """.formatted(username, timestamp.format(FORMATTER));
    }
}
