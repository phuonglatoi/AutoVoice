package com.example.autovoice.presenters;

import android.content.Context;
import android.util.Log;
import com.example.autovoice.models.User;
import com.example.autovoice.utils.SessionManager;

public class MainUserPresenter {

    // Interface (Contract) cho View (MainUserActivity)
    public interface MainUserView {
        void navigateToLoginScreen(); // Yêu cầu View chuyển đến màn hình Login
        Context getContext(); // Để Presenter lấy Context
        // Thêm các phương thức khác nếu cần
    }

    private final MainUserView view;
    private final SessionManager sessionManager;
    // private Context context; // Không cần nếu dùng view.getContext()

    public MainUserPresenter(MainUserView view) {
        this.view = view;
        this.sessionManager = new SessionManager(view.getContext().getApplicationContext());
    }

    public User getCurrentUser() {
        if (sessionManager == null) return null;
        return sessionManager.getLoggedInUser();
    }

    public void logout() {
        Log.i("MainUserPresenter", "Logout requested.");
        if (sessionManager != null) {
            sessionManager.clearSession();
            Log.d("MainUserPresenter", "Session cleared.");
        }
        if (view != null) {
            Log.d("MainUserPresenter", "Requesting view to navigate to login screen.");
            view.navigateToLoginScreen(); // Yêu cầu View điều hướng
        } else {
            Log.e("MainUserPresenter", "View is null, cannot navigate after logout.");
        }
    }

    public String getSubscriptionStatus() {
        User user = getCurrentUser();
        if (user != null && user.getSubscriptionStatus() != null) {
            return user.getSubscriptionStatus();
        }
        return "N/A";
    }

    public void onDestroy() {
        Log.d("MainUserPresenter", "onDestroy called (no specific resources to release).");
        // Hiện tại không có tài nguyên đặc biệt cần đóng ở đây
    }
}
