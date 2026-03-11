#!/usr/bin/env bash
set -euo pipefail

# run this from repo root
# usage: ./deploy-fix.sh

echo "Applying permanent Render-ready fixes..."

# 1) Ensure directories exist
mkdir -p src/main/resources
mkdir -p src/main/java/com/example/chatapp/config

# 2) application.properties (server.port + firebase toggle)
cat > src/main/resources/application.properties <<'EOF'
# Use the platform port (Render sets PORT). Default to 8080 locally.
server.port=${PORT:8080}

# Enable/disable firebase integration. Set to false to run without firebase.
firebase.enabled=true
EOF

echo "Wrote src/main/resources/application.properties"

# 3) Dockerfile (multi-stage build)
cat > Dockerfile <<'EOF'
# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -e -B clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
EOF

echo "Wrote Dockerfile"

# 4) .gitignore (ignore firebase key)
if ! grep -q "src/main/resources/firebase-key.json" .gitignore 2>/dev/null; then
  cat >> .gitignore <<'EOF'

# Firebase service account (do NOT commit)
src/main/resources/firebase-key.json
EOF
  echo "Appended firebase-key.json to .gitignore"
else
  echo ".gitignore already configured"
fi

# 5) FirebaseConfig.java (robust loader)
cat > src/main/java/com/example/chatapp/config/FirebaseConfig.java <<'EOF'
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {
    private final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    /**
     * Creates a Firestore bean.
     * Loading order:
     * 1) /etc/secrets/firebase-key.json  (Render Secret File recommended)
     * 2) FIREBASE_KEY_JSON environment variable (contains full JSON)
     * 3) classpath resource firebase-key.json (dev only - do NOT commit)
     */
    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
    public Firestore firestore() throws Exception {
        InputStream serviceAccountStream = null;

        // 1) Try secret file
        File secretFile = new File("/etc/secrets/firebase-key.json");
        if (secretFile.exists()) {
            logger.info("Loading Firebase credentials from secret file: /etc/secrets/firebase-key.json");
            serviceAccountStream = new FileInputStream(secretFile);
        } else {
            // 2) Try environment variable
            String firebaseJson = System.getenv("FIREBASE_KEY_JSON");
            if (firebaseJson != null && !firebaseJson.isBlank()) {
                logger.info("Loading Firebase credentials from env var FIREBASE_KEY_JSON");
                serviceAccountStream = new ByteArrayInputStream(firebaseJson.getBytes(StandardCharsets.UTF_8));
            } else {
                // 3) Try classpath resource (dev fallback)
                serviceAccountStream = FirebaseConfig.class.getClassLoader().getResourceAsStream("firebase-key.json");
                if (serviceAccountStream != null) {
                    logger.info("Loading Firebase credentials from classpath resource firebase-key.json");
                }
            }
        }

        if (serviceAccountStream == null) {
            throw new IllegalStateException("Firebase credentials not found. Provide /etc/secrets/firebase-key.json or FIREBASE_KEY_JSON env var.");
        }

        // read all bytes so we can parse project_id and build credentials from same JSON
        byte[] jsonBytes = serviceAccountStream.readAllBytes();
        serviceAccountStream.close();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonBytes);
        String projectId = root.path("project_id").asText(null);
        if (projectId == null || projectId.isBlank()) {
            projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("Firebase service account JSON does not contain project_id and GOOGLE_CLOUD_PROJECT is not set.");
        }

        InputStream credsStream = new ByteArrayInputStream(jsonBytes);
        GoogleCredentials credentials = GoogleCredentials.fromStream(credsStream);
        credsStream.close();

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

        // Build Firestore options with explicit projectId and credentials
        return com.google.cloud.firestore.FirestoreOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build()
                .getService();
    }
}
EOF

echo "Wrote src/main/java/com/example/chatapp/config/FirebaseConfig.java"

# 6) Stage changes and commit
git add Dockerfile src/main/resources/application.properties src/main/java/com/example/chatapp/config/FirebaseConfig.java .gitignore

git commit -m "Prepare Render deployment: Dockerfile, server.port, robust Firebase credential loader, ignore firebase key" || {
  echo "Nothing to commit or commit failed. Continuing."
}

# 7) Push
git push origin main

echo "Pushed changes to origin/main. NEXT: rotate key in Firebase, upload new key to Render Secret Files, then redeploy from Render UI."
echo "Manual steps reminder:"
echo "  1) Firebase Console -> Project Settings -> Service accounts -> delete old leaked key -> generate new key (download JSON)."
echo "  2) Render dashboard -> your service -> Environment -> Secret Files -> Add -> upload the new JSON as firebase-key.json"
echo "  3) Render dashboard -> Manual Deploy -> Deploy latest commit"
echo
echo "If you prefer automation for uploading the secret, you must provide a Render API key and service id (I can provide an optional curl-based snippet if you give those values)."
