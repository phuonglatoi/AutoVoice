package com.example.autovoice.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteConstraintException;
import android.text.TextUtils;
import android.util.Log;

import com.example.autovoice.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // Import Locale

public class UserDAO {

    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private static final String TAG = "UserDAO_CaseFix";

    private String[] allColumnsWithPassword = {
            DBHelper.COLUMN_USER_ID,
            DBHelper.COLUMN_USER_EMAIL,
            DBHelper.COLUMN_USER_USERNAME,
            DBHelper.COLUMN_USER_PASSWORD,
            DBHelper.COLUMN_USER_SUBSCRIPTION_STATUS,
            DBHelper.COLUMN_USER_IS_ADMIN,
            DBHelper.COLUMN_USER_PROFILE_IMAGE_LOCAL_PATH,
            DBHelper.COLUMN_USER_AUTH_PROVIDER
    };

    private String[] allColumnsNoPassword = {
            DBHelper.COLUMN_USER_ID,
            DBHelper.COLUMN_USER_EMAIL,
            DBHelper.COLUMN_USER_USERNAME,
            DBHelper.COLUMN_USER_SUBSCRIPTION_STATUS,
            DBHelper.COLUMN_USER_IS_ADMIN,
            DBHelper.COLUMN_USER_PROFILE_IMAGE_LOCAL_PATH,
            DBHelper.COLUMN_USER_AUTH_PROVIDER
    };

    public UserDAO(Context context) {
        dbHelper = new DBHelper(context.getApplicationContext());
        try {
            open();
        } catch (SQLException e) {
            Log.e(TAG, "DATABASE ERROR: Failed to open database: " + e.getMessage(), e);
        }
    }

