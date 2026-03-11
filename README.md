# Firebase Firestore Spring Boot API

This project is a Spring Boot Maven application that uses the Firebase Admin SDK to connect to Firestore.

## Firebase configuration

`FirebaseConfig` initializes Firebase using this service account JSON file:

`C:\Users\Pranjal Pal\Downloads\chat-app-1e086-firebase-adminsdk-fbsvc-7dce8e839d.json`

## Dependency

The project uses:

- `com.google.firebase:firebase-admin:9.2.0`

## API endpoints

### `POST /register`

Request body:

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "secret123"
}
```

Stores the user in Firestore collection `users`.

### `POST /login`

Request body:

```json
{
  "email": "alice@example.com",
  "password": "secret123"
}
```

Checks the credentials against Firestore. On success, returns the list of all users.

### `GET /users`

Returns all users from Firestore collection `users`.

### `POST /sendMessage`

Request body:

```json
{
  "senderId": "user-id-1",
  "receiverId": "user-id-2",
  "text": "Hello"
}
```

Stores the message in Firestore collection `messages`.

### `GET /messages/{userId}`

Returns all messages sent or received by the given user.

## Models

- `User`: `id`, `username`, `email`, `password`
- `Message`: `id`, `senderId`, `receiverId`, `text`, `timestamp`

## Build and run

Requirements:

- Java 17
- Maven 3.9+

Build:

```bash
mvn clean package
```

Run:

```bash
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.
