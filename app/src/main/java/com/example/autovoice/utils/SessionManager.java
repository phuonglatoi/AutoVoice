package com.example.autovoice.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.example.autovoice.models.User;

public class SessionManager {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final String PREF_NAME = "AutoVoiceUserSession"; // Đổi tên nếu muốn
    private static final String TAG = "SessionManager_Debug";

    // Keys
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_EMAIL = "userEmail";
    private static final String KEY_USERNAME = "userName";
    private static final String KEY_SUBSCRIPTION_STATUS = "subscriptionStatus";
    private static final String KEY_IS_ADMIN = "isAdmin";
    private static final String KEY_PROFILE_IMAGE_LOCAL_PATH = "profileImageLocalPath";
    private static final String KEY_AUTH_PROVIDER = "authProvider";

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveUserSession(User user) {
        if (user == null) {
            Log.e(TAG, "User object is null in saveUserSession. Cannot save.");
            return;
        }
        Log.d(TAG, "Saving session for User ID: " + user.getId() +
                ", Email: " + user.getEmail() +
                ", AuthProvider: " + user.getAuthProvider() +
                ", ImagePath: " + user.getProfileImageLocalPath());

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, user.getId());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_SUBSCRIPTION_STATUS, user.getSubscriptionStatus());
        editor.putBoolean(KEY_IS_ADMIN, user.isAdmin());
        editor.putString(KEY_AUTH_PROVIDER, user.getAuthProvider());

        if (!TextUtils.isEmpty(user.getProfileImageLocalPath())) {
            editor.putString(KEY_PROFILE_IMAGE_LOCAL_PATH, user.getProfileImageLocalPath());
        } else {
            editor.remove(KEY_PROFILE_IMAGE_LOCAL_PATH); // Xóa nếu path rỗng/null
        }
        editor.apply(); // Dùng apply() để chạy bất đồng bộ
    }

    public User getLoggedInUser() {
        if (!isLoggedIn()) {
            Log.d(TAG, "No user logged in.");
            return null;
        }

        User user = new User();
        user.setId(sharedPreferences.getInt(KEY_USER_ID, 0)); // 0 là giá trị mặc định nếu không tìm thấy
        user.setEmail(sharedPreferences.getString(KEY_EMAIL, null));
        user.setUsername(sharedPreferences.getString(KEY_USERNAME, null));
        user.setSubscriptionStatus(sharedPreferences.getString(KEY_SUBSCRIPTION_STATUS, "Free")); // Mặc định là Free
        user.setAdmin(sharedPreferences.getBoolean(KEY_IS_ADMIN, false));
        user.setAuthProvider(sharedPreferences.getString(KEY_AUTH_PROVIDER, "EMAIL")); // Mặc định là EMAIL
        user.setProfileImageLocalPath(sharedPreferences.getString(KEY_PROFILE_IMAGE_LOCAL_PATH, null));

        Log.d(TAG, "Retrieved User - ID: " + user.getId() +
                ", Email: " + user.getEmail() +
                ", AuthProvider: " + user.getAuthProvider() +
                ", ImagePath: " + user.getProfileImageLocalPath());
        return user;
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void clearSession() {
        editor.clear(); // Xóa tất cả dữ liệu trong SharedPreferences này
        editor.apply();
        Log.d(TAG, "Session cleared.");
    }
}
