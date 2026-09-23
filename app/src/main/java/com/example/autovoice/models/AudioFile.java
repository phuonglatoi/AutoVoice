package com.example.autovoice.models;

public class AudioFile {
    private int id; // ID tự tăng trong CSDL
    private int userId; // ID của người dùng sở hữu file này
    private String fileName; // Tên file được lưu trong bộ nhớ trong của ứng dụng (ví dụ: audio_xin_chao_20250514_113000.mp3)
    private String localPath; // Đường dẫn đầy đủ đến file trong bộ nhớ trong của ứng dụng
    private String originalTextSnippet; // Một đoạn ngắn của văn bản gốc (ví dụ: 50 ký tự đầu)
    private long createdAtTimestamp; // Thời gian tạo file (dưới dạng timestamp long)
    private String displayNameForDownload; // Tên file gợi ý khi người dùng tải xuống (ví dụ: AutoVoice_Xin_chao.mp3)
    private String voiceDetails; // Thông tin giọng đọc đã dùng (ví dụ: "vi-VN-Wavenet-A, Tốc độ: 1.0x")

    public AudioFile() {
    }

    // Constructor có thể dùng khi tạo mới hoặc đọc từ DB
    public AudioFile(int id, int userId, String fileName, String localPath, String originalTextSnippet, long createdAtTimestamp, String displayNameForDownload, String voiceDetails) {
        this.id = id;
        this.userId = userId;
        this.fileName = fileName;
        this.localPath = localPath;
        this.originalTextSnippet = originalTextSnippet;
        this.createdAtTimestamp = createdAtTimestamp;
        this.displayNameForDownload = displayNameForDownload;
        this.voiceDetails = voiceDetails;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getOriginalTextSnippet() {
        return originalTextSnippet;
    }

    public void setOriginalTextSnippet(String originalTextSnippet) {
        this.originalTextSnippet = originalTextSnippet;
    }

    public long getCreatedAtTimestamp() {
        return createdAtTimestamp;
    }

    public void setCreatedAtTimestamp(long createdAtTimestamp) {
        this.createdAtTimestamp = createdAtTimestamp;
    }

    public String getDisplayNameForDownload() {
        return displayNameForDownload;
    }

    public void setDisplayNameForDownload(String displayNameForDownload) {
        this.displayNameForDownload = displayNameForDownload;
    }

    public String getVoiceDetails() {
        return voiceDetails;
    }

    public void setVoiceDetails(String voiceDetails) {
        this.voiceDetails = voiceDetails;
    }
}
