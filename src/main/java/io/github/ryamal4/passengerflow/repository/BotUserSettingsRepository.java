package io.github.ryamal4.passengerflow.repository;

import io.github.ryamal4.passengerflow.model.BotUserSettings;
import io.github.ryamal4.passengerflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BotUserSettingsRepository extends JpaRepository<BotUserSettings, Long> {
    Optional<BotUserSettings> findByUser(User user);

    Optional<BotUserSettings> findByTelegramChatId(Long telegramChatId);

    boolean existsByTelegramChatId(Long telegramChatId);
}
