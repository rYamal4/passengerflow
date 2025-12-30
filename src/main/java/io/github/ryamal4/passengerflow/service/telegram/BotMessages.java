package io.github.ryamal4.passengerflow.service.telegram;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotMessages {

    public static final String WELCOME = """
            Добро пожаловать в PassengerFlow Bot!

            Для начала работы войдите в систему:
            /login - войти в аккаунт
            """;

    public static final String HELP_AUTHORIZED = """
            Доступные команды:
            /subscribe_login - подписаться на уведомления о входе
            /unsubscribe_login - отписаться от уведомлений о входе
            /logout - выйти из аккаунта
            """;

    public static final String ENTER_LOGIN = "Введите ваш логин:";
    public static final String ENTER_PASSWORD = "Введите ваш пароль:";
    public static final String INVALID_CREDENTIALS = "Неверный логин или пароль. Попробуйте снова: /login";
    public static final String USER_NOT_FOUND = "Пользователь не найден.";
    public static final String LOGIN_REQUIRED = "Сначала войдите: /login";
    public static final String LOGOUT_SUCCESS = "Вы вышли из аккаунта.";
    public static final String NOT_AUTHORIZED = "Вы не авторизованы. Используйте /login для входа.";

    public static final String ALREADY_SUBSCRIBED_LOGIN = "Вы уже подписаны на уведомления о входе.";
    public static final String NOT_SUBSCRIBED_LOGIN = "Вы не подписаны на уведомления о входе.";
    public static final String SUBSCRIBED_LOGIN_SUCCESS = "✅ Вы подписались на уведомления о входе.";
    public static final String UNSUBSCRIBED_LOGIN_SUCCESS = "Вы отписались от уведомлений о входе.";

    public static final String LOGIN_NOTIFICATION = """
            🔐 Вход в систему PassengerFlow

            Пользователь: %s
            Время: %s
            """;

    public static String loggedInAs(String username) {
        return "Вы вошли как " + username + "\n\n" + HELP_AUTHORIZED;
    }

    public static String alreadyLoggedIn(String username) {
        return "Вы уже вошли как " + username + ". Используйте /logout для выхода.";
    }

    public static String loginSuccess(String username) {
        return "✅ Вы вошли как " + username + "\n\n" + HELP_AUTHORIZED;
    }
}
