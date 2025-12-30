package io.github.ryamal4.passengerflow.model;

import io.github.ryamal4.passengerflow.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.util.EnumSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "bot_user_settings")
public class BotUserSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private Long telegramChatId;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "bot_user_notifications", joinColumns = @JoinColumn(name = "settings_id"))
    @Column(name = "notification_type")
    @Builder.Default
    private Set<NotificationType> enabledNotifications = EnumSet.of(NotificationType.LOGIN);
}
