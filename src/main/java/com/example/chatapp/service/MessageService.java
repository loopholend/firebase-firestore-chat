package com.example.chatapp.service;

import com.example.chatapp.model.Message;
import com.example.chatapp.repository.MessageRepository;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message sendMessage(Message message) {
        if (!StringUtils.hasText(message.getSenderId())) {
            throw new IllegalArgumentException("senderId is required.");
        }
        if (!StringUtils.hasText(message.getReceiverId())) {
            throw new IllegalArgumentException("receiverId is required.");
        }
        if (!StringUtils.hasText(message.getText())) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }

        Message newMessage = new Message();
        newMessage.setSenderId(message.getSenderId().trim());
        newMessage.setReceiverId(message.getReceiverId().trim());
        newMessage.setText(message.getText().trim());
        newMessage.setTimestamp(message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis());
        newMessage.setRead(false);
        return messageRepository.save(newMessage);
    }

    public Message sendMessage(String senderId, String receiverId, String text) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setText(text);
        return sendMessage(message);
    }

    public List<Message> getMessagesForUser(String userId) {
        List<Message> messages = messageRepository.findByUserId(userId);
        return messages != null ? messages : Collections.emptyList();
    }

    public List<Message> getConversation(String firstUserId, String secondUserId) {
        List<Message> messages = getMessagesForUser(firstUserId);
        return messages.stream()
                .filter(Objects::nonNull)
                .filter(message ->
                        (Objects.equals(message.getSenderId(), firstUserId) && Objects.equals(message.getReceiverId(), secondUserId)) ||
                        (Objects.equals(message.getSenderId(), secondUserId) && Objects.equals(message.getReceiverId(), firstUserId)))
                .sorted((left, right) -> Long.compare(left.getTimestamp(), right.getTimestamp()))
                .toList();
    }

    public Set<String> getConversationContactIds(String currentUserId) {
        return getMessagesForUser(currentUserId).stream()
                .filter(Objects::nonNull)
                .map(message -> Objects.equals(message.getSenderId(), currentUserId) ? message.getReceiverId() : message.getSenderId())
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    public Set<String> getUnreadSenderIds(String currentUserId) {
        return getMessagesForUser(currentUserId).stream()
                .filter(Objects::nonNull)
                .filter(message -> Objects.equals(message.getReceiverId(), currentUserId))
                .filter(message -> !message.isRead())
                .map(Message::getSenderId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    public void markConversationAsRead(String currentUserId, String otherUserId) {
        getMessagesForUser(currentUserId).stream()
                .filter(Objects::nonNull)
                .filter(message -> Objects.equals(message.getReceiverId(), currentUserId))
                .filter(message -> Objects.equals(message.getSenderId(), otherUserId))
                .filter(message -> !message.isRead())
                .forEach(message -> {
                    message.setRead(true);
                    messageRepository.save(message);
                });
    }

    public Optional<Message> findById(String messageId) {
        return messageRepository.findById(messageId);
    }
}