    public void open() throws SQLException {
        if (database == null || !database.isOpen()) {
            database = dbHelper.getWritableDatabase();
            Log.i(TAG, "Database connection opened.");
        }
    }

    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
            Log.i(TAG, "Database connection closed.");
        }
    }

    public long addUser(User user) {
        if (user == null || database == null || !database.isOpen()) {
            Log.e(TAG, "addUser: Invalid params or DB not open.");
            return -1;
        }
        if (TextUtils.isEmpty(user.getEmail()) || TextUtils.isEmpty(user.getUsername())) {
            Log.e(TAG, "addUser: Email or Username is empty.");
            return -1;
        }

        ContentValues values = new ContentValues();
        // Luôn lưu email và username dưới dạng chữ thường để đảm bảo tính duy nhất
        values.put(DBHelper.COLUMN_USER_EMAIL, user.getEmail().toLowerCase(Locale.ROOT));
        values.put(DBHelper.COLUMN_USER_USERNAME, user.getUsername().toLowerCase(Locale.ROOT));

        if (user.getPassword() != null) {
            values.put(DBHelper.COLUMN_USER_PASSWORD, user.getPassword());
        } else {
            values.putNull(DBHelper.COLUMN_USER_PASSWORD);
        }
        values.put(DBHelper.COLUMN_USER_SUBSCRIPTION_STATUS, user.getSubscriptionStatus() != null ? user.getSubscriptionStatus() : "Free");
        values.put(DBHelper.COLUMN_USER_IS_ADMIN, user.isAdmin() ? 1 : 0);
        values.put(DBHelper.COLUMN_USER_AUTH_PROVIDER, user.getAuthProvider() != null ? user.getAuthProvider() : "EMAIL");
        if (user.getProfileImageLocalPath() != null) {
            values.put(DBHelper.COLUMN_USER_PROFILE_IMAGE_LOCAL_PATH, user.getProfileImageLocalPath());
        } else {
            values.putNull(DBHelper.COLUMN_USER_PROFILE_IMAGE_LOCAL_PATH);
        }

        long insertId = -1;
        try {
            Log.d(TAG, "addUser: Attempting to insert user - Email: " + values.getAsString(DBHelper.COLUMN_USER_EMAIL) + ", Username: " + values.getAsString(DBHelper.COLUMN_USER_USERNAME));
            insertId = database.insertOrThrow(DBHelper.TABLE_USERS, null, values);
            if (insertId > 0) {
                Log.i(TAG, "addUser: User added successfully with ID: " + insertId);
            }
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "addUser: SQLiteConstraintException for Email: " + values.getAsString(DBHelper.COLUMN_USER_EMAIL) + " or Username: " + values.getAsString(DBHelper.COLUMN_USER_USERNAME) + ". Message: " + e.getMessage(), e);
            insertId = -2;
        } catch (SQLException e) {
            Log.e(TAG, "addUser: SQLException. Message: " + e.getMessage(), e);
            insertId = -1;
        }
        return insertId;
    }

    public User getUserByEmail(String email) {
        if (database == null || !database.isOpen() || TextUtils.isEmpty(email)) {
            Log.w(TAG, "getUserByEmail: DB not open or email empty.");
            return null;
        }
        // Chuẩn hóa email về chữ thường để tìm kiếm không phân biệt hoa thường
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        Log.d(TAG, "Querying user by normalized email: [" + normalizedEmail + "]");
        User user = null;
        Cursor cursor = null;
        // Sử dụng COLLATE NOCASE để tìm kiếm không phân biệt chữ hoa/thường
        String selection = DBHelper.COLUMN_USER_EMAIL + " = ? COLLATE NOCASE";
        String[] selectionArgs = {normalizedEmail};
        try {
            cursor = database.query(DBHelper.TABLE_USERS, allColumnsWithPassword,
                    selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                user = cursorToUser(cursor, true);
                Log.i(TAG, "User found by email: " + email);
            } else {
                Log.d(TAG, "No user found with email: " + email);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying user by email: " + email, e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return user;
    }

    public User getUserByUsername(String username) {
        if (database == null || !database.isOpen() || TextUtils.isEmpty(username)) {
            Log.w(TAG, "getUserByUsername: DB not open or username empty.");
            return null;
        }
        // Chuẩn hóa username về chữ thường
        String normalizedUsername = username.toLowerCase(Locale.ROOT);
        Log.d(TAG, "Querying user by normalized username: [" + normalizedUsername + "]");
        User user = null;
        Cursor cursor = null;
        // Sử dụng COLLATE NOCASE
        String selection = DBHelper.COLUMN_USER_USERNAME + " = ? COLLATE NOCASE";
        String[] selectionArgs = {normalizedUsername};
        try {
            cursor = database.query(DBHelper.TABLE_USERS, allColumnsNoPassword,
                    selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                user = cursorToUser(cursor, false);
                Log.i(TAG, "User found by username: " + username);
            } else {
                Log.d(TAG, "No user found with username: " + username);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying user by username: " + username, e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return user;
    }

    @SuppressLint("Range")
    private User cursorToUser(Cursor cursor, boolean includePassword) {
        // ... (Giữ nguyên logic cursorToUser, nó đã đọc các cột cần thiết)
        if (cursor == null) return null;
        User user = new User();
        try {
            user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_USER_ID)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_USER_EMAIL)));
            user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_USER_USERNAME)));

            if (includePassword) {
                int passwordColIndex = cursor.getColumnIndex(DBHelper.COLUMN_USER_PASSWORD);
                if (passwordColIndex != -1) {
                    user.setPassword(cursor.getString(passwordColIndex));
                }
            }

            int subStatusColIndex = cursor.getColumnIndex(DBHelper.COLUMN_USER_SUBSCRIPTION_STATUS);
            if (subStatusColIndex != -1) user.setSubscriptionStatus(cursor.getString(subStatusColIndex));

            int isAdminColIndex = cursor.getColumnIndex(DBHelper.COLUMN_USER_IS_ADMIN);
            if (isAdminColIndex != -1) user.setAdmin(cursor.getInt(isAdminColIndex) == 1);

            int imgPathColIndex = cursor.getColumnIndex(DBHelper.COLUMN_USER_PROFILE_IMAGE_LOCAL_PATH);
            if (imgPathColIndex != -1) user.setProfileImageLocalPath(cursor.getString(imgPathColIndex));

            int authProviderColIndex = cursor.getColumnIndex(DBHelper.COLUMN_USER_AUTH_PROVIDER);
            if (authProviderColIndex != -1) user.setAuthProvider(cursor.getString(authProviderColIndex));

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "cursorToUser: Column not found in cursor - " + e.getMessage(), e);
            return null;
        }
        return user;
    }

    // Các phương thức update và delete giữ nguyên
    public boolean updatePassword(int userId, String newHashedPassword) {
        if (database == null || !database.isOpen() || userId <= 0 || TextUtils.isEmpty(newHashedPassword)) return false;
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_USER_PASSWORD, newHashedPassword);
        int rowsAffected = 0;
        try {
            rowsAffected = database.update(DBHelper.TABLE_USERS, values, DBHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        } catch (Exception e) { Log.e(TAG, "Error updating password", e); return false; }
        return rowsAffected > 0;
    }

    public boolean updateSubscriptionStatus(int userId, String newStatus) {
        if (database == null || !database.isOpen() || userId <= 0 || TextUtils.isEmpty(newStatus)) return false;
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_USER_SUBSCRIPTION_STATUS, newStatus);
        int rowsAffected = 0;
        try {
            rowsAffected = database.update(DBHelper.TABLE_USERS, values, DBHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        } catch (Exception e) { Log.e(TAG, "Error updating subscription", e); return false; }
        return rowsAffected > 0;
    }

    public boolean updateProfileImagePath(int userId, String newImagePath) {
        if (database == null || !database.isOpen() || userId <= 0) return false;
        ContentValues values = new ContentValues();
        if (TextUtils.isEmpty(newImagePath)) values.putNull(DBHelper.COLUMN_USER_PROFILE_IMAGE_LOCAL_PATH);
        else values.put(DBHelper.COLUMN_USER_PROFILE_IMAGE_LOCAL_PATH, newImagePath);
        int rowsAffected = 0;
        try {
            rowsAffected = database.update(DBHelper.TABLE_USERS, values, DBHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        } catch (Exception e) { Log.e(TAG, "Error updating image path", e); return false; }
        return rowsAffected > 0;
    }

    public int deleteUserByUsername(String username) {
        if (database == null || !database.isOpen() || TextUtils.isEmpty(username)) return 0;
        if ("admin".equalsIgnoreCase(username)) return 0; // So sánh không phân biệt hoa thường
        int rowsAffected = 0;
        try {
            // Xóa cũng nên không phân biệt hoa thường nếu username được lưu chuẩn hóa
            rowsAffected = database.delete(DBHelper.TABLE_USERS, DBHelper.COLUMN_USER_USERNAME + " = ? COLLATE NOCASE", new String[]{username});
        } catch (Exception e) { Log.e(TAG, "Error deleting user", e); }
        return rowsAffected;
    }
    public List<User> getAllUsers() {
        if (database == null || !database.isOpen()) return new ArrayList<>();
        List<User> users = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(DBHelper.TABLE_USERS, allColumnsNoPassword, null, null, null, null, DBHelper.COLUMN_USER_USERNAME + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    users.add(cursorToUser(cursor, false));
                    cursor.moveToNext();
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return users;
    }
    public User getUserById(int userId) {
        if (database == null || !database.isOpen() || userId <= 0) {
            Log.e(TAG, "Database not open or invalid user ID for getUserById: " + userId);
            return null;
        }
        User user = null;
        Cursor cursor = null;
        try {
            cursor = database.query(DBHelper.TABLE_USERS, allColumnsWithPassword,
                    DBHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                user = cursorToUser(cursor, true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying user by ID: " + userId, e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return user;
    }
}
