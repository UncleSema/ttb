package ru.unclesema.ttb.service.user;

import org.springframework.stereotype.Service;
import ru.unclesema.ttb.model.User;
import ru.unclesema.ttb.strategy.CandleStrategy;
import ru.unclesema.ttb.strategy.OrderBookStrategy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Сервис отвечающий за работу с пользователями.
 *
 * <p> Пользователь становится активным, когда он включает для себя стратегию, и перестаёт быть таким, когда выключает её. </p>
 */
@Service
public class UserServiceImpl implements UserService {
    private final Set<User> allUsers = new HashSet<>();
    private final Set<User> activeCandleUsers = new HashSet<>();
    private final Set<User> activeOrderBookUsers = new HashSet<>();

    /**
     * Добавить нового пользователя
     */
    @Override
    public void addUser(User user) {
        allUsers.add(user);
    }

    /**
     * Получить всех пользователей
     */
    @Override
    public List<User> getAllUsers() {
        return allUsers.stream().toList();
    }

    /**
     * Получить пользователя по AccountID
     *
     * @throws IllegalArgumentException если пользователь не найден
     */
    @Override
    public User findUserByAccountId(String accountId) {
        var optionalUser = allUsers.stream().filter(user -> user.accountId().equals(accountId)).findAny();
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("Пользователь " + accountId + " не найден");
        }
        return optionalUser.get();
    }

    /**
     * Проверка на то, что пользователь активный
     */
    @Override
    public boolean isActive(User user) {
        return activeCandleUsers.contains(user) || activeOrderBookUsers.contains(user);
    }

    /**
     * Сделать пользователя активным
     */
    @Override
    public void makeUserActive(User user) {
        if (user.strategy() instanceof OrderBookStrategy) {
            activeOrderBookUsers.add(user);
        }
        if (user.strategy() instanceof CandleStrategy) {
            activeCandleUsers.add(user);
        }
    }

    /**
     * Сделать пользователя неактивным
     */
    @Override
    public void makeUserInactive(User user) {
        activeCandleUsers.remove(user);
        activeOrderBookUsers.remove(user);
    }

    /**
     * Получить всех активных пользователей, у которых стратегия использует свечи.
     */
    @Override
    public Set<User> getActiveCandleUsers() {
        return activeCandleUsers;
    }

    /**
     * Получить всех активных пользователей, у которых стратегия использует стакан.
     */
    @Override
    public Set<User> getActiveOrderBookUsers() {
        return activeOrderBookUsers;
    }

    /**
     * Проверка на то, что пользователь с данным <code>accountId</code> существует.
     */
    @Override
    public boolean contains(String accountId) {
        var optionalUser = allUsers.stream().filter(user -> user.accountId().equals(accountId)).findAny();
        return optionalUser.isPresent();
    }
}
