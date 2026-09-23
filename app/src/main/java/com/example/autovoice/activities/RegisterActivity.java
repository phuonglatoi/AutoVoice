package com.example.autovoice.activities;

import android.content.Intent;
import android.os.Bundle;
// Bỏ import android.view.View; vì dùng lambda
import android.util.Log; // Thêm Log
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.autovoice.R;
import com.example.autovoice.database.UserDAO; // Đảm bảo import đúng DAO
import com.example.autovoice.models.User;

// Import thư viện jBCrypt để hash mật khẩu
import org.mindrot.jbcrypt.BCrypt;


public class RegisterActivity extends AppCompatActivity {

    // Có thể dùng ViewBinding ở đây nếu muốn nhất quán
    private EditText edtEmail, edtUsername, edtPassword, edtConfirmPassword; // Thêm confirm password
    private Button btnRegister, btnToLogin;
    private UserDAO userDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Đảm bảo layout activity_register có đủ các view với ID đúng
        setContentView(R.layout.activity_register);
        Log.d("RegisterActivity", "onCreate");

        try {
            userDAO = new UserDAO(this); // Khởi tạo DAO
        } catch (Exception e) {
            Log.e("RegisterActivity", "Error initializing UserDAO", e);
            Toast.makeText(this, "Database error on startup.", Toast.LENGTH_LONG).show();
            // Có thể finish() activity nếu không có DAO
            finish();
            return;
        }


        // Ánh xạ View (Nên dùng ViewBinding thay thế)
        edtEmail = findViewById(R.id.edtEmail);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword); // *** Cần thêm EditText này vào layout ***
        btnRegister = findViewById(R.id.btnRegister);
        btnToLogin = findViewById(R.id.btnToLogin);

        // Kiểm tra null sau findViewById
        if (edtEmail == null || edtUsername == null || edtPassword == null || edtConfirmPassword == null || btnRegister == null || btnToLogin == null) {
            Log.e("RegisterActivity", "One or more views not found in activity_register.xml!");
            Toast.makeText(this, "Layout Error!", Toast.LENGTH_SHORT).show();
            // Có thể finish();
            return;
        }

        // Đặt listener
        btnRegister.setOnClickListener(view -> handleRegister());
        btnToLogin.setOnClickListener(view -> {
            Log.d("RegisterActivity", "Navigating to Login screen.");
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            // Không cần finish() ở đây nếu muốn người dùng có thể quay lại màn Register
        });
    }

    @Override
    protected void onDestroy() {
        Log.d("RegisterActivity", "onDestroy");
        if (userDAO != null) {
            userDAO.close(); // Đóng kết nối DAO khi Activity hủy
        }
        super.onDestroy();
    }

    private void handleRegister() {
        Log.d("RegisterActivity", "handleRegister called.");
        String email = edtEmail.getText().toString().trim();
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim(); // Lấy confirm password

        // --- Validate Input ---
        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin.");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Định dạng email không hợp lệ.");
            return;
        }
        if (password.length() < 6) { // Kiểm tra độ dài mật khẩu cơ bản
            showMessage("Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showMessage("Mật khẩu nhập lại không khớp.");
            return;
        }

        // --- Kiểm tra Email đã tồn tại chưa ---
        // Đảm bảo UserDAO có phương thức getUserByEmail
        try {
            if (userDAO.getUserById(edtUsername.getId()) != null) {
                showMessage("Email này đã được sử dụng.");
                return;
            }
            // (Tùy chọn) Kiểm tra Username đã tồn tại chưa (cần thêm getUserByUsername vào DAO)
            // if (userDAO.getUserByUsername(username) != null) {
            //    showMessage("Username này đã được sử dụng.");
            //    return;
            // }
        } catch (Exception e) {
            Log.e("RegisterActivity", "Error checking for existing user", e);
            showMessage("Lỗi khi kiểm tra dữ liệu người dùng.");
            return;
        }


        // --- Hash Mật khẩu ---
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        Log.d("RegisterActivity", "Password hashed successfully.");

        // --- Tạo đối tượng User ---
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(hashedPassword); // <<< LƯU MẬT KHẨU ĐÃ HASH
        user.setSubscriptionStatus("Free"); // Trạng thái mặc định
        user.setAdmin(false); // User đăng ký mặc định không phải admin

        // --- Thêm User vào DB bằng DAO ---
        try {
            // Gọi đúng phương thức addUser thay vì insertUser
            long result = userDAO.addUser(user);

            // Xử lý kết quả trả về từ DAO
            if (result >= 0) { // Thành công (addUser trả về row ID >= 0)
                Log.i("RegisterActivity", "Registration successful for email: " + email);
                showMessage("Đăng ký thành công!");
                // Chuyển sang màn hình Login sau khi đăng ký thành công
                Intent intent = new Intent(this, LoginActivity.class);
                // Xóa các activity trước đó khỏi stack để không quay lại Register bằng nút Back
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Đóng màn hình Register
            } else if (result == -2) { // Lỗi trùng lặp (-2 là mã lỗi tự đặt trong DAO)
                Log.w("RegisterActivity", "Registration failed: Duplicate email/username for " + email);
                showMessage("Email hoặc Username đã tồn tại."); // Thông báo lỗi cụ thể hơn
            } else { // Lỗi khác (ví dụ -1)
                Log.e("RegisterActivity", "Registration failed: DAO error for " + email + ", code: " + result);
                showMessage("Đăng ký thất bại. Vui lòng thử lại.");
            }
        } catch (Exception e) {
            Log.e("RegisterActivity", "Exception during addUser call", e);
            showMessage("Đã xảy ra lỗi trong quá trình đăng ký.");
        }
    }

    // Hàm tiện ích để hiển thị Toast
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}