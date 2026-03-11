package com.example.chatapp.repository.firestore;

import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Repository;

@Repository
public class FirestoreUserRepository implements UserRepository {

    private final CollectionReference usersCollection;

    public FirestoreUserRepository(Firestore firestore) {
        this.usersCollection = firestore.collection("users");
    }

    @Override
    public User save(User user) {
        try {
            if (user.getId() == null || user.getId().isBlank()) {
                DocumentReference documentReference = usersCollection.document();
                user.setId(documentReference.getId());
                documentReference.set(user).get();
            } else {
                usersCollection.document(user.getId()).set(user).get();
            }
            return user;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while saving user", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to save user", e);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        try {
            DocumentSnapshot snapshot = usersCollection.document(id).get().get();
            if (!snapshot.exists()) {
                return Optional.empty();
            }
            User user = snapshot.toObject(User.class);
            if (user != null) {
                user.setId(snapshot.getId());
            }
            return Optional.ofNullable(user);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading user", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to load user", e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return findSingleByField("email", email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findSingleByField("username", username);
    }

    @Override
    public List<User> findAll() {
        try {
            ApiFuture<QuerySnapshot> future = usersCollection.get();
            List<? extends DocumentSnapshot> documents = future.get().getDocuments();
            List<User> users = new ArrayList<>();
            for (DocumentSnapshot document : documents) {
                User user = document.toObject(User.class);
                if (user != null) {
                    user.setId(document.getId());
                    users.add(user);
                }
            }
            users.sort((left, right) -> left.getUsername().compareToIgnoreCase(right.getUsername()));
            return users;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading users", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to load users", e);
        }
    }

    private Optional<User> findSingleByField(String field, String value) {
        try {
            ApiFuture<QuerySnapshot> future = usersCollection.whereEqualTo(field, value).limit(1).get();
            List<? extends DocumentSnapshot> documents = future.get().getDocuments();
            if (documents.isEmpty()) {
                return Optional.empty();
            }
            DocumentSnapshot document = documents.get(0);
            User user = document.toObject(User.class);
            if (user != null) {
                user.setId(document.getId());
            }
            return Optional.ofNullable(user);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while querying users", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to query users", e);
        }
    }
}
