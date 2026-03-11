package com.example.chatapp.web;

import jakarta.servlet.http.HttpSession;

public final class SessionUser {

    private static final String SESSION_USER_ID = "loggedInUserId";

    private SessionUser() {
    }

    public static void store(HttpSession session, String userId) {
        session.setAttribute(SESSION_USER_ID, userId);
    }

    public static String getUserId(HttpSession session) {
        Object value = session.getAttribute(SESSION_USER_ID);
        return value instanceof String ? (String) value : null;
    }

    public static void clear(HttpSession session) {
        session.invalidate();
    }
}
