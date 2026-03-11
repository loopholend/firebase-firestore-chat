package com.example.chatapp.service;

import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(User user) {
        String username = user.getUsername().trim();
        String email = user.getEmail().trim().toLowerCase();
        String password = user.getPassword().trim();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Password is required.");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(password);
        return userRepository.save(newUser);
    }

    public Optional<User> login(String email, String password) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email.trim().toLowerCase())
                .filter(user -> password.equals(user.getPassword()));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> findAllOtherUsers(String currentUserId) {
        return userRepository.findAll().stream()
                .filter(user -> isValidUser(user, currentUserId))
                .toList();
    }

    public List<User> findConversationContacts(String currentUserId, Collection<String> contactIds) {
        if (contactIds == null || contactIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findAll().stream()
                .filter(user -> contactIds.contains(user.getId()))
                .filter(user -> isValidUser(user, currentUserId))
                .sorted((left, right) -> left.getUsername().compareToIgnoreCase(right.getUsername()))
                .toList();
    }

    public List<User> searchUsers(String currentUserId, String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        return userRepository.findAll().stream()
                .filter(user -> isValidUser(user, currentUserId))
                .filter(user -> user.getUsername().toLowerCase().contains(normalizedQuery))
                .sorted((left, right) -> left.getUsername().compareToIgnoreCase(right.getUsername()))
                .toList();
    }

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    private boolean isValidUser(User user, String currentUserId) {
        return user != null
                && StringUtils.hasText(user.getId())
                && !user.getId().equals(currentUserId)
                && StringUtils.hasText(user.getUsername())
                && StringUtils.hasText(user.getEmail());
    }
}
