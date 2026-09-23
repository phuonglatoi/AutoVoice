package com.example.autovoice.presenters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.example.autovoice.database.UserDAO;
import com.example.autovoice.models.User;
import com.example.autovoice.utils.SessionManager;

// Import thư viện jBCrypt để hash và kiểm tra mật khẩu
import org.mindrot.jbcrypt.BCrypt;

public class ChangePasswordPresenter {

    private static final String TAG = "ChangePassPresenter"; // Tag cho Log

    // Interface (Contract) cho View mà ChangePasswordActivity sẽ implement
    public interface ChangePasswordView {
        void showCurrentPasswordError(String message);
        void showNewPasswordError(String message);
        void showConfirmPasswordError(String message);
        void clearErrors();
        void showChangePasswordSuccess(String message);
        void showChangePasswordFailure(String message);
        void showProgress(); // Để hiển thị trạng thái đang xử lý
        void hideProgress(); // Để ẩn trạng thái đang xử lý
        // Context không cần thiết trong interface này nếu Presenter nhận nó qua constructor
    }

    private final ChangePasswordView view;
    // private final Context context; // Không cần lưu context riêng nếu đã có UserDAO và SessionManager
    private final UserDAO userDAO;
    private final SessionManager sessionManager;

    public ChangePasswordPresenter(ChangePasswordView view, Context context) {
        this.view = view;
        // Sử dụng application context để khởi tạo DAO và SessionManager, tránh rò rỉ
        Context appContext = context.getApplicationContext();
        this.userDAO = new UserDAO(appContext);
        this.sessionManager = new SessionManager(appContext);
    }

    public void changePassword(String currentPassword, String newPassword, String confirmNewPassword) {
        if (view == null) {
            Log.e(TAG, "ChangePasswordView is null. Cannot proceed.");
            return;
        }
        view.clearErrors(); // Xóa các thông báo lỗi cũ
        view.showProgress(); // Bắt đầu hiển thị progress (nếu có)

        // 1. Validate Input cơ bản
        boolean isValid = true;
        if (TextUtils.isEmpty(currentPassword)) {
            view.showCurrentPasswordError("Mật khẩu hiện tại không được để trống.");
            isValid = false;
        }
        if (TextUtils.isEmpty(newPassword)) {
            view.showNewPasswordError("Mật khẩu mới không được để trống.");
            isValid = false;
        } else if (newPassword.length() < 6) { // Kiểm tra độ dài tối thiểu
            view.showNewPasswordError("Mật khẩu mới phải có ít nhất 6 ký tự.");
            isValid = false;
        }
        if (TextUtils.isEmpty(confirmNewPassword)) {
            view.showConfirmPasswordError("Xác nhận mật khẩu mới không được để trống.");
            isValid = false;
        } else if (!newPassword.equals(confirmNewPassword)) {
            view.showConfirmPasswordError("Mật khẩu mới và xác nhận không khớp.");
            isValid = false;
        }
        if (isValid && newPassword.equals(currentPassword)) { // Kiểm tra mật khẩu mới có trùng mật khẩu cũ không
            view.showNewPasswordError("Mật khẩu mới không được trùng với mật khẩu cũ.");
            isValid = false;
        }


        if (!isValid) {
            view.hideProgress(); // Ẩn progress nếu có lỗi validate
            return;
        }

        // 2. Lấy thông tin người dùng hiện tại từ SessionManager
        User currentUserFromSession = sessionManager.getLoggedInUser();
        if (currentUserFromSession == null || currentUserFromSession.getId() == 0) { // Kiểm tra ID hợp lệ
            view.hideProgress();
            view.showChangePasswordFailure("Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.");
            Log.e(TAG, "Current user from session is null or has invalid ID.");
            return;
        }
        int userId = currentUserFromSession.getId();
        Log.d(TAG, "Attempting to change password for User ID: " + userId);

        // 3. Lấy thông tin user đầy đủ (bao gồm hashed password) từ UserDAO
        User userFromDb = userDAO.getUserById(userId); // UserDAO.getUserById() phải trả về User với hashed password
        if (userFromDb == null || TextUtils.isEmpty(userFromDb.getPassword())) {
            view.hideProgress();
            view.showChangePasswordFailure("Lỗi lấy thông tin tài khoản để xác thực.");
            Log.e(TAG, "Failed to fetch user details from DB or password is empty for ID: " + userId);
            return;
        }

        // 4. Xác thực mật khẩu hiện tại bằng BCrypt
        if (!BCrypt.checkpw(currentPassword, userFromDb.getPassword())) {
            view.hideProgress();
            view.showCurrentPasswordError("Mật khẩu hiện tại không chính xác.");
            Log.w(TAG, "Current password mismatch for user ID: " + userId);
            return;
        }
        Log.d(TAG, "Current password verified successfully for user ID: " + userId);

        // 5. Hash mật khẩu mới
        String newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        Log.d(TAG, "New password hashed successfully for user ID: " + userId);

        // 6. Cập nhật mật khẩu mới vào cơ sở dữ liệu
        // UserDAO.updatePassword() cần được implement để cập nhật hashed password
        boolean updateSuccess = userDAO.updatePassword(userId, newHashedPassword);

        view.hideProgress(); // Ẩn progress sau khi xử lý xong
        if (updateSuccess) {
            Log.i(TAG, "Password updated successfully in DB for user ID: " + userId);
            // Không cần cập nhật lại session ở đây vì session không lưu mật khẩu.
            // Nếu có lưu các thông tin khác liên quan đến bảo mật mà cần reset, thì xử lý ở đây.
            view.showChangePasswordSuccess("Thay đổi mật khẩu thành công!");
        } else {
            Log.e(TAG, "Failed to update password in DB for user ID: " + userId);
            view.showChangePasswordFailure("Thay đổi mật khẩu thất bại. Vui lòng thử lại.");
        }
    }

    // Phương thức này được gọi từ onDestroy của Activity để giải phóng tài nguyên
    public void onDestroy() {
        Log.d(TAG, "onDestroy called. Closing UserDAO.");
        if (userDAO != null) {
            try {
                userDAO.close(); // Đóng kết nối DAO
            } catch (Exception e) {
                Log.e(TAG, "Error closing UserDAO: " + e.getMessage(), e);
            }
        }
        // Không cần gán view = null vì Presenter thường có vòng đời gắn với View
    }
}
