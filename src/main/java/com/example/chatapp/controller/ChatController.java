package com.example.chatapp.controller;

import com.example.chatapp.model.User;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.UserService;
import com.example.chatapp.web.SessionUser;
import com.example.chatapp.web.dto.MessageForm;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChatController {

    private final UserService userService;
    private final MessageService messageService;

    public ChatController(UserService userService, MessageService messageService) {
        this.userService = userService;
        this.messageService = messageService;
    }

    @GetMapping("/chat/{recipientId}")
    public String chat(@PathVariable String recipientId, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }

        User recipient = userService.findById(recipientId).orElse(null);
        if (recipient == null || recipient.getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/home";
        }

        messageService.markConversationAsRead(currentUser.getId(), recipient.getId());

        MessageForm messageForm = new MessageForm();
        messageForm.setReceiverId(recipient.getId());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("recipient", recipient);
        model.addAttribute("conversation", messageService.getConversation(currentUser.getId(), recipient.getId()));
        model.addAttribute("messageForm", messageForm);
        return "chat";
    }

    @PostMapping("/sendMessage")
    public String sendMessage(@Valid @ModelAttribute("messageForm") MessageForm messageForm,
                              BindingResult bindingResult,
                              HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }

        User recipient = userService.findById(messageForm.getReceiverId()).orElse(null);
        if (recipient == null || recipient.getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/home";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("recipient", recipient);
            model.addAttribute("conversation", messageService.getConversation(currentUser.getId(), recipient.getId()));
            messageForm.setReceiverId(recipient.getId());
            model.addAttribute("messageForm", messageForm);
            return "chat";
        }

        try {
            messageService.sendMessage(currentUser.getId(), recipient.getId(), messageForm.getText());
            return "redirect:/chat/" + recipient.getId();
        } catch (IllegalArgumentException ex) {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("recipient", recipient);
            model.addAttribute("conversation", messageService.getConversation(currentUser.getId(), recipient.getId()));
            messageForm.setReceiverId(recipient.getId());
            model.addAttribute("messageForm", messageForm);
            model.addAttribute("error", ex.getMessage());
            return "chat";
        }
    }

    private User getCurrentUser(HttpSession session) {
        String userId = SessionUser.getUserId(session);
        if (userId == null) {
            return null;
        }
        return userService.findById(userId).orElse(null);
    }
}
