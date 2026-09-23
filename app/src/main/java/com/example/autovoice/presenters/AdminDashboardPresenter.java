package com.example.autovoice.presenters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns; // Import Patterns

import com.example.autovoice.database.UserDAO;
import com.example.autovoice.models.User;
import org.mindrot.jbcrypt.BCrypt; // Import thư viện BCrypt

import java.util.List;

public class AdminDashboardPresenter {

    // Interface Contract cho View (AdminDashboardActivity)
    public interface AdminDashboardContract {
        interface View {
            void displayUsers(List<User> userList);
            void showMessage(String message);
            void navigateToLogin();
            void showAddUserDialog();
            void showAddUserSuccess();
            Context getContext();

            // --- CÁC PHƯƠNG THỨC MỚI CHO SỬA MẬT KHẨU VÀ NÂNG CẤP GÓI ---
            void showEditPasswordDialog(User userToEdit);
            void showUpgradePackageDialog(User userToEdit);
            void closeEditDialogs(); // Để đóng các dialog sau khi thành công
        }
        // interface Presenter { /* ... */ } // Nếu cần
    }

    private final AdminDashboardContract.View view;
    private final UserDAO userDAO;
    // private Context context; // Có thể không cần nếu view.getContext() luôn sẵn dùng

    public AdminDashboardPresenter(AdminDashboardContract.View view) {
        this.view = view;
        // Lấy context từ View để khởi tạo DAO
        this.userDAO = new UserDAO(view.getContext());
    }

    public void loadAllUsers() {
        if (view == null) {
            Log.e("AdminDashboardPresenter", "View is null in loadAllUsers.");
            return;
        }
        try {
            List<User> userList = userDAO.getAllUsers(); // DAO nên trả về list không có password
            Log.i("AdminDashboardPresenter", "Loaded " + (userList != null ? userList.size() : 0) + " users.");
            view.displayUsers(userList);
        } catch (Exception e) {
            Log.e("AdminDashboardPresenter", "Error loading users", e);
            view.showMessage("Error loading users: " + e.getMessage());
        }
    }

    public void addUserRequested() {
        if (view != null) {
            view.showAddUserDialog();
        }
    }

    public void addUser(String email, String username, String password, String confirmPassword, boolean isAdmin) {
        if (view == null) return;

        // Validate input
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            view.showMessage("Định dạng email không hợp lệ."); return;
        }
        if (TextUtils.isEmpty(username)) {
            view.showMessage("Tên người dùng không được để trống."); return;
        }
        if (TextUtils.isEmpty(password)) {
            view.showMessage("Mật khẩu không được để trống."); return;
        }
        if (password.length() < 6) {
            view.showMessage("Mật khẩu phải có ít nhất 6 ký tự."); return;
        }
        if (!password.equals(confirmPassword)) {
            view.showMessage("Mật khẩu xác nhận không khớp."); return;
        }

        // Hash mật khẩu trước khi lưu
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setPassword(hashedPassword); // Lưu mật khẩu đã hash
        newUser.setAdmin(isAdmin);
        newUser.setSubscriptionStatus("Free"); // Gói mặc định khi admin thêm

