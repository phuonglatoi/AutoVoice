package com.example.autovoice.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.autovoice.R;
import com.example.autovoice.databinding.FragmentProfileBinding;
import com.example.autovoice.models.User;
import com.example.autovoice.activities.MainUserActivity;
import com.example.autovoice.activities.ChangePasswordActivity; // Import ChangePasswordActivity
import com.example.autovoice.utils.SessionManager;
import com.example.autovoice.database.UserDAO;

// Import thư viện Glide nếu bạn quyết định dùng để tải ảnh từ URL
// import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment_Google";

    private FragmentProfileBinding binding;
    private SessionManager sessionManager;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private User currentUser;
    private UserDAO userDAO;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        userDAO = new UserDAO(requireContext());
        currentUser = sessionManager.getLoggedInUser();

        if (currentUser != null) {
            Log.d(TAG, "onCreate - Initial currentUser - ID: " + currentUser.getId() +
                    ", Username: " + currentUser.getUsername() +
                    ", AuthProvider: " + currentUser.getAuthProvider()); // Log thêm AuthProvider
        } else {
            Log.e(TAG, "onCreate - CRITICAL: currentUser is NULL from SessionManager!");
            handleUserNotLoggedIn();
        }

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            Log.d(TAG, "Image selected: " + imageUri.toString());
                            saveAndDisplayImage(imageUri);
                        } else {
                            Log.w(TAG, "Image URI is null after selection.");
                        }
                    } else {
                        Log.d(TAG, "Image picking cancelled or failed. Result code: " + result.getResultCode());
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        Log.d(TAG, "onCreateView: Layout inflated.");
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Views are ready.");

        if (currentUser == null && sessionManager != null) {
            currentUser = sessionManager.getLoggedInUser();
        }

        if (currentUser == null) {
            Log.e(TAG, "onViewCreated - CRITICAL: currentUser is STILL NULL!");
            handleUserNotLoggedIn();
            return;
        }
        Log.d(TAG, "onViewCreated - CurrentUser ID: " + currentUser.getId() +
                ", AuthProvider: " + currentUser.getAuthProvider());

        loadUserProfile();

        binding.ivProfileImage.setOnClickListener(v -> openImageChooser());

        // Sự kiện click nút thay đổi mật khẩu
        binding.btnChangePassword.setOnClickListener(v -> {
            Log.d(TAG, "Change Password button clicked.");
            // Kiểm tra xem có nên cho phép đổi mật khẩu không (ví dụ, không cho nếu đăng nhập bằng Google)
            if (currentUser != null && !"GOOGLE".equals(currentUser.getAuthProvider())) {
                Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
                startActivity(intent);
            } else if (currentUser != null && "GOOGLE".equals(currentUser.getAuthProvider())) {
                Toast.makeText(getContext(), "Bạn đã đăng nhập bằng Google, không cần đặt mật khẩu riêng.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "Lỗi thông tin người dùng.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnLogoutProfile.setOnClickListener(v -> {
            Log.d(TAG, "Logout button clicked.");
            Activity currentActivity = getActivity();
            if (currentActivity instanceof MainUserActivity) {
                ((MainUserActivity) currentActivity).performUserLogout();
            } else {
                Log.w(TAG, "Cannot perform logout: Activity is not MainUserActivity or is null.");
                Toast.makeText(getContext(), "Lỗi khi đăng xuất.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called.");
        if (sessionManager != null) {
            currentUser = sessionManager.getLoggedInUser(); // Luôn lấy user mới nhất từ session
            if (currentUser != null) {
                Log.d(TAG, "onResume - Refetched currentUser - ID: " + currentUser.getId() +
                        ", AuthProvider: " + currentUser.getAuthProvider() +
                        ", ImagePath: " + currentUser.getProfileImageLocalPath());
                loadUserProfile();
            } else {
                Log.e(TAG, "onResume - currentUser is NULL!");
                handleUserNotLoggedIn();
            }
        }
    }

    private void loadUserProfile() {
        if (binding == null) {
            Log.e(TAG, "loadUserProfile - Binding is null.");
            return;
        }
        if (currentUser != null) {
            Log.d(TAG, "loadUserProfile - Displaying for User ID: " + currentUser.getId() +
                    ", Username: " + currentUser.getUsername() +
                    ", AuthProvider: " + currentUser.getAuthProvider());

            binding.tvProfileName.setText(currentUser.getUsername());
            binding.tvProfileEmail.setText(currentUser.getEmail());

            // --- XỬ LÝ HIỂN THỊ NÚT THAY ĐỔI MẬT KHẨU ---
            if ("GOOGLE".equals(currentUser.getAuthProvider())) {
                binding.btnChangePassword.setVisibility(View.GONE); // Ẩn nút nếu đăng nhập bằng Google
                Log.d(TAG, "User logged in with Google, hiding Change Password button.");
            } else {
                binding.btnChangePassword.setVisibility(View.VISIBLE); // Hiện nút nếu đăng nhập bằng Email
                Log.d(TAG, "User logged in with Email, showing Change Password button.");
            }
            // ------------------------------------------

            String localPath = currentUser.getProfileImageLocalPath();
            // Nếu đăng nhập bằng Google và có photoUrl, bạn có thể ưu tiên tải từ URL đó
            // String googlePhotoUrl = currentUser.getGooglePhotoUrl(); // Giả sử bạn lưu URL ảnh Google
            // if ("GOOGLE".equals(currentUser.getAuthProvider()) && !TextUtils.isEmpty(googlePhotoUrl) && getContext() != null) {
            //    Log.d(TAG, "Loading Google profile image from URL: " + googlePhotoUrl);
            //    Glide.with(getContext()).load(googlePhotoUrl).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).circleCrop().into(binding.ivProfileImage);
            // } else
            if (!TextUtils.isEmpty(localPath)) {
                File imageFile = new File(localPath);
                if (imageFile.exists() && imageFile.isFile()) {
                    binding.ivProfileImage.setImageURI(Uri.fromFile(imageFile));
                    Log.i(TAG, "Profile image LOADED successfully from local path: " + localPath);
                } else {
                    Log.w(TAG, "Profile image file DOES NOT EXIST at: [" + localPath + "]. Setting placeholder.");
                    binding.ivProfileImage.setImageResource(R.drawable.ic_profile_placeholder);
                }
            } else {
                Log.d(TAG, "Profile image local path is empty. Setting placeholder.");
                binding.ivProfileImage.setImageResource(R.drawable.ic_profile_placeholder);
            }
        } else {
            Log.w(TAG, "loadUserProfile - Current user is null.");
            binding.tvProfileName.setText("Khách");
            binding.tvProfileEmail.setText("Không có thông tin");
            binding.ivProfileImage.setImageResource(R.drawable.ic_profile_placeholder);
            binding.btnChangePassword.setVisibility(View.VISIBLE); // Mặc định hiện nếu không có thông tin user
        }
    }

    private void openImageChooser() {
        // ... (giữ nguyên)
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
        Log.d(TAG, "Image chooser intent launched.");
    }

    private void saveAndDisplayImage(Uri originalImageUri) {
        // ... (giữ nguyên logic saveAndDisplayImage, đảm bảo nó gọi userDAO.updateProfileImagePath)
        if (currentUser == null) {
            Toast.makeText(getContext(), "Lỗi: Người dùng không xác định.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "saveAndDisplayImage - currentUser is null.");
            return;
        }
        if (getContext() == null) {
            Log.e(TAG, "saveAndDisplayImage - Context is null.");
            return;
        }

        int userId = currentUser.getId();
        Log.i(TAG, "saveAndDisplayImage - START - User ID for filename: " + userId);

        if (userId == 0) {
            Log.e(TAG, "saveAndDisplayImage - Invalid user ID (0). Cannot save image.");
            Toast.makeText(getContext(), "Lỗi ID người dùng không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }
        String fileName = "user_" + userId + "_profile.jpg";

        File directory = new File(getContext().getFilesDir(), "profile_images");
        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(TAG, "saveAndDisplayImage - Failed to create profile_images directory.");
            Toast.makeText(getContext(), "Lỗi tạo thư mục lưu ảnh.", Toast.LENGTH_SHORT).show();
            return;
        }

        File destinationFile = new File(directory, fileName);
        String oldPath = currentUser.getProfileImageLocalPath();
        if (!TextUtils.isEmpty(oldPath) && !oldPath.equals(destinationFile.getAbsolutePath())) {
            File oldFile = new File(oldPath);
            if (oldFile.exists() && oldFile.delete()) {
                Log.d(TAG, "saveAndDisplayImage - Old profile image deleted: " + oldPath);
            }
        }

        try (InputStream inputStream = getContext().getContentResolver().openInputStream(originalImageUri);
             OutputStream outputStream = new FileOutputStream(destinationFile)) {
            if (inputStream == null) throw new IOException("Unable to open input stream from URI.");
            byte[] buffer = new byte[4 * 1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);
            outputStream.flush();

            String newPath = destinationFile.getAbsolutePath();
            currentUser.setProfileImageLocalPath(newPath);
            sessionManager.saveUserSession(currentUser);

            if (userDAO != null) {
                boolean dbUpdateSuccess = userDAO.updateProfileImagePath(currentUser.getId(), newPath);
                if (dbUpdateSuccess) Log.i(TAG, "Profile image path UPDATED IN DATABASE for User ID: " + currentUser.getId());
                else Log.e(TAG, "FAILED to update profile image path in DATABASE for User ID: " + currentUser.getId());
            } else Log.e(TAG, "UserDAO is null. Cannot update database.");

            if (binding != null) binding.ivProfileImage.setImageURI(Uri.fromFile(destinationFile));
            Toast.makeText(getContext(), "Ảnh đại diện đã được cập nhật!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "saveAndDisplayImage - Error saving image", e);
            Toast.makeText(getContext(), "Lỗi khi lưu ảnh.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleUserNotLoggedIn() {
        // ... (giữ nguyên)
        if (isAdded() && getActivity() instanceof MainUserActivity) {
            Toast.makeText(getContext(), "Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            ((MainUserActivity) getActivity()).navigateToLoginScreen();
        } else if (isAdded()){
            Toast.makeText(getContext(), "Lỗi người dùng.", Toast.LENGTH_LONG).show();
        }
        Log.e(TAG, "handleUserNotLoggedIn - User not logged in or fragment not attached properly.");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        Log.d(TAG, "onDestroyView: Binding set to null.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (userDAO != null) {
            userDAO.close();
            Log.d(TAG, "UserDAO closed in onDestroy.");
        }
    }
}
