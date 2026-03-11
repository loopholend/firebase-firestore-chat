package com.example.chatapp.controller;

import com.example.chatapp.model.User;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.UserService;
import com.example.chatapp.web.SessionUser;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    private final UserService userService;
    private final MessageService messageService;

    public HomeController(UserService userService, MessageService messageService) {
        this.userService = userService;
        this.messageService = messageService;
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Set<String> contactIds = messageService.getConversationContactIds(currentUser.getId());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("users", userService.findConversationContacts(currentUser.getId(), contactIds));
        model.addAttribute("unreadUserIds", messageService.getUnreadSenderIds(currentUser.getId()));
        return "home";
    }

    @GetMapping("/users/search")
    @ResponseBody
    public List<Map<String, Object>> searchUsers(@RequestParam(name = "q", required = false) String query,
                                                 HttpSession session) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return List.of();
        }

        Set<String> unreadUserIds = messageService.getUnreadSenderIds(currentUser.getId());
        return userService.searchUsers(currentUser.getId(), query).stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "hasUnread", unreadUserIds.contains(user.getId())))
                .toList();
    }

    private User getCurrentUser(HttpSession session) {
        String userId = SessionUser.getUserId(session);
        if (userId == null) {
            return null;
        }
        return userService.findById(userId).orElse(null);
    }
}
