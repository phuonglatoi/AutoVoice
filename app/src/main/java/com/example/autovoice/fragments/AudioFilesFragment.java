package com.example.autovoice.fragments;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.autovoice.R;
import com.example.autovoice.adapters.AudioFileListAdapter;
import com.example.autovoice.database.AudioDAO; // <<< Đảm bảo import đúng tên lớp DAO
import com.example.autovoice.models.AudioFile;
import com.example.autovoice.models.User;
import com.example.autovoice.activities.MainUserActivity;
import com.example.autovoice.utils.SessionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException; // <<< Thêm import
import java.io.FileOutputStream; // <<< Thêm import (mặc dù có thể không dùng trực tiếp nếu dùng ContentResolver)
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects; // <<< Thêm import
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioFilesFragment extends Fragment implements AudioFileListAdapter.OnAudioFileActionListener {

    private static final String TAG = "AudioFilesFragment_Fix"; // Tag mới cho Log

    private ListView audioFilesListView;
    private TextView tvNoAudioFiles;
    private AudioFileListAdapter adapter;
    private List<AudioFile> audioFileList;

    private AudioDAO audioDAO;
    private SessionManager sessionManager;
    private User currentUser;

    private MediaPlayer mediaPlayer;
    private AudioFile currentlyPlayingAudioFile = null;
    private ImageButton lastClickedPlayButton = null;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private AudioFile fileToDownloadAfterPermission = null; // Sẽ lưu trữ AudioFile thay vì chỉ đường dẫn

    private SwipeRefreshLayout swipeRefreshLayout;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();


    public AudioFilesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        audioDAO = new AudioDAO(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentUser = sessionManager.getLoggedInUser();

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission granted after request.");
                if (fileToDownloadAfterPermission != null && !TextUtils.isEmpty(fileToDownloadAfterPermission.getLocalPath())) {
                    // Gọi downloadAudioToPublicDirectory với đối tượng AudioFile
                    downloadAudioToPublicDirectory(fileToDownloadAfterPermission);
                    // fileToDownloadAfterPermission = null; // Có thể xóa ở đây hoặc sau khi download hoàn tất
                } else {
                    Log.w(TAG, "fileToDownloadAfterPermission is null or path is empty after permission grant.");
                    showToast("Không tìm thấy thông tin file để tải xuống sau khi cấp quyền.");
                }
            } else {
                Log.w(TAG, "WRITE_EXTERNAL_STORAGE permission denied after request.");
                showToast("Cần quyền ghi vào bộ nhớ để tải file.");
                fileToDownloadAfterPermission = null; // Xóa nếu quyền bị từ chối
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Đảm bảo tên layout của bạn là fragment_audio_files.xml
        // hoặc đổi R.layout.fragment_audio_files thành R.layout.fragment_your_audio_files
        View view = inflater.inflate(R.layout.fragment_your_audio_files, container, false); // <<< SỬA TÊN LAYOUT NẾU CẦN
        Log.d(TAG, "onCreateView");

        audioFilesListView = view.findViewById(R.id.audioFilesListView);
        tvNoAudioFiles = view.findViewById(R.id.tvNoAudioFiles);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayoutAudioFiles);

        audioFileList = new ArrayList<>();
        adapter = new AudioFileListAdapter(requireContext(), audioFileList, this);
        audioFilesListView.setAdapter(adapter);

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadAudioFilesWithAutoDelete);
            swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light);
        } else {
            Log.e(TAG, "SwipeRefreshLayout not found in layout! Check ID: R.id.swipeRefreshLayoutAudioFiles");
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
        if (currentUser == null) { // Lấy lại nếu onCreate chưa kịp hoặc fragment được tạo lại
            if (sessionManager != null) currentUser = sessionManager.getLoggedInUser();
        }
        if (currentUser == null) {
            handleUserNotLoggedIn();
            return;
        }
        loadAudioFilesWithAutoDelete();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Reloading data if needed.");
        if (currentUser != null) {
            loadAudioFilesWithAutoDelete();
        } else {
            handleUserNotLoggedIn();
        }
    }

    private void loadAudioFilesWithAutoDelete() {
        if (currentUser == null || audioDAO == null) {
            Log.e(TAG, "Cannot load/delete audio files: currentUser or audioDAO is null.");
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            updateUIForNoFiles("Lỗi tải dữ liệu người dùng.");
            return;
        }
        Log.d(TAG, "loadAudioFilesWithAutoDelete: Starting for user ID: " + currentUser.getId());
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        executorService.execute(() -> {
            int deletedCount = deleteOldAudioFiles();
            Log.d(TAG, "loadAudioFilesWithAutoDelete: " + deletedCount + " old audio files deleted.");

            final List<AudioFile> files = audioDAO.getAudioFilesByUserId(currentUser.getId());

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    audioFileList.clear();
                    if (files != null && !files.isEmpty()) {
                        audioFileList.addAll(files);
                        updateUIForFilesExist();
                        Log.d(TAG, "loadAudioFilesWithAutoDelete: Loaded " + files.size() + " audio files.");
                    } else {
                        updateUIForNoFiles("Chưa có file audio nào được tạo.");
                        Log.d(TAG, "loadAudioFilesWithAutoDelete: No audio files found.");
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }

    private int deleteOldAudioFiles() {
        if (currentUser == null || audioDAO == null || getContext() == null) {
            Log.e(TAG, "deleteOldAudioFiles: Pre-condition failed (currentUser, audioDAO, or context is null).");
            return 0;
        }

        List<AudioFile> allUserFiles = audioDAO.getAudioFilesByUserId(currentUser.getId());
        if (allUserFiles == null || allUserFiles.isEmpty()) {
            Log.d(TAG, "deleteOldAudioFiles: No files for user " + currentUser.getId());
            return 0;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -30); // Xóa file cũ hơn 30 ngày
        long thirtyDaysAgoTimestamp = calendar.getTimeInMillis();
        Log.d(TAG, "deleteOldAudioFiles: Deleting files older than: " + new Date(thirtyDaysAgoTimestamp));

        int deletedCount = 0;
        List<AudioFile> filesToCheck = new ArrayList<>(allUserFiles);

        for (AudioFile fileToDelete : filesToCheck) {
            if (fileToDelete.getCreatedAtTimestamp() < thirtyDaysAgoTimestamp) {
                Log.d(TAG, "deleteOldAudioFiles: Attempting to delete old file ID: " + fileToDelete.getId() + ", Path: " + fileToDelete.getLocalPath());
                boolean physicalFileDeleted = false;
                if (!TextUtils.isEmpty(fileToDelete.getLocalPath())) {
                    File physicalFile = new File(fileToDelete.getLocalPath());
                    if (physicalFile.exists()) {
                        if (physicalFile.delete()) {
                            physicalFileDeleted = true;
                            Log.i(TAG, "deleteOldAudioFiles: Physically deleted: " + fileToDelete.getLocalPath());
                        } else {
                            Log.e(TAG, "deleteOldAudioFiles: FAILED to physically delete: " + fileToDelete.getLocalPath());
                        }
                    } else {
                        Log.w(TAG, "deleteOldAudioFiles: Physical file not found (already deleted?): " + fileToDelete.getLocalPath());
                        physicalFileDeleted = true; // Coi như đã xóa để dọn DB
                    }
                } else {
                    Log.w(TAG, "deleteOldAudioFiles: Local path empty for file ID: " + fileToDelete.getId());
                    physicalFileDeleted = true; // Không có file vật lý, cho phép xóa DB
                }

                if (physicalFileDeleted) {
                    if (audioDAO.deleteAudioFile(fileToDelete.getId())) {
                        deletedCount++;
                        Log.i(TAG, "deleteOldAudioFiles: Deleted metadata from DB for file ID: " + fileToDelete.getId());
                    } else {
                        Log.e(TAG, "deleteOldAudioFiles: FAILED to delete metadata from DB for file ID: " + fileToDelete.getId());
                    }
                }
            }
        }
        return deletedCount;
    }

    private void updateUIForNoFiles(String message) {
        if (tvNoAudioFiles != null) {
            tvNoAudioFiles.setText(message);
            tvNoAudioFiles.setVisibility(View.VISIBLE);
        }
        if (audioFilesListView != null) audioFilesListView.setVisibility(View.GONE);
    }

    private void updateUIForFilesExist() {
        if (tvNoAudioFiles != null) tvNoAudioFiles.setVisibility(View.GONE);
        if (audioFilesListView != null) audioFilesListView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPlayAudioClicked(AudioFile audioFile, ImageButton playButton) {
        if (audioFile == null || TextUtils.isEmpty(audioFile.getLocalPath())) {
            showToast("File audio không hợp lệ."); return;
        }
        File fileToPlay = new File(audioFile.getLocalPath());
        if (!fileToPlay.exists() || !fileToPlay.isFile()) {
            showToast("File audio không tồn tại.");
            Log.w(TAG, "File not found: " + audioFile.getLocalPath());
            // Cân nhắc xóa bản ghi DB nếu file không còn
            // audioDAO.deleteAudioFile(audioFile.getId());
            // loadAudioFilesWithAutoDelete(); // Làm mới danh sách
            return;
        }
        Log.d(TAG, "Play clicked for: " + audioFile.getFileName());
        togglePlayPause(audioFile, playButton);
    }

    @Override
    public void onDownloadAudioClicked(AudioFile audioFile) {
        if (audioFile == null || TextUtils.isEmpty(audioFile.getLocalPath()) || TextUtils.isEmpty(audioFile.getDisplayNameForDownload())) {
            showToast("Thông tin file không hợp lệ để tải."); return;
        }
        File localFile = new File(audioFile.getLocalPath());
        if (!localFile.exists() || !localFile.isFile()) {
            showToast("File nguồn không tồn tại để tải về."); return;
        }
        Log.d(TAG, "Download clicked for: " + audioFile.getFileName());

        fileToDownloadAfterPermission = audioFile; // Lưu toàn bộ đối tượng AudioFile
        requestStoragePermissionAndDownload(); // Gọi phương thức không tham số
    }

    private void togglePlayPause(AudioFile audioFile, ImageButton playButton) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            if (currentlyPlayingAudioFile != null && currentlyPlayingAudioFile.getId() == audioFile.getId()) {
                // Đang phát chính file này, tạm dừng
                stopAudio();
            } else {
                // Đang phát file khác, dừng file hiện tại và phát file mới
                stopAudio();
                playAudio(audioFile, playButton);
            }
        } else {
            // Không có audio nào đang phát hoặc đang tạm dừng, phát file này
            playAudio(audioFile, playButton);
        }
    }

    private void playAudio(AudioFile audioFileToPlay, ImageButton playButton) {
        if (audioFileToPlay == null || TextUtils.isEmpty(audioFileToPlay.getLocalPath()) || getContext() == null) {
            showToast("File audio không hợp lệ."); return;
        }
        File file = new File(audioFileToPlay.getLocalPath());
        if (!file.exists() || !file.isFile()){ showToast("File không tồn tại."); return; }

        stopAudio(); // Dừng bất kỳ audio nào đang phát

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "MediaPlayer playback completed: " + audioFileToPlay.getFileName());
            stopAudio(); // Reset nút và trạng thái
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer Error: what=" + what + ", extra=" + extra);
            showToast("Lỗi phát audio.");
            stopAudio(); // Reset khi có lỗi
            return true; // true nếu lỗi đã được xử lý
        });

        try {
            Log.d(TAG, "MediaPlayer attempting to play: " + file.getAbsolutePath());
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepareAsync(); // Chuẩn bị bất đồng bộ
            mediaPlayer.setOnPreparedListener(mp -> {
                try {
                    mp.start();
                    currentlyPlayingAudioFile = audioFileToPlay;
                    lastClickedPlayButton = playButton;
                    if (lastClickedPlayButton != null) lastClickedPlayButton.setImageResource(R.drawable.ic_pause); // Cập nhật icon pause
                    Log.d(TAG, "MediaPlayer started playing: " + audioFileToPlay.getFileName());
                } catch (IllegalStateException e) {
                    Log.e(TAG, "MediaPlayer start failed after prepare", e);
                    stopAudio(); // Dọn dẹp khi thất bại
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer setDataSource failed", e);
            showToast("Không thể đọc file audio.");
            stopAudio();
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer state error during setup", e);
            showToast("Lỗi trình phát media.");
            stopAudio();
        }
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset(); // Reset để tái sử dụng hoặc trước khi release
                mediaPlayer.release(); // Giải phóng tài nguyên
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error stopping/releasing MediaPlayer", e);
            }
            mediaPlayer = null;
        }
        currentlyPlayingAudioFile = null;
        if (lastClickedPlayButton != null) {
            lastClickedPlayButton.setImageResource(R.drawable.ic_play_arrow); // Reset về icon play
            lastClickedPlayButton = null;
        }
        Log.d(TAG, "MediaPlayer stopped and released.");
    }


    // Sửa đổi để không nhận tham số, sử dụng fileToDownloadAfterPermission
    private void requestStoragePermissionAndDownload() {
        if (fileToDownloadAfterPermission == null || TextUtils.isEmpty(fileToDownloadAfterPermission.getLocalPath())) {
            showToast("Lỗi: Không có file nào được chọn để tải.");
            Log.e(TAG, "requestStoragePermissionAndDownload: fileToDownloadAfterPermission is null or path is empty");
            return;
        }

        // Không cần lấy sourceFile và displayName ở đây nữa, vì chúng sẽ được lấy từ
        // fileToDownloadAfterPermission bên trong downloadAudioToPublicDirectory(AudioFile)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Không cần quyền cụ thể cho MediaStore trên Q+ đối với các tệp của ứng dụng hoặc thư mục public
            Log.d(TAG, "Android Q+, proceeding with download.");
            downloadAudioToPublicDirectory(fileToDownloadAfterPermission);
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission already granted.");
                downloadAudioToPublicDirectory(fileToDownloadAfterPermission);
            } else {
                Log.d(TAG, "Requesting WRITE_EXTERNAL_STORAGE permission.");
                // Launcher sẽ gọi downloadAudioToPublicDirectory nếu quyền được cấp,
                // sử dụng biến 'fileToDownloadAfterPermission'.
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }


    // Overload này được giữ lại nếu bạn cần gọi trực tiếp bằng đối tượng AudioFile
    // (ví dụ: sau khi quyền được cấp bởi launcher)
    private void downloadAudioToPublicDirectory(AudioFile audioFileToDownload) {
        if (audioFileToDownload == null || TextUtils.isEmpty(audioFileToDownload.getLocalPath())) {
            showToast("Lỗi thông tin file để tải.");
            Log.e(TAG, "audioFileToDownload is null or localPath is empty.");
            return;
        }
        File sourceFile = new File(audioFileToDownload.getLocalPath());
        String displayName = audioFileToDownload.getDisplayNameForDownload();

        if (TextUtils.isEmpty(displayName)) {
            displayName = sourceFile.getName(); // Tên dự phòng
            Log.w(TAG, "Display name for download was empty, using source file name: " + displayName);
        }
        // Gọi phương thức download chính với File và displayName
        downloadAudioToPublicDirectory(sourceFile, displayName);
    }


    // <<< PHƯƠNG THỨC ĐÃ SỬA ĐỔI ĐỂ TẢI XUỐNG TỪNG PHẦN >>>
    private void downloadAudioToPublicDirectory(File sourceFile, String displayNameForDownload) {
        if (getContext() == null || getActivity() == null) { // Kiểm tra cả getActivity
            Log.e(TAG, "Context or Activity is null, cannot proceed with download.");
            // Không hiển thị Toast ở đây vì có thể gây crash nếu Context thực sự null
            return;
        }
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            showToast("File nguồn không tồn tại.");
            Log.e(TAG, "Source file does not exist or is not a file: " + sourceFile.getAbsolutePath());
            return;
        }
        if (TextUtils.isEmpty(displayNameForDownload)) {
            displayNameForDownload = sourceFile.getName();
            Log.w(TAG, "displayNameForDownload was empty, using source file name: " + displayNameForDownload);
        }

        final String finalDisplayName = displayNameForDownload;

        // Hiển thị Toast ban đầu trên UI thread
        getActivity().runOnUiThread(() -> showToast("Bắt đầu tải xuống: " + finalDisplayName));

        executorService.execute(() -> {
            ContentResolver resolver = getContext().getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, finalDisplayName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg"); // Hoặc xác định động nếu có thể (ví dụ: audio/wav, audio/ogg)
            // Bạn có thể muốn lấy MIME type từ tên file hoặc một trường trong AudioFile model.
            // Ví dụ: contentValues.put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(sourceFile.getName()));

            Uri externalContentUri;
            String relativePath = Environment.DIRECTORY_MUSIC + File.separator + "AutoVoiceApp"; // Tên thư mục ứng dụng của bạn

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1); // Đánh dấu là đang chờ xử lý
                externalContentUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            } else {
                File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                File appDir = new File(publicDir, "AutoVoiceApp"); // Nhất quán tên thư mục
                if (!appDir.exists() && !appDir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory: " + appDir.getAbsolutePath());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> showToast("Không thể tạo thư mục tải xuống."));
                    }
                    return;
                }
                File destFile = new File(appDir, finalDisplayName);
                contentValues.put(MediaStore.MediaColumns.DATA, destFile.getAbsolutePath());
                externalContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }

            Uri fileUri = null;
            OutputStream outputStream = null;
            InputStream inputStream = null;
            long fileLength = sourceFile.length();
            long totalBytesCopied = 0;
            int lastShownProgress = 0; // Để tránh hiển thị Toast quá nhiều

            try {
                fileUri = resolver.insert(externalContentUri, contentValues);
                if (fileUri == null) {
                    throw new IOException("Failed to create new MediaStore record for " + finalDisplayName);
                }

                outputStream = resolver.openOutputStream(Objects.requireNonNull(fileUri));
                if (outputStream == null) {
                    throw new IOException("Failed to get output stream for " + fileUri);
                }

                inputStream = new FileInputStream(sourceFile);

                byte[] buffer = new byte[8192]; // Bộ đệm 8KB
                int bytesRead;
                Log.d(TAG, "Starting chunked copy for: " + finalDisplayName + ", Total size: " + fileLength + " bytes");

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesCopied += bytesRead;

                    // --- Cập nhật tiến trình (ví dụ mỗi 10%) ---
                    if (fileLength > 0) { // Tránh chia cho 0
                        int currentProgress = (int) ((totalBytesCopied * 100) / fileLength);
                        if (currentProgress >= lastShownProgress + 10 || currentProgress == 100) { // Cập nhật mỗi 10% hoặc khi hoàn thành
                            lastShownProgress = currentProgress;
                            final int finalProgress = currentProgress; // Biến final để dùng trong lambda
                            Log.d(TAG, "Download progress for " + finalDisplayName + ": " + finalProgress + "%");
                            // Tùy chọn: Hiển thị Toast tiến trình (có thể hơi nhiều)
                            // if (getActivity() != null) {
                            //     getActivity().runOnUiThread(() -> showToast("Đang tải " + finalDisplayName + ": " + finalProgress + "%"));
                            // }
                        }
                    }
                }
                outputStream.flush(); // Đảm bảo tất cả dữ liệu đã được ghi
                Log.i(TAG, "Successfully copied " + totalBytesCopied + " bytes for " + finalDisplayName + " to: " + fileUri.toString());

                // --- Hoàn thành tải xuống ---
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear();
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0); // Đánh dấu hoàn thành
                    resolver.update(fileUri, contentValues, null, null);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast(finalDisplayName + " đã được tải xuống thành công!");
                        // Tùy chọn: Quét media cho các phiên bản Android cũ hơn nếu không sử dụng MediaStore.Audio.Media.EXTERNAL_CONTENT_URI để insert
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            File finalFile = new File(contentValues.getAsString(MediaStore.MediaColumns.DATA));
                            if (finalFile.exists()){
                                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                mediaScanIntent.setData(Uri.fromFile(finalFile)); // Uri từ tệp
                                getContext().sendBroadcast(mediaScanIntent);
                                Log.d(TAG, "Media scan initiated for: " + finalFile.getAbsolutePath());
                            }
                        }
                    });
                }

            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found during download: " + sourceFile.getAbsolutePath(), e);
                if (getActivity() != null) getActivity().runOnUiThread(() -> showToast("Lỗi: File nguồn không tìm thấy."));
                if (fileUri != null) resolver.delete(fileUri, null, null); // Dọn dẹp MediaStore entry nếu đã tạo
            } catch (IOException e) {
                Log.e(TAG, "IOException during file copy for: " + finalDisplayName, e);
                if (getActivity() != null) getActivity().runOnUiThread(() -> showToast("Lỗi I/O khi tải file."));
                if (fileUri != null) resolver.delete(fileUri, null, null); // Dọn dẹp
            } catch (NullPointerException e) { // Bắt thêm NullPointerException
                Log.e(TAG, "NullPointerException during download for: " + finalDisplayName, e);
                if (getActivity() != null) getActivity().runOnUiThread(() -> showToast("Lỗi không xác định khi tải file."));
                if (fileUri != null) resolver.delete(fileUri, null, null); // Dọn dẹp
            }
            finally {
                try {
                    if (inputStream != null) inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing input stream for " + finalDisplayName, e);
                }
                try {
                    if (outputStream != null) outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream for " + finalDisplayName, e);
                }
                // Xóa fileToDownloadAfterPermission sau khi quá trình tải (thành công hoặc thất bại) đã xử lý
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> fileToDownloadAfterPermission = null);
                }
            }
        });
    }


    private void showToast(String message) {
        // Kiểm tra isAdded() để đảm bảo fragment vẫn được gắn vào activity
        if (getContext() != null && isAdded()) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "showToast called but getContext() is null or fragment not added. Message: " + message);
        }
    }

    private void handleUserNotLoggedIn() {
        Log.w(TAG, "User not logged in or currentUser is null.");
        if (isAdded() && getActivity() instanceof MainUserActivity) {
            // Chỉ hiển thị Toast và điều hướng nếu fragment được gắn và activity là MainUserActivity
            Toast.makeText(getContext(), "Vui lòng đăng nhập để xem file.", Toast.LENGTH_LONG).show();
            ((MainUserActivity) getActivity()).navigateToLoginScreen();
        } else if (isAdded() && getContext() != null) {
            // Nếu không phải MainUserActivity nhưng fragment được gắn, chỉ hiển thị Toast
            Toast.makeText(getContext(), "Lỗi người dùng. Không thể tải dữ liệu.", Toast.LENGTH_LONG).show();
        }
        // Nếu không isAdded(), không làm gì cả để tránh crash
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called. Stopping audio.");
        stopAudio();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called. Stopping audio.");
        stopAudio(); // Dọn dẹp tài nguyên MediaPlayer
        // Giải phóng tham chiếu đến view để tránh rò rỉ bộ nhớ
        audioFilesListView = null;
        tvNoAudioFiles = null;
        swipeRefreshLayout = null;
        adapter = null; // Mặc dù adapter có thể không giữ context trực tiếp, nhưng đây là thói quen tốt
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called.");
        if (audioDAO != null) {
            audioDAO.close(); // Đóng DAO
            audioDAO = null;
        }
        if (mediaPlayer != null) { // MediaPlayer nên được release ở stopAudio() hoặc onDestroyView()
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (executorService != null && !executorService.isShutdown()) {
            Log.d(TAG, "Shutting down executor service.");
            executorService.shutdown(); // Cân nhắc dùng shutdownNow() nếu cần dừng ngay lập tức
        }
        requestPermissionLauncher = null; // Hủy đăng ký launcher
    }
}