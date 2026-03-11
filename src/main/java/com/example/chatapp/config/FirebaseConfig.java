package com.example.chatapp.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
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
        InputStream serviceAccountStream = null;

        File secretFile = new File("/etc/secrets/firebase-key.json");
        if (secretFile.exists()) {
            logger.info("Loading Firebase credentials from secret file: /etc/secrets/firebase-key.json");
            serviceAccountStream = new FileInputStream(secretFile);
        } else {
            String firebaseJson = System.getenv("FIREBASE_KEY_JSON");
            if (firebaseJson != null && !firebaseJson.isBlank()) {
                logger.info("Loading Firebase credentials from env var FIREBASE_KEY_JSON");
                serviceAccountStream =
                        new ByteArrayInputStream(firebaseJson.getBytes(StandardCharsets.UTF_8));
            } else {
                serviceAccountStream =
                        FirebaseConfig.class.getClassLoader().getResourceAsStream("firebase-key.json");
                if (serviceAccountStream != null) {
                    logger.info("Loading Firebase credentials from classpath resource firebase-key.json");
                }
            }
        }

        if (serviceAccountStream == null) {
            throw new IllegalStateException(
                    "Firebase credentials not found. Provide /etc/secrets/firebase-key.json or FIREBASE_KEY_JSON env var.");
        }

        byte[] jsonBytes;
        try (InputStream stream = serviceAccountStream) {
            jsonBytes = stream.readAllBytes();
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonBytes);
        String projectId = root.path("project_id").asText(null);
        if (projectId == null || projectId.isBlank()) {
            projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException(
                    "Firebase service account JSON does not contain project_id and GOOGLE_CLOUD_PROJECT is not set.");
        }

        GoogleCredentials credentials;
        try (InputStream credsStream = new ByteArrayInputStream(jsonBytes)) {
            credentials = GoogleCredentials.fromStream(credsStream);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
            logger.info("Initialized FirebaseApp with projectId={}", projectId);
        } else {
            logger.info("FirebaseApp already initialized, skipping initialization.");
        }

        return com.google.cloud.firestore.FirestoreOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build()
                .getService();
    }
}
