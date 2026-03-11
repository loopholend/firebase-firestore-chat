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
import org.springframework.util.StringUtils;

@Configuration
public class FirebaseConfig {

    @Bean
    public Firestore firestore() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream classpathResource = FirebaseConfig.class.getClassLoader()
                    .getResourceAsStream("firebase-key.json");
            InputStream serviceAccount = classpathResource;
            if (classpathResource == null) {
                String firebaseKeyJson = System.getenv("FIREBASE_KEY_JSON");
                if (!StringUtils.hasText(firebaseKeyJson)) {
                    throw new IllegalStateException("firebase-key.json was not found on the classpath and FIREBASE_KEY_JSON is not set.");
                }
                serviceAccount = new ByteArrayInputStream(firebaseKeyJson.getBytes(StandardCharsets.UTF_8));
            }
            try (InputStream stream = serviceAccount) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();
                FirebaseApp.initializeApp(options);
            }
        }

        return FirestoreClient.getFirestore();
    }
}