        long result = userDAO.addUser(newUser);
        if (result == -2) { // Lỗi trùng lặp từ DAO
            view.showMessage("Email hoặc Tên người dùng đã tồn tại.");
        } else if (result >= 0) { // Thêm thành công
            view.showMessage("Người dùng '" + username + "' đã được thêm thành công.");
            view.showAddUserSuccess(); // Đóng dialog thêm user
            loadAllUsers(); // Tải lại danh sách
        } else { // Lỗi khác
            view.showMessage("Thêm người dùng thất bại. Vui lòng thử lại.");
        }
    }

    public void deleteUserByUsername(String username) {
        if (view == null) return;
        // ... (logic deleteUserByUsername như trước)
        try {
            int rowsAffected = userDAO.deleteUserByUsername(username);
            if (rowsAffected > 0) {
                view.showMessage("User '" + username + "' deleted successfully.");
                loadAllUsers();
            } else {
                view.showMessage("Could not delete user '" + username + "'.");
            }
        } catch (Exception e) {
            Log.e("AdminDashboardPresenter", "Error deleting user " + username, e);
            view.showMessage("Error deleting user: " + e.getMessage());
        }
    }

    // --- YÊU CẦU HIỂN THỊ DIALOG SỬA MẬT KHẨU ---
    public void editUserPasswordRequested(User userToEdit) {
        if (view != null && userToEdit != null) {
            Log.d("AdminDashboardPresenter", "Edit password requested for user: " + userToEdit.getUsername());
            view.showEditPasswordDialog(userToEdit);
        } else if (view != null) {
            view.showMessage("Không thể sửa, không có thông tin người dùng.");
        } else {
            Log.e("AdminDashboardPresenter", "View is null in editUserPasswordRequested.");
        }
    }

    // --- ADMIN ĐẶT LẠI MẬT KHẨU CHO USER ---
    public void adminSetUserPassword(int userId, String newPassword, String confirmNewPassword) {
        if (view == null) {
            Log.e("AdminDashboardPresenter", "View is null in adminSetUserPassword.");
            return;
        }
        Log.d("AdminDashboardPresenter", "Admin attempting to set password for user ID: " + userId);

        if (TextUtils.isEmpty(newPassword)) {
            // Không báo lỗi nếu admin không muốn đổi pass, chỉ đóng dialog
            // view.showMessage("Mật khẩu mới không được để trống.");
            Log.d("AdminDashboardPresenter", "New password field is empty, password will not be changed.");
            view.closeEditDialogs(); // Đóng dialog nếu không có gì để làm
            return;
        }
        if (newPassword.length() < 6) {
            view.showMessage("Mật khẩu mới phải có ít nhất 6 ký tự.");
            return;
        }
        if (!newPassword.equals(confirmNewPassword)) {
            view.showMessage("Mật khẩu mới và xác nhận không khớp.");
            return;
        }

        String hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        if (userDAO.updatePassword(userId, hashedNewPassword)) { // Giả sử DAO có hàm này
            Log.i("AdminDashboardPresenter", "Password updated successfully by admin for user ID: " + userId);
            view.showMessage("Mật khẩu đã được đặt lại thành công cho người dùng.");
            view.closeEditDialogs(); // Yêu cầu View đóng dialog
            // Không cần loadAllUsers() vì chỉ đổi mật khẩu, không ảnh hưởng danh sách hiển thị
        } else {
            Log.e("AdminDashboardPresenter", "Failed to update password in DB by admin for user ID: " + userId);
            view.showMessage("Lỗi khi đặt lại mật khẩu.");
        }
    }

    // --- YÊU CẦU HIỂN THỊ DIALOG NÂNG CẤP GÓI ---
    public void upgradeUserPackageRequested(User userToEdit) {
        if (view != null && userToEdit != null) {
            Log.d("AdminDashboardPresenter", "Upgrade package requested for user: " + userToEdit.getUsername());
            view.showUpgradePackageDialog(userToEdit);
        } else if (view != null) {
            view.showMessage("Không thể nâng cấp, không có thông tin người dùng.");
        } else {
            Log.e("AdminDashboardPresenter", "View is null in upgradeUserPackageRequested.");
        }
    }

    // --- ADMIN CẬP NHẬT GÓI CHO USER ---
    public void adminUpdateUserPackage(int userId, String newPackageStatus) {
        if (view == null) {
            Log.e("AdminDashboardPresenter", "View is null in adminUpdateUserPackage.");
            return;
        }
        Log.d("AdminDashboardPresenter", "Admin attempting to update package for user ID: " + userId + " to " + newPackageStatus);

        if (TextUtils.isEmpty(newPackageStatus)) {
            view.showMessage("Vui lòng chọn một gói dịch vụ hợp lệ.");
            return;
        }

        // Giả sử UserDAO có phương thức updateSubscriptionStatus
        if (userDAO.updateSubscriptionStatus(userId, newPackageStatus)) {
            Log.i("AdminDashboardPresenter", "Package updated successfully for user ID: " + userId);
            view.showMessage("Gói dịch vụ đã được cập nhật thành công cho người dùng.");
            view.closeEditDialogs(); // Yêu cầu View đóng dialog
            loadAllUsers(); // Tải lại danh sách để hiển thị gói mới
        } else {
            Log.e("AdminDashboardPresenter", "Failed to update package in DB for user ID: " + userId);
            view.showMessage("Lỗi khi cập nhật gói dịch vụ.");
        }
    }

    public void handleLogout() {
        if (view != null) {
            Log.i("AdminDashboardPresenter", "Logout requested.");
            // Xử lý logic logout nếu cần (ví dụ: xóa token admin nếu có)
            view.navigateToLogin();
        }
    }

    public void onDestroy() {
        if (userDAO != null) {
            userDAO.close();
        }
        Log.d("AdminDashboardPresenter", "onDestroy called, DAO closed.");
    }
}
