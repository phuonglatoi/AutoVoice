package com.example.autovoice.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
// Import Toolbar nếu bạn dùng setSupportActionBar
import androidx.appcompat.widget.Toolbar;


import com.example.autovoice.R;
import com.example.autovoice.adapters.AdminUserAdapter;
import com.example.autovoice.databinding.ActivityAdminDashboardBinding; // Import Binding Class
import com.example.autovoice.models.User;
import com.example.autovoice.presenters.AdminDashboardPresenter;
import com.google.android.material.textfield.TextInputEditText; // Import TextInputEditText

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Implement cả 2 interface: Contract View của Presenter và Listener của Adapter
public class AdminDashboardActivity extends AppCompatActivity
        implements AdminDashboardPresenter.AdminDashboardContract.View, AdminUserAdapter.OnUserActionListener {

    private ActivityAdminDashboardBinding binding; // Sử dụng ViewBinding
    private AdminUserAdapter adapter;
    private AdminDashboardPresenter presenter;
    private AlertDialog addUserDialogInstance;
    private AlertDialog editPasswordDialogInstance; // Dialog để sửa mật khẩu
    private AlertDialog upgradePackageDialogInstance; // Dialog để nâng cấp gói

    // Danh sách các gói dịch vụ (ví dụ, bạn có thể lấy từ server hoặc định nghĩa ở đâu đó)
    private final String[] subscriptionOptions = {"Free", "Premium", "Pro", "Enterprise"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("AdminDashboardActivity", "onCreate started.");

        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarAdmin);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }

        presenter = new AdminDashboardPresenter(this);
        Log.d("AdminDashboardActivity", "Presenter initialized.");

        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUserAdapter(this, new ArrayList<>(), this);
        binding.rvUsers.setAdapter(adapter);
        Log.d("AdminDashboardActivity", "RecyclerView and Adapter initialized.");

        binding.fabAddUser.setOnClickListener(v -> {
            Log.d("AdminDashboardActivity", "Add User FAB clicked.");
            presenter.addUserRequested(); // Gọi presenter yêu cầu hiển thị dialog
        });

        binding.btnLogout.setOnClickListener(v -> {
            Log.d("AdminDashboardActivity", "Logout button clicked.");
            showLogoutConfirmationDialog();
        });

        Log.d("AdminDashboardActivity", "Requesting initial user list load.");
        presenter.loadAllUsers();
    }

    @Override
    protected void onDestroy() {
        Log.d("AdminDashboardActivity", "onDestroy called.");
        if (addUserDialogInstance != null && addUserDialogInstance.isShowing()) {
            addUserDialogInstance.dismiss();
        }
        if (editPasswordDialogInstance != null && editPasswordDialogInstance.isShowing()) {
            editPasswordDialogInstance.dismiss();
        }
        if (upgradePackageDialogInstance != null && upgradePackageDialogInstance.isShowing()) {
            upgradePackageDialogInstance.dismiss();
        }
        if (presenter != null) {
            presenter.onDestroy();
        }
        binding = null;
        super.onDestroy();
    }

    // --- Implementation of AdminDashboardContract.View ---
    @Override
    public void displayUsers(List<User> userList) {
        Log.i("AdminDashboardActivity", "Displaying " + (userList != null ? userList.size() : 0) + " users.");
        if (adapter != null) {
            adapter.updateUserList(userList);
            if (userList == null || userList.isEmpty()) {
                showMessage("User list is empty.");
            }
        } else {
            Log.e("AdminDashboardActivity", "Adapter is null in displayUsers!");
        }
    }

    @Override
    public void showMessage(String message) {
        Log.d("AdminDashboardActivity", "Showing message: " + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void navigateToLogin() {
        Log.i("AdminDashboardActivity", "Navigating back to LoginActivity.");
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void showAddUserDialog() {
        Log.d("AdminDashboardActivity", "Showing Add User dialog.");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_user, null); // Inflate layout dialog_add_user

        final EditText etEmail = dialogView.findViewById(R.id.et_dialog_email);
        final EditText etUsername = dialogView.findViewById(R.id.et_dialog_username);
        // Đảm bảo ID trong dialog_add_user.xml là et_dialog_password và et_dialog_confirm_password
        // và chúng là TextInputEditText (hoặc bạn ép kiểu sang EditText nếu layout dùng EditText)
        final TextInputEditText etPassword = dialogView.findViewById(R.id.et_dialog_password);
        final TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.et_dialog_confirm_password);
        final CheckBox cbIsAdmin = dialogView.findViewById(R.id.cb_dialog_is_admin);

        if (etEmail == null || etUsername == null || etPassword == null || etConfirmPassword == null || cbIsAdmin == null) {
            Log.e("AdminDashboardActivity", "One or more views in dialog_add_user.xml not found! Check IDs and View types.");
            showMessage("Lỗi tạo dialog thêm người dùng."); // Thông báo lỗi cụ thể hơn
            return; // Dừng lại nếu không tìm thấy view
        }

        builder.setView(dialogView)
                .setPositiveButton("Add", null) // Override listener
                .setNegativeButton("Cancel", (dialog, id) -> {
                    Log.d("AdminDashboardActivity", "Add User dialog cancelled.");
                    dialog.cancel();
                });

        addUserDialogInstance = builder.create();
        addUserDialogInstance.setOnShowListener(dialogInterface -> {
            Button button = addUserDialogInstance.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String email = etEmail.getText().toString().trim();
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();
                boolean isAdmin = cbIsAdmin.isChecked();

                Log.d("AdminDashboardActivity", "Add button in dialog clicked.");
                presenter.addUser(email, username, password, confirmPassword, isAdmin);
            });
        });
        addUserDialogInstance.show();
    }

    @Override
    public void showAddUserSuccess() {
        Log.d("AdminDashboardActivity", "Add user successful callback received, dismissing dialog.");
        if (addUserDialogInstance != null && addUserDialogInstance.isShowing()) {
            addUserDialogInstance.dismiss();
        }
    }

    @Override
    public void showEditPasswordDialog(User userToEdit) {
        // ... (Code cho showEditPasswordDialog như đã cung cấp trước đó)
        if (userToEdit == null) {
            showMessage("Không có thông tin người dùng để sửa mật khẩu.");
            return;
        }
        Log.d("AdminDashboardActivity", "Showing edit password dialog for user: " + userToEdit.getUsername());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_admin_edit_password, null);

        final TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogEditPasswordTitle);
        final TextInputEditText edtAdminNewPassword = dialogView.findViewById(R.id.edtAdminNewPassword);
        final TextInputEditText edtAdminConfirmNewPassword = dialogView.findViewById(R.id.edtAdminConfirmNewPassword);
        final TextView tvUserInfoForPasswordReset = dialogView.findViewById(R.id.tvUserInfoForPasswordReset);


        if (tvDialogTitle == null || edtAdminNewPassword == null || edtAdminConfirmNewPassword == null || tvUserInfoForPasswordReset == null) {
            Log.e("AdminDashboardActivity", "One or more views in dialog_admin_edit_password.xml not found!");
            showMessage("Lỗi hiển thị dialog sửa mật khẩu.");
            return;
        }
        if(tvUserInfoForPasswordReset != null) {
            tvUserInfoForPasswordReset.setText("Người dùng: " + userToEdit.getUsername());
        } else {
            tvDialogTitle.setText("Đặt lại mật khẩu cho: " + userToEdit.getUsername());
        }


        builder.setView(dialogView)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", (dialog, id) -> dialog.cancel());

        editPasswordDialogInstance = builder.create();
        editPasswordDialogInstance.setOnShowListener(dialogInterface -> {
            Button saveButton = editPasswordDialogInstance.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(view -> {
                String newPassword = edtAdminNewPassword.getText().toString().trim();
                String confirmNewPassword = edtAdminConfirmNewPassword.getText().toString().trim();
                presenter.adminSetUserPassword(userToEdit.getId(), newPassword, confirmNewPassword);
            });
        });
        editPasswordDialogInstance.show();
    }

    @Override
    public void showUpgradePackageDialog(User userToEdit) {
        // ... (Code cho showUpgradePackageDialog như đã cung cấp trước đó)
        if (userToEdit == null) {
            showMessage("Không có thông tin người dùng để nâng cấp gói.");
            Log.e("AdminDashboardActivity", "userToEdit is null in showUpgradePackageDialog");
            return;
        }
        Log.d("AdminDashboardActivity", "Showing upgrade package dialog for user: " + userToEdit.getUsername());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_admin_upgrade_package, null);

        final TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogUpgradeTitle);
        final TextView tvUserInfoForPackageUpgrade = dialogView.findViewById(R.id.tvUserInfoForPackageUpgrade); // Thêm ID này vào layout nếu chưa có
        final TextView tvCurrentUserPackage = dialogView.findViewById(R.id.tvCurrentUserPackage);
        final Spinner spinnerNewPackage = dialogView.findViewById(R.id.spinnerNewPackage);

        if (tvDialogTitle == null || tvCurrentUserPackage == null || spinnerNewPackage == null || tvUserInfoForPackageUpgrade == null) {
            Log.e("AdminDashboardActivity", "One or more views in dialog_admin_upgrade_package.xml not found!");
            showMessage("Lỗi hiển thị dialog nâng cấp gói.");
            return;
        }

        // tvDialogTitle.setText("Nâng cấp gói cho: " + userToEdit.getUsername()); // Tiêu đề đã có trong XML
        tvUserInfoForPackageUpgrade.setText("Người dùng: " + userToEdit.getUsername());
        tvCurrentUserPackage.setText("Gói hiện tại: " + (userToEdit.getSubscriptionStatus() != null ? userToEdit.getSubscriptionStatus() : "N/A"));

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, subscriptionOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNewPackage.setAdapter(spinnerAdapter);

        if (userToEdit.getSubscriptionStatus() != null) {
            int currentPackagePosition = Arrays.asList(subscriptionOptions).indexOf(userToEdit.getSubscriptionStatus());
            if (currentPackagePosition >= 0) {
                spinnerNewPackage.setSelection(currentPackagePosition);
            } else {
                Log.w("AdminDashboardActivity", "Current package '" + userToEdit.getSubscriptionStatus() + "' not in options. Spinner will show default.");
            }
        }

        builder.setView(dialogView)
                .setPositiveButton("Lưu Thay Đổi", null)
                .setNegativeButton("Hủy", (dialog, id) -> dialog.cancel());

        upgradePackageDialogInstance = builder.create();
        upgradePackageDialogInstance.setOnShowListener(dialogInterface -> {
            Button saveButton = upgradePackageDialogInstance.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(view -> {
                String selectedPackage = spinnerNewPackage.getSelectedItem().toString();
                Log.d("AdminDashboardActivity", "Save button in upgrade dialog. Selected: " + selectedPackage + " for user ID: " + userToEdit.getId());
                presenter.adminUpdateUserPackage(userToEdit.getId(), selectedPackage);
            });
        });
        upgradePackageDialogInstance.show();
    }

    @Override
    public void closeEditDialogs() {
        if (editPasswordDialogInstance != null && editPasswordDialogInstance.isShowing()) {
            editPasswordDialogInstance.dismiss();
        }
        if (upgradePackageDialogInstance != null && upgradePackageDialogInstance.isShowing()) {
            upgradePackageDialogInstance.dismiss();
        }
    }


    @Override
    public Context getContext() {
        return this;
    }

    // --- Implementation of AdminUserAdapter.OnUserActionListener ---
    @Override
    public void onDeleteUser(User user) {
        if (user == null) {
            Log.e("AdminDashboardActivity", "onDeleteUser called with null user!");
            return;
        }
        Log.w("AdminDashboardActivity", "Delete action requested for user: " + user.getUsername());
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Bạn có chắc muốn xóa người dùng '" + user.getUsername() + "'?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    Log.d("AdminDashboardActivity", "Deletion confirmed for: " + user.getUsername());
                    if (presenter != null) {
                        presenter.deleteUserByUsername(user.getUsername());
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @Override
    public void onEditUserPassword(User user) {
        if (user != null && presenter != null) {
            Log.d("AdminDashboardActivity", "onEditUserPassword called for user: " + user.getUsername());
            presenter.editUserPasswordRequested(user);
        } else {
            Log.w("AdminDashboardActivity", "onEditUserPassword: User or Presenter is null.");
        }
    }

    @Override
    public void onUpgradeUserPackage(User user) {
        if (user != null && presenter != null) {
            Log.d("AdminDashboardActivity", "onUpgradeUserPackage called for user: " + user.getUsername());
            presenter.upgradeUserPackageRequested(user);
        } else {
            Log.w("AdminDashboardActivity", "onUpgradeUserPackage: User or Presenter is null.");
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    Log.d("AdminDashboardActivity", "Logout confirmed by user.");
                    if(presenter != null) {
                        presenter.handleLogout();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
