package com.example.chatapp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class FirebaseConfig {

    private static final String DEFAULT_SERVICE_ACCOUNT_PATH =
            "C:\\Users\\Pranjal Pal\\Downloads\\chat-app-1e086-firebase-adminsdk-fbsvc-7dce8e839d.json";

    @Bean
    public Firestore firestore(@Value("${firebase.credentials.path:}") String credentialsPath) throws IOException {
        String resolvedPath = StringUtils.hasText(credentialsPath) ? credentialsPath : DEFAULT_SERVICE_ACCOUNT_PATH;
        if (FirebaseApp.getApps().isEmpty()) {
            try (FileInputStream serviceAccount = new FileInputStream(resolvedPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
            }
        }

        return FirestoreClient.getFirestore();
    }
}
