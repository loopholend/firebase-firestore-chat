package com.example.chatapp.repository;

import com.example.chatapp.model.Message;
import java.util.List;
import java.util.Optional;

public interface MessageRepository {

    Message save(Message message);

    List<Message> findByUserId(String userId);

    Optional<Message> findById(String id);
}
