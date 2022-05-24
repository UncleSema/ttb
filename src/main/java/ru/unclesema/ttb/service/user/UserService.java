package ru.unclesema.ttb.service.user;

import ru.unclesema.ttb.model.User;

import java.util.List;
import java.util.Set;

/**
 * Сервис отвечающий за работу с пользователями.
 *
 * <p> Пользователь становится активным, когда он включает для себя стратегию, и перестаёт быть таким, когда выключает её. </p>
 */
public interface UserService {
    /**
     * Добавить нового пользователя
     */
    void addUser(User user);

    /**
     * Получить всех пользователей
     */
    List<User> getAllUsers();

    /**
     * Получить пользователя по AccountID
     *
     * @throws IllegalArgumentException если пользователь не найден
     */
    User findUserByAccountId(String accountId);

    /**
     * Проверка на то, что пользователь активный
     */
    boolean isActive(User user);

    /**
     * Сделать пользователя активным
     */
    void makeUserActive(User user);

    /**
     * Сделать пользователя неактивным
     */
    void makeUserInactive(User user);

    /**
     * Получить всех активных пользователей, у которых стратегия использует свечи.
     */
    Set<User> getActiveCandleUsers();

    /**
     * Получить всех активных пользователей, у которых стратегия использует стакан.
     */
    Set<User> getActiveOrderBookUsers();

    /**
     * Проверка на то, что пользователь с данным <code>accountId</code> существует.
     */
    boolean contains(String accountId);
}
