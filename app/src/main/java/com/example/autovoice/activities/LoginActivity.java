package com.example.autovoice.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.autovoice.R; // Đảm bảo import R đúng
import com.example.autovoice.models.User;
import com.example.autovoice.presenters.LoginPresenter;

// Các import cần thiết cho Google Sign-In
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton; // Import SignInButton
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity implements LoginPresenter.LoginView {

    private static final String TAG = "LoginActivity_GoogleFix"; // Đổi TAG để dễ theo dõi

    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnRegister;
    private SignInButton btnGoogleSignIn;
    private LoginPresenter presenter;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d(TAG, "onCreate called");

        presenter = new LoginPresenter(this);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        if (edtEmail == null || edtPassword == null || btnLogin == null || btnRegister == null || btnGoogleSignIn == null) {
            Log.e(TAG, "One or more views not found! Check activity_login.xml IDs.");
            Toast.makeText(this, "Lỗi giao diện, vui lòng thử lại!", Toast.LENGTH_LONG).show();
            finish(); // Đóng activity nếu có lỗi nghiêm trọng
            return;
        }

        // --- CẤU HÌNH GOOGLE SIGN-IN ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        // ---------------------------------

        // --- ĐĂNG KÝ ACTIVITYRESULTLAUNCHER CHO GOOGLE SIGN-IN ---
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Google Sign-In Activity Result Code: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignInResult(task);
                    } else {
                        Log.w(TAG, "Google Sign-In failed or was cancelled by user. Result Code: " + result.getResultCode());
                        onLoginFailed("Đăng nhập với Google thất bại hoặc đã bị hủy.");
                    }
                });
        // ------------------------------------------------------

        btnLogin.setOnClickListener(view -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            Log.d(TAG, "Email/Password Login button clicked for email: " + email);
            if (presenter != null) {
                presenter.login(email, password);
            }
        });

        btnRegister.setOnClickListener(view -> {
            Log.d(TAG, "Register button clicked, navigating to RegisterActivity.");
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        btnGoogleSignIn.setOnClickListener(view -> {
            Log.d(TAG, "Google Sign-In button clicked.");
            // --- THAY ĐỔI Ở ĐÂY: Đăng xuất trước khi đăng nhập lại để luôn hiển thị chọn tài khoản ---
            if (mGoogleSignInClient != null) {
                mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                    // Sau khi signOut hoàn tất (hoặc thất bại, không quá quan trọng ở đây)
                    // thì tiến hành signIn như bình thường
                    Log.d(TAG, "Attempted to signOut from Google before new sign-in. Task successful: " + task.isSuccessful());
                    signInWithGoogleFlow(); // Gọi hàm bắt đầu luồng đăng nhập
                });
            } else {
                // Nếu mGoogleSignInClient chưa sẵn sàng, vẫn thử đăng nhập
                signInWithGoogleFlow();
            }
            // ------------------------------------------------------------------------------------
        });
    }

    // Đổi tên phương thức cũ signInWithGoogle thành signInWithGoogleFlow
    private void signInWithGoogleFlow() {
        Log.d(TAG, "Initiating Google Sign-In flow.");
        if (mGoogleSignInClient == null) {
            Log.e(TAG, "GoogleSignInClient is not initialized in signInWithGoogleFlow!");
            onLoginFailed("Lỗi cấu hình đăng nhập Google.");
            return;
        }
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.i(TAG, "Google Sign-In successful. Email: " + account.getEmail() + ", Name: " + account.getDisplayName());
            if (presenter != null) {
                presenter.processGoogleSignIn(account);
            } else {
                Log.e(TAG, "Presenter is null, cannot process Google Sign-In result.");
                onLoginFailed("Lỗi hệ thống khi xử lý đăng nhập Google.");
            }
        } catch (ApiException e) {
            Log.w(TAG, "Google Sign-In failed with ApiException. Status code: " + e.getStatusCode(), e);
            String errorMessage = "Đăng nhập với Google thất bại. Mã lỗi: " + e.getStatusCode();
            onLoginFailed(errorMessage);
        } catch (Exception e) {
            Log.e(TAG, "An unexpected error occurred during Google Sign-In handling.", e);
            onLoginFailed("Lỗi không xác định khi đăng nhập Google.");
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called.");
        if (presenter != null) {
            presenter.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onLoginSuccess(User user) {
        if (user == null) {
            Log.e(TAG, "onLoginSuccess called with null user object!");
            onLoginFailed("onLoginSuccess called with null user object!");
            return;
        }
        Log.i(TAG, "Login successful. User: " + user.getUsername() + ", isAdmin: " + user.isAdmin() + ", AuthProvider: " + user.getAuthProvider());
        Toast.makeText(this, "Login Success!", Toast.LENGTH_SHORT).show();

        Intent intent;
        if (user.isAdmin()) {
            Log.d(TAG, "Navigating to AdminDashboardActivity.");
            intent = new Intent(this, AdminDashboardActivity.class);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        } else {
            Log.d(TAG, "Navigating to MainUserActivity.");
            intent = new Intent(this, MainUserActivity.class);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onLoginFailed(String message) {
        Log.w(TAG, "Login failed: " + message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    // public void onLogoutComplete() {
    //     Log.d(TAG, "onLogoutComplete called.");
    // }
}
