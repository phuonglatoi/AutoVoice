package com.example.autovoice.models;

public class User {
    private int id; // ID người dùng, rất quan trọng
    private String email;
    private String username;
    private String password; // Nên là mật khẩu đã hash
    private String subscriptionStatus;
    private boolean isAdmin;
    private String profileImageLocalPath; // Đường dẫn ảnh đại diện lưu cục bộ
    private String authProvider; // Ví dụ: "EMAIL", "GOOGLE"
    // private String googlePhotoUrl; // Tùy chọn: nếu muốn lưu URL ảnh từ Google

    public User() {}

    // Constructor có thể cần cập nhật tùy theo cách bạn tạo User từ DAO
    public User(int id, String email, String username, String password,
                String subscriptionStatus, boolean isAdmin,
                String profileImageLocalPath, String authProvider) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.subscriptionStatus = subscriptionStatus;
        this.isAdmin = isAdmin;
        this.profileImageLocalPath = profileImageLocalPath;
        this.authProvider = authProvider;
    }

    // Getters và Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; } // Nhận mật khẩu (đã hash)

    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { this.isAdmin = admin; }

    public String getProfileImageLocalPath() { return profileImageLocalPath; }
    public void setProfileImageLocalPath(String profileImageLocalPath) { this.profileImageLocalPath = profileImageLocalPath; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    // public String getGooglePhotoUrl() { return googlePhotoUrl; }
    // public void setGooglePhotoUrl(String googlePhotoUrl) { this.googlePhotoUrl = googlePhotoUrl; }
}
