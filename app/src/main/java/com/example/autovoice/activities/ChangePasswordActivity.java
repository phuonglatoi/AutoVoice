package com.example.autovoice.activities;

import android.content.Context; // Import Context
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
// Import ViewBinding class (tên dựa theo tên file layout)
import com.example.autovoice.databinding.ActivityChangePasswordBinding;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import Toolbar nếu bạn dùng


import com.example.autovoice.R; // Đảm bảo import R đúng
// Import Presenter và Contract View
import com.example.autovoice.presenters.ChangePasswordPresenter;

public class ChangePasswordActivity extends AppCompatActivity implements ChangePasswordPresenter.ChangePasswordView {

    private static final String TAG = "ChangePasswordActivity"; // Tag cho Log

    private ActivityChangePasswordBinding binding; // Sử dụng ViewBinding
    private ChangePasswordPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate layout bằng ViewBinding
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "onCreate: Activity created.");

        // Khởi tạo Presenter, truyền 'this' (vì Activity implement ChangePasswordView) và Context
        presenter = new ChangePasswordPresenter(this, getApplicationContext());

        // Thiết lập Toolbar (nếu layout của bạn có Toolbar với ID là toolbar_change_password)
        // Toolbar toolbar = findViewById(R.id.toolbar_change_password); // Hoặc binding.toolbarChangePassword
        // setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Thay Đổi Mật Khẩu");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút Back (Up)
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        } else {
            // Nếu không dùng ActionBar mặc định, bạn có thể set tiêu đề cho Toolbar trực tiếp
            // Hoặc nếu layout có TextView làm tiêu đề thì không cần
            Log.w(TAG, "SupportActionBar is null. Title or Up button might not be set automatically.");
        }


        // Xử lý sự kiện click nút "Lưu Thay Đổi"
        binding.btnSubmitChangePassword.setOnClickListener(v -> {
            String currentPassword = ""; // Khởi tạo để tránh lỗi nếu edtCurrentPassword là null
            String newPassword = "";
            String confirmNewPassword = "";

            if (binding.edtCurrentPassword != null) {
                currentPassword = binding.edtCurrentPassword.getText().toString().trim();
            } else {
                Log.e(TAG, "edtCurrentPassword is null!");
            }

            if (binding.edtNewPassword != null) {
                newPassword = binding.edtNewPassword.getText().toString().trim();
            } else {
                Log.e(TAG, "edtNewPassword is null!");
            }

            if (binding.edtConfirmNewPassword != null) {
                confirmNewPassword = binding.edtConfirmNewPassword.getText().toString().trim();
            } else {
                Log.e(TAG, "edtConfirmNewPassword is null!");
            }

            Log.d(TAG, "Submit Change Password button clicked.");
            presenter.changePassword(currentPassword, newPassword, confirmNewPassword);
        });

        // Xử lý nút "Hủy" (nếu có trong layout của bạn, ví dụ btnCancelChange)
        // Nếu không có nút Hủy riêng, người dùng sẽ dùng nút Back của hệ thống/ActionBar
        // if (binding.btnCancelChange != null) {
        //    binding.btnCancelChange.setOnClickListener(v -> {
        //        Log.d(TAG, "Cancel button clicked.");
        //        finish(); // Đóng Activity
        //    });
        // }
    }

    // --- Implementation của ChangePasswordPresenter.ChangePasswordView ---
    @Override
    public void showCurrentPasswordError(String message) {
        if (binding != null && binding.tilCurrentPassword != null) {
            binding.tilCurrentPassword.setError(message);
            binding.edtCurrentPassword.requestFocus(); // Focus vào trường lỗi
        }
        Log.w(TAG, "Current Password Error: " + message);
    }

    @Override
    public void showNewPasswordError(String message) {
        if (binding != null && binding.tilNewPassword != null) {
            binding.tilNewPassword.setError(message);
            binding.edtNewPassword.requestFocus();
        }
        Log.w(TAG, "New Password Error: " + message);
    }

    @Override
    public void showConfirmPasswordError(String message) {
        if (binding != null && binding.tilConfirmNewPassword != null) {
            binding.tilConfirmNewPassword.setError(message);
            binding.edtConfirmNewPassword.requestFocus();
        }
        Log.w(TAG, "Confirm Password Error: " + message);
    }

    @Override
    public void clearErrors() {
        if (binding != null) {
            if (binding.tilCurrentPassword != null) binding.tilCurrentPassword.setError(null);
            if (binding.tilNewPassword != null) binding.tilNewPassword.setError(null);
            if (binding.tilConfirmNewPassword != null) binding.tilConfirmNewPassword.setError(null);
        }
        Log.d(TAG, "Input errors cleared.");
    }

    @Override
    public void showChangePasswordSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.i(TAG, "Password changed successfully: " + message);
        finish(); // Đóng Activity này sau khi đổi mật khẩu thành công
    }

    @Override
    public void showChangePasswordFailure(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Password change failed: " + message);
    }

    @Override
    public void showProgress() {
        // TODO: Hiển thị ProgressBar nếu bạn có một ProgressBar trong layout
        // Ví dụ: if (binding != null && binding.progressBar != null) binding.progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Showing progress (ProgressBar display not implemented).");
    }

    @Override
    public void hideProgress() {
        // TODO: Ẩn ProgressBar nếu có
        // Ví dụ: if (binding != null && binding.progressBar != null) binding.progressBar.setVisibility(View.GONE);
        Log.d(TAG, "Hiding progress (ProgressBar display not implemented).");
    }

    // Xử lý sự kiện khi người dùng nhấn nút "Up" (Back) trên ActionBar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Hoặc finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            presenter.onDestroy(); // Thông báo cho presenter để giải phóng tài nguyên (ví dụ đóng DAO)
        }
        binding = null; // Giải phóng đối tượng binding
        Log.d(TAG, "onDestroy called.");
    }
}
