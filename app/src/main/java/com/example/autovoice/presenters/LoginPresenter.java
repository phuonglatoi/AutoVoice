package com.example.autovoice.presenters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.net.Uri; // Import Uri cho photoUrl

import com.example.autovoice.database.UserDAO;
import com.example.autovoice.models.User;
import com.example.autovoice.utils.SessionManager;

import org.mindrot.jbcrypt.BCrypt;
// Import lớp GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.Locale; // Import Locale

public class LoginPresenter {

    private static final String TAG = "LoginPresenter_Fix"; // Tag đã được cập nhật

    public interface LoginView {
        void onLoginSuccess(User user);
        void onLoginFailed(String message);
        Context getContext(); // View phải cung cấp Context hợp lệ
    }

    private LoginView view;
    private UserDAO userDAO;
    private SessionManager sessionManager;
    private boolean isInitializedProperly = false;

    public LoginPresenter(LoginView view) {
        this.view = view;
        if (this.view == null) {
            Log.e(TAG, "LoginView is null in constructor.");
            isInitializedProperly = false;
            return;
        }
        Context contextFromView = view.getContext();
        if (contextFromView == null) {
            Log.e(TAG, "Context from LoginView is null.");
            isInitializedProperly = false;
            if (this.view != null) this.view.onLoginFailed("Lỗi khởi tạo hệ thống (context).");
            return;
        }
        Context appContext = contextFromView.getApplicationContext();
        try {
            this.userDAO = new UserDAO(appContext);
            this.sessionManager = new SessionManager(appContext);
            isInitializedProperly = true;
            Log.d(TAG, "Presenter initialized successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UserDAO or SessionManager: " + e.getMessage(), e);
            isInitializedProperly = false;
            if (this.view != null) this.view.onLoginFailed("Lỗi khởi tạo dữ liệu người dùng (DAO/Session).");
        }
    }

