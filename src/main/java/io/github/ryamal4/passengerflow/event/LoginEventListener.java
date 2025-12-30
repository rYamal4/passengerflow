package io.github.ryamal4.passengerflow.event;

import io.github.ryamal4.passengerflow.service.telegram.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(TelegramNotificationService.class)
public class LoginEventListener {
    private final TelegramNotificationService notificationService;

    @Async
    @EventListener
    public void handleLoginEvent(LoginEvent event) {
        log.debug("Login event received for user: {}", event.user().getUsername());
        notificationService.sendLoginNotification(event.user(), event.timestamp());
    }
}
