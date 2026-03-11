package com.example.chatapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
    public Firestore firestore() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            File secretFile = new File("/etc/secrets/firebase-key.json");
            InputStream serviceAccount = null;

            if (secretFile.exists()) {
                serviceAccount = new FileInputStream(secretFile);
                logger.info("Loading Firebase credentials from /etc/secrets/firebase-key.json");
            } else {
                String firebaseJson = System.getenv("FIREBASE_KEY_JSON");
                if (firebaseJson != null && !firebaseJson.isBlank()) {
                    serviceAccount = new ByteArrayInputStream(firebaseJson.getBytes(StandardCharsets.UTF_8));
                    logger.info("Loading Firebase credentials from FIREBASE_KEY_JSON env var");
                } else {
                    serviceAccount = FirebaseConfig.class.getClassLoader().getResourceAsStream("firebase-key.json");
                    if (serviceAccount != null) {
                        logger.info("Loading Firebase credentials from classpath resource firebase-key.json");
                    }
                }
            }

            if (serviceAccount == null) {
                throw new IllegalStateException("Firebase credentials not found. Provide /etc/secrets/firebase-key.json or FIREBASE_KEY_JSON environment variable.");
            }

            try (InputStream stream = serviceAccount) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                FirebaseApp.initializeApp(options);
            }
        }

        return FirestoreOptions.getDefaultInstance().getService();
    }
}
