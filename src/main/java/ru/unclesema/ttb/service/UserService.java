package ru.unclesema.ttb.service;

import org.springframework.stereotype.Service;
import ru.unclesema.ttb.User;
import ru.unclesema.ttb.strategy.CandleStrategy;
import ru.unclesema.ttb.strategy.OrderBookStrategy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {
    private final Set<User> allUsers = new HashSet<>();
    private final Set<User> activeCandleUsers = new HashSet<>();
    private final Set<User> activeOrderBookUsers = new HashSet<>();

    public void addUser(User user) {
        allUsers.add(user);
    }

    public void removeUser(User user) {
        allUsers.remove(user);
    }

    public List<User> getAllUsers() {
        return allUsers.stream().toList();
    }

    public User findUserByAccountId(String accountId) {
        var optionalUser = allUsers.stream().filter(user -> user.accountId().equals(accountId)).findAny();
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("Пользователь " + accountId + " не найден");
        }
        return optionalUser.get();
    }

    public boolean isActive(User user) {
        return activeCandleUsers.contains(user) || activeOrderBookUsers.contains(user);
    }

    public void makeUserActive(User user) {
        if (user.strategy() instanceof OrderBookStrategy) {
            activeOrderBookUsers.add(user);
        }
        if (user.strategy() instanceof CandleStrategy) {
            activeCandleUsers.add(user);
        }
    }

    public void makeUserInactive(User user) {
        activeCandleUsers.remove(user);
        activeOrderBookUsers.remove(user);
    }

    public Set<User> getActiveCandleUsers() {
        return activeCandleUsers;
    }

    public Set<User> getActiveOrderBookUsers() {
        return activeOrderBookUsers;
    }

    public boolean contains(String accountId) {
        var optionalUser = allUsers.stream().filter(user -> user.accountId().equals(accountId)).findAny();
        return optionalUser.isPresent();
    }
}
