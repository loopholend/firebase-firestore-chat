package com.example.chatapp.repository.firestore;

import com.example.chatapp.model.Message;
import com.example.chatapp.repository.MessageRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Repository;

@Repository
public class FirestoreMessageRepository implements MessageRepository {

    private final CollectionReference messagesCollection;

    public FirestoreMessageRepository(Firestore firestore) {
        this.messagesCollection = firestore.collection("messages");
    }

    @Override
    public Message save(Message message) {
        try {
            if (message.getId() == null || message.getId().isBlank()) {
                DocumentReference documentReference = messagesCollection.document();
                message.setId(documentReference.getId());
                documentReference.set(message).get();
            } else {
                messagesCollection.document(message.getId()).set(message).get();
            }
            return message;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while saving message", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to save message", e);
        }
    }

    @Override
    public List<Message> findByUserId(String userId) {
        try {
            ApiFuture<QuerySnapshot> sentFuture = messagesCollection.whereEqualTo("senderId", userId).get();
            ApiFuture<QuerySnapshot> receivedFuture = messagesCollection.whereEqualTo("receiverId", userId).get();

            List<Message> messages = new ArrayList<>();
            messages.addAll(mapMessages(sentFuture.get().getDocuments()));
            messages.addAll(mapMessages(receivedFuture.get().getDocuments()));
            messages.sort(Comparator.comparingLong(Message::getTimestamp).reversed());
            return messages;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading messages", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to load messages", e);
        }
    }

    @Override
    public Optional<Message> findById(String id) {
        try {
            DocumentSnapshot snapshot = messagesCollection.document(id).get().get();
            if (!snapshot.exists()) {
                return Optional.empty();
            }
            Message message = snapshot.toObject(Message.class);
            if (message != null) {
                message.setId(snapshot.getId());
            }
            return Optional.ofNullable(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading message", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to load message", e);
        }
    }

    private List<Message> mapMessages(List<? extends DocumentSnapshot> documents) {
        List<Message> messages = new ArrayList<>();
        for (DocumentSnapshot document : documents) {
            Message message = document.toObject(Message.class);
            if (message != null) {
                message.setId(document.getId());
                messages.add(message);
            }
        }
        messages.sort(Comparator.comparingLong(Message::getTimestamp).reversed());
        return messages;
    }
}
