package ru.unclesema.ttb.service;

import org.springframework.stereotype.Service;
import ru.unclesema.ttb.User;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final HashSet<User> users = new HashSet<>();

    public void addUser(User user) {
        users.add(user);
    }

    public void removeUser(User user) {
        users.remove(user);
    }

    public List<User> getAllUsers() {
        return users.stream().toList();
    }

    public Optional<User> findUserByHash(Integer userHash) {
        return users.stream().filter(user -> userHash == user.hashCode()).findAny();
    }
}