    public void login(String email, String password) {
        if (!isInitializedProperly) {
            if (view != null) view.onLoginFailed("Lỗi hệ thống. Không thể đăng nhập.");
            return;
        }
        if (view == null) { Log.e(TAG, "LoginView is null! Cannot proceed with email login."); return; }

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            view.onLoginFailed("Vui lòng nhập đầy đủ email và mật khẩu."); return;
        }
        // Chuẩn hóa email về chữ thường trước khi xử lý
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            view.onLoginFailed("Định dạng email không hợp lệ."); return;
        }
        Log.d(TAG, "Attempting email login for: " + normalizedEmail);

        User userFromDb = null;
        try {
            if (userDAO == null) {
                view.onLoginFailed("Lỗi dữ liệu (DAO null)."); return;
            }
            // Tìm user bằng email đã chuẩn hóa
            userFromDb = userDAO.getUserByEmail(normalizedEmail);
        } catch (Exception e) {
            Log.e(TAG, "Exception from userDAO.getUserByEmail: " + e.getMessage(), e);
            view.onLoginFailed("Lỗi truy vấn dữ liệu."); return;
        }

        if (userFromDb != null && userFromDb.getPassword() != null) {
            String storedHashedPassword = userFromDb.getPassword();
            if (BCrypt.checkpw(password, storedHashedPassword)) {
                Log.i(TAG, "Email Login successful for: " + normalizedEmail);
                User sessionUser = createSessionUserFromDbUser(userFromDb);
                if (sessionUser != null && (sessionUser.getAuthProvider() == null || !sessionUser.getAuthProvider().equals("EMAIL"))) {
                    sessionUser.setAuthProvider("EMAIL");
                    // Cân nhắc: userDAO.updateAuthProvider(sessionUser.getId(), "EMAIL"); // Cần thêm hàm này vào DAO nếu muốn
                }
                if (sessionManager == null) {
                    view.onLoginFailed("Lỗi quản lý phiên."); return;
                }
                sessionManager.saveUserSession(sessionUser);
                view.onLoginSuccess(sessionUser);
            } else {
                view.onLoginFailed("Email hoặc mật khẩu không đúng.");
            }
        } else {
            view.onLoginFailed("Email hoặc mật khẩu không đúng.");
        }
    }

    public void processGoogleSignIn(GoogleSignInAccount googleAccount) {
        if (!isInitializedProperly) {
            if (view != null) view.onLoginFailed("Lỗi hệ thống (Google Sign-In).");
            return;
        }
        if (view == null || googleAccount == null) {
            if(view != null) view.onLoginFailed("Lỗi thông tin đăng nhập Google.");
            return;
        }

        String emailFromGoogle = googleAccount.getEmail();
        if (TextUtils.isEmpty(emailFromGoogle)) {
            view.onLoginFailed("Không thể lấy email từ tài khoản Google.");
            return;
        }
        // Chuẩn hóa email về chữ thường để kiểm tra và lưu trữ nhất quán
        String email = emailFromGoogle.toLowerCase(Locale.ROOT);

        String googleUserId = googleAccount.getId();
        String displayName = googleAccount.getDisplayName();
        Log.i(TAG, "Processing Google Sign-In for email: [" + email + "], GoogleID: [" + googleUserId + "], DisplayName: [" + displayName + "]");

        if (userDAO == null) {
            view.onLoginFailed("Lỗi dữ liệu (DAO null Google)."); return;
        }

        User existingUser = userDAO.getUserByEmail(email); // Email đã được chuẩn hóa
        User userToSaveInSession;

        if (existingUser == null) {
            Log.d(TAG, "User with email [" + email + "] not found. Creating new Google user.");
            User newUser = new User();
            newUser.setEmail(email); // Lưu email chữ thường

            // --- LOGIC TẠO USERNAME DUY NHẤT (CẢI TIẾN VÀ CHUẨN HÓA) ---
            String baseUsernameSource = TextUtils.isEmpty(displayName) ? email.split("@")[0] : displayName;
            String baseUsername = baseUsernameSource.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.]", ""); // Chuyển về chữ thường và làm sạch

            if (TextUtils.isEmpty(baseUsername) || baseUsername.length() < 3) {
                baseUsername = "user" + (System.currentTimeMillis() % 10000);
                baseUsername = baseUsername.toLowerCase(Locale.ROOT); // Đảm bảo username dự phòng cũng là chữ thường
            }
            Log.d(TAG, "Base username generated and normalized: [" + baseUsername + "]");

            String finalUsername = baseUsername;
            int attempt = 0;
            User checkUser;
            // Vòng lặp kiểm tra username duy nhất (đã chuẩn hóa chữ thường)
            // UserDAO.getUserByUsername nên tìm kiếm case-insensitive hoặc nhận username đã chuẩn hóa
            while ((checkUser = userDAO.getUserByUsername(finalUsername)) != null) {
                attempt++;
                finalUsername = baseUsername + attempt;
                Log.d(TAG, "Username [" + (baseUsername + (attempt > 1 ? String.valueOf(attempt -1) : "")) + "] exists (User ID: " + (checkUser != null ? checkUser.getId() : "null") + "), trying: [" + finalUsername + "]");
                if (attempt > 100) { // Giới hạn số lần thử
                    Log.e(TAG, "Could not generate a unique username after 100 attempts for base: " + baseUsername);
                    view.onLoginFailed("Lỗi tạo tên người dùng duy nhất. Vui lòng thử lại sau.");
                    return;
                }
            }
            newUser.setUsername(finalUsername); // Lưu username chữ thường
            Log.i(TAG, "Final unique username for Google user: [" + finalUsername + "]");
            // ------------------------------------

            newUser.setPassword(null);
            newUser.setAdmin(false);
            newUser.setSubscriptionStatus("Free");
            newUser.setAuthProvider("GOOGLE");

            Log.d(TAG, "Attempting to add new Google user to DB: Email=[" + newUser.getEmail() + "], Username=[" + newUser.getUsername() + "]");
            long newUserId = userDAO.addUser(newUser);

            if (newUserId >= 0) {
                newUser.setId((int) newUserId);
                userToSaveInSession = newUser;
                Log.i(TAG, "New Google user created. DB ID: " + newUserId + ", Username: " + finalUsername);
            } else {
                Log.e(TAG, "FAILED to create new Google user in DB. DAO.addUser Error code: " + newUserId + " for username: [" + finalUsername + "] and email: [" + email + "]");
                if (newUserId == -2) {
                    view.onLoginFailed("Lỗi tạo tài khoản: Email hoặc Username đã được sử dụng. Vui lòng thử đăng nhập bằng email và mật khẩu nếu bạn đã có tài khoản.");
                } else {
                    view.onLoginFailed("Lỗi tạo tài khoản mới từ Google (mã: " + newUserId +").");
                }
                return;
            }
        } else {
            Log.d(TAG, "User with email [" + email + "] already exists. ID: " + existingUser.getId() + ". Current AuthProvider: " + existingUser.getAuthProvider());
            // User đã tồn tại, cập nhật authProvider thành GOOGLE
            // và có thể cập nhật các thông tin khác từ Google nếu muốn
            existingUser.setAuthProvider("GOOGLE");
            // TODO: Gọi userDAO.updateUser(existingUser) hoặc một phương thức DAO cụ thể để cập nhật authProvider và các thông tin khác trong DB.
            // Ví dụ: boolean updatedInDb = userDAO.updateAuthProvider(existingUser.getId(), "GOOGLE");
            // if (!updatedInDb) { Log.w(TAG, "Failed to update auth provider in DB for existing user: " + email); }
            userToSaveInSession = existingUser;
        }

        if (sessionManager == null) {
            view.onLoginFailed("Lỗi quản lý phiên (Google)."); return;
        }
        sessionManager.saveUserSession(userToSaveInSession);
        Log.i(TAG, "Session saved for user ID: " + userToSaveInSession.getId() + " with AuthProvider: " + userToSaveInSession.getAuthProvider());
        view.onLoginSuccess(userToSaveInSession);
    }

    private User createSessionUserFromDbUser(User dbUser) {
        if (dbUser == null) return null;
        User sessionUser = new User();
        sessionUser.setId(dbUser.getId());
        sessionUser.setEmail(dbUser.getEmail());
        sessionUser.setUsername(dbUser.getUsername());
        sessionUser.setSubscriptionStatus(dbUser.getSubscriptionStatus());
        sessionUser.setAdmin(dbUser.isAdmin());
        sessionUser.setProfileImageLocalPath(dbUser.getProfileImageLocalPath());
        sessionUser.setAuthProvider(dbUser.getAuthProvider());
        return sessionUser;
    }

    public boolean isLoggedIn() {
        if (!isInitializedProperly || sessionManager == null) return false;
        return sessionManager.isLoggedIn();
    }

    public User getCurrentUser() {
        if (!isInitializedProperly || sessionManager == null) return null;
        return sessionManager.getLoggedInUser();
    }

    public void logout() {
        if (isInitializedProperly && sessionManager != null) {
            sessionManager.clearSession();
        }
        // if (view != null) {
        //     view.onLogoutComplete();
        // }
    }

    public void onDestroy() {
        if (userDAO != null) {
            try {
                userDAO.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing UserDAO: " + e.getMessage(), e);
            }
        }
    }
}
