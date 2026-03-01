package com.automatedtest.platform.common;

import com.automatedtest.platform.entity.User;

public class UserContext {
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();
    private static final ThreadLocal<Integer> currentProjectId = new ThreadLocal<>();

    public static void setCurrentUser(User user) {
        currentUser.set(user);
    }

    public static User getCurrentUser() {
        return currentUser.get();
    }

    public static void setCurrentProjectId(Integer projectId) {
        currentProjectId.set(projectId);
    }

    public static Integer getCurrentProjectId() {
        return currentProjectId.get();
    }

    public static void clear() {
        currentUser.remove();
        currentProjectId.remove();
    }
}
