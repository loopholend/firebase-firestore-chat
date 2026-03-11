package com.example.chatapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {

    @Bean
    public Firestore firestore() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            String firebaseKey = System.getenv("FIREBASE_KEY_JSON");
            if (firebaseKey == null || firebaseKey.isBlank()) {
                throw new IllegalStateException("FIREBASE_KEY_JSON is not set.");
            }

            InputStream serviceAccount =
                    new ByteArrayInputStream(firebaseKey.getBytes(StandardCharsets.UTF_8));

            try (serviceAccount) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
            }
        }

        return FirestoreClient.getFirestore();
    }
}
