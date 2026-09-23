package com.example.autovoice.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.autovoice.R;
import com.example.autovoice.models.User;
import com.example.autovoice.utils.SessionManager;

public class SplashActivity extends Activity {

    private Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Gắn layout splash.xml
        setContentView(R.layout.activity_splash);

        // Tìm nút trong layout
        btnStart = findViewById(R.id.btnStart);

        // Thiết lập sự kiện click cho nút "Bắt đầu"
        btnStart.setOnClickListener(v -> {
            SessionManager sessionManager = new SessionManager(SplashActivity.this);
            User currentUser = sessionManager.getLoggedInUser();

            if (currentUser != null) {
                if (currentUser.isAdmin()) {
                    // Chuyển đến màn hình Admin
                    startActivity(new Intent(SplashActivity.this, AdminDashboardActivity.class));
                } else {
                    // Chuyển đến màn hình người dùng
                    startActivity(new Intent(SplashActivity.this, MainUserActivity.class));
                }
            } else {
                // Nếu không có người dùng đã đăng nhập, chuyển đến LoginActivity
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }

            finish(); // Đóng SplashActivity
        });
    }
    public void createAdminAccountIfNeeded(SQLiteDatabase db) {
        // Kiểm tra xem bảng 'users' có chứa tài khoản admin hay không
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE is_admin = 1", null);

        if (cursor.getCount() > 0) {
            // Nếu đã có tài khoản admin, xóa tài khoản admin cũ
            db.execSQL("DELETE FROM users WHERE is_admin = 1");
        }

        // Tạo tài khoản admin mặc định
        String insertAdmin = "INSERT INTO users (email, username, password, subscription, is_admin) " +
                "VALUES ('autovoice', 'admin', 'admin123', 'Premium', 1)";
        db.execSQL(insertAdmin); // Thực thi câu lệnh SQL để chèn tài khoản admin vào bảng

        cursor.close();
    }


}
