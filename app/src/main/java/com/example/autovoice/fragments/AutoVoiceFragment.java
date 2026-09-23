package com.example.autovoice.fragments;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.autovoice.R;
import com.example.autovoice.databinding.FragmentAutoVoiceBinding; // Sử dụng ViewBinding
import com.example.autovoice.models.AudioFile; // <<< IMPORT MODEL AUDIOFILE

import com.example.autovoice.models.User;
import com.example.autovoice.utils.SessionManager;
import com.example.autovoice.database.AudioDAO; // <<< IMPORT AUDIO DAO (ĐÃ ĐỔI TÊN)

// Imports cho Google Cloud TextToSpeech
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.ListVoicesRequest;
import com.google.cloud.texttospeech.v1.ListVoicesResponse;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1.Voice;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AutoVoiceFragment extends Fragment {

    private static final String TAG = "AutoVoiceFragment_GCT";
    private FragmentAutoVoiceBinding binding;
    private SessionManager sessionManager;
    private User currentUser;
    private TextToSpeechClient ttsClient;
    private AudioDAO audioDAO; // <<< THÊM BIẾN DAO (ĐÃ ĐỔI TÊN)

    private File lastGeneratedAudioFile = null;

    private String currentTextToSpeakForFile = "";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Dữ liệu mẫu cho Spinners - BẠN NÊN LẤY ĐỘNG TỪ API
    private List<String> availableLanguages = new ArrayList<>();
    private List<Voice> allVoicesForSelectedLanguage = new ArrayList<>();
    private List<String> availableVoiceNames = new ArrayList<>();
    // Các loại giọng phổ biến, có thể thay đổi tùy theo API
    private final String[] voiceTypes = {"WaveNet", "Standard", "Neural2"}; // Neural2 có thể không có ở mọi ngôn ngữ

    public AutoVoiceFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        currentUser = sessionManager.getLoggedInUser();

        initializeTtsClient(); // Khởi tạo Google Cloud TTS Client
        audioDAO = new AudioDAO(requireContext()); // <<< KHỞI TẠO AUDIO DAO

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission granted.");
                if (lastGeneratedAudioFile != null && !currentTextToSpeakForFile.isEmpty()) {
                    downloadAudioToPublicDirectory(lastGeneratedAudioFile, currentTextToSpeakForFile);
                } else {
                    showToast("Chưa có file audio để tải hoặc text rỗng.");
                }
            } else {
                Log.w(TAG, "WRITE_EXTERNAL_STORAGE permission denied.");
                showToast("Cần quyền ghi vào bộ nhớ để tải file.");
            }
        });
    }

    private void initializeTtsClient() {
        // CẢNH BÁO BẢO MẬT: KHÔNG BAO GIỜ HARDCODE CREDENTIALS TRONG CODE PRODUCTION.
        // Sử dụng Service Account JSON file từ res/raw cho ví dụ này.
        // Trong production, backend nên xử lý việc gọi API.
        executorService.execute(() -> {
            try {
                // Thay thế 'your_service_account_credentials' bằng tên file JSON của bạn trong res/raw
                InputStream credentialsStream = getResources().openRawResource(R.raw.your_service_account_credentials);
                GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
                TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                        .build();
                ttsClient = TextToSpeechClient.create(settings);
                Log.i(TAG, "Google Cloud TextToSpeechClient initialized successfully.");
                // Sau khi client khởi tạo thành công, tải danh sách ngôn ngữ
                fetchAvailableLanguages();
            } catch (IOException e) {
                Log.e(TAG, "Failed to initialize Google Cloud TextToSpeechClient", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showToast("Lỗi khởi tạo dịch vụ TTS Google. Kiểm tra credentials.");
                        if (binding != null) binding.btnGenerateAudio.setEnabled(false);
                    });
                }
            }
        });
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAutoVoiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (currentUser == null) {
            showToast("Lỗi: Không tìm thấy thông tin người dùng.");
            binding.btnGenerateAudio.setEnabled(false);
            binding.btnDownloadAudio.setEnabled(false);
            return;
        }

        updateCharLimitUI();
        setupSpinners();
        setupListeners();
    }

    private void updateCharLimitUI() {
        if (binding == null || currentUser == null) return;
        binding.tvCharLimitInfo.setText("Limit: " + getCharacterLimit() + " Character ");
    }

    private void updateCharCount(String text) {
        if (binding == null) return;
        int currentLength = text.length();
        int limit = getCharacterLimit();
        binding.tvCharLimitInfo.setText(currentLength + "/" + limit + " ký tự");
        if (currentLength > limit) {
            binding.tvCharLimitInfo.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            binding.btnGenerateAudio.setEnabled(false);
        } else {
            binding.tvCharLimitInfo.setTextColor(ContextCompat.getColor(requireContext(), R.color.design_default_color_on_surface));
            binding.btnGenerateAudio.setEnabled(true);
        }
    }

    private int getCharacterLimit() {
        if (currentUser != null && "Premium".equalsIgnoreCase(currentUser.getSubscriptionStatus())) {
            return 3000;
        }
        return 500; // Mặc định
    }

    private void setupSpinners() {
        if (getContext() == null || binding == null) return;

        // Spinner Loại giọng (Voice Type) - dữ liệu tĩnh ví dụ
        ArrayAdapter<String> voiceTypeAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, voiceTypes);
        voiceTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerVoiceType.setAdapter(voiceTypeAdapter);

        // Listener cho Spinner Ngôn ngữ (sẽ cập nhật Spinner Tên giọng)
        binding.spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguageCode = availableLanguages.get(position).split(" ")[0]; // Lấy mã ngôn ngữ, ví dụ "vi-VN"
                fetchVoicesForLanguage(selectedLanguageCode);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchAvailableLanguages() {
        if (ttsClient == null) return;
        executorService.execute(() -> {
            try {
                ListVoicesRequest request = ListVoicesRequest.newBuilder().build();
                ListVoicesResponse response = ttsClient.listVoices(request);
                List<String> uniqueLanguageCodesWithNames = new ArrayList<>();
                // Lọc và chỉ lấy các ngôn ngữ duy nhất, có thể kèm tên hiển thị
                // Ví dụ: "vi-VN (Vietnamese)", "en-US (English, United States)"
                // Đây là phần phức tạp, cần xử lý response.getVoicesList()
                // Tạm thời dùng danh sách mẫu:
                availableLanguages.clear();
                availableLanguages.add("vi-VN (Tiếng Việt)");
                availableLanguages.add("en-US (English, US)");
                availableLanguages.add("en-GB (English, UK)");
                // Thêm các ngôn ngữ khác bạn muốn hỗ trợ

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (getContext() != null && binding != null) {
                            ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(getContext(),
                                    android.R.layout.simple_spinner_item, availableLanguages);
                            languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            binding.spinnerLanguage.setAdapter(languageAdapter);
                            // Tự động chọn ngôn ngữ đầu tiên và tải giọng nói
                            if (!availableLanguages.isEmpty()) {
                                binding.spinnerLanguage.setSelection(0);
                                String defaultLangCode = availableLanguages.get(0).split(" ")[0];
                                fetchVoicesForLanguage(defaultLangCode);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching available languages", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> showToast("Lỗi tải danh sách ngôn ngữ."));
                }
            }
        });
    }

    private void fetchVoicesForLanguage(String languageCode) {
        if (ttsClient == null || TextUtils.isEmpty(languageCode)) return;
        Log.d(TAG, "Fetching voices for language: " + languageCode);
        executorService.execute(() -> {

            try {
                ListVoicesRequest request = ListVoicesRequest.newBuilder().setLanguageCode(languageCode).build();
                ListVoicesResponse response = ttsClient.listVoices(request);
                allVoicesForSelectedLanguage = response.getVoicesList();

                // Lọc tên giọng dựa trên loại giọng đã chọn (nếu có)
                // String selectedVoiceType = binding.spinnerVoiceType.getSelectedItem().toString();
                availableVoiceNames = allVoicesForSelectedLanguage.stream()
                        // .filter(v -> v.getName().contains(selectedVoiceType)) // Lọc theo loại giọng nếu cần
                        .map(Voice::getName) // Chỉ lấy tên giọng
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (getContext() != null && binding != null) {
                            ArrayAdapter<String> voiceNameAdapter = new ArrayAdapter<>(getContext(),
                                    android.R.layout.simple_spinner_item, availableVoiceNames);
                            voiceNameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            binding.spinnerVoiceName.setAdapter(voiceNameAdapter);
                            if (!availableVoiceNames.isEmpty()) {
                                binding.spinnerVoiceName.setSelection(0); // Chọn giọng đầu tiên
                            } else {
                                showToast("Không có giọng đọc nào cho ngôn ngữ " + languageCode);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching voices for language: " + languageCode, e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> showToast("Lỗi tải danh sách giọng đọc."));
                }
            }
        });
    }


    private void setupListeners() {
        if (binding == null) return;
        binding.edtTextToSpeak.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCharCount(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnGenerateAudio.setOnClickListener(v -> {
            String textToSpeak = binding.edtTextToSpeak.getText().toString().trim();
            handleGenerateAudio(textToSpeak);
        });

        binding.btnDownloadAudio.setOnClickListener(v -> {
            if (lastGeneratedAudioFile != null && lastGeneratedAudioFile.exists() && !currentTextToSpeakForFile.isEmpty()) {
                requestStoragePermissionAndDownload();
            } else {
                showToast("Chưa có file audio để tải.");
            }
        });

        binding.seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Chuyển đổi progress (0-200) thành tốc độ (0.25 - 4.0)
                // Ví dụ: 100 là 1.0x. progress / 100.0f * 2.0f (nếu max là 200, thì 200/100 * 2 = 4x, 0/100*2 = 0, không ổn)
                // Cần ánh xạ lại: progress 0 -> 0.25x, 100 -> 1.0x, 200 -> 4.0x
                // (progress - 100) / 50.0f + 1.0f  (nếu 0-200, 0->-1, 100->1, 200->3)
                // (progress * (4.0f - 0.25f) / 200.0f) + 0.25f
                float speed = 0.25f + (progress / (float) seekBar.getMax()) * (4.0f - 0.25f);
                binding.tvSpeedValue.setText(String.format(Locale.US, "%.2fx", speed));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void handleGenerateAudio(String text) {
        Log.d(TAG, "handleGenerateAudio: START - Text length: " + text.length());
        if (ttsClient == null) { /* ... */ return; }
        if (TextUtils.isEmpty(text)) { /* ... */ return; }
        if (text.length() > getCharacterLimit()) { /* ... */ return; }

        final String selectedLanguageCode = binding.spinnerLanguage.getSelectedItem() != null ? binding.spinnerLanguage.getSelectedItem().toString().split(" ")[0] : "vi-VN";
        final String selectedVoiceName = binding.spinnerVoiceName.getSelectedItem() != null ? binding.spinnerVoiceName.getSelectedItem().toString() : "";
        final float progress = binding.seekBarSpeed.getProgress();
        final float speakingRate;
        if (progress <= 100) { speakingRate = 0.25f + (progress / 100.0f) * (1.0f - 0.25f); }
        else { speakingRate = 1.0f + ((progress - 100) / 100.0f) * (4.0f - 1.0f); }

        Log.d(TAG, "handleGenerateAudio: Params - Lang=" + selectedLanguageCode + ", Voice=" + selectedVoiceName + ", Rate=" + speakingRate);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnGenerateAudio.setEnabled(false);
        binding.btnDownloadAudio.setEnabled(false);
        //binding.btnPlayAudio.setEnabled(false);
        //binding.btnPlayAudio.setImageResource(R.drawable.ic_play_arrow);
        lastGeneratedAudioFile = null;
        currentTextToSpeakForFile = text;

        executorService.execute(() -> {
            Log.d(TAG, "handleGenerateAudio: Background task started.");
            File audioFileGeneratedByTTS = null; // <<< KHAI BÁO Ở PHẠM VI RỘNG HƠN
            try {
                SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
                VoiceSelectionParams.Builder voiceBuilder = VoiceSelectionParams.newBuilder().setLanguageCode(selectedLanguageCode);
                if (!TextUtils.isEmpty(selectedVoiceName)) {
                    voiceBuilder.setName(selectedVoiceName);
                }
                AudioConfig audioConfig = AudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.MP3)
                        .setSpeakingRate(speakingRate)
                        .build();

                SynthesizeSpeechResponse response = ttsClient.synthesizeSpeech(input, voiceBuilder.build(), audioConfig);
                ByteString audioContents = response.getAudioContent();
                Log.i(TAG, "handleGenerateAudio: Received audio content. Size: " + audioContents.size() + " bytes");

                if (audioContents.isEmpty()) { /* ... xử lý lỗi ... */ return; }

                // Gán kết quả của saveAudioToAppInternalStorage cho biến đã khai báo
                audioFileGeneratedByTTS = saveAudioToAppInternalStorage(audioContents, text);

            } catch (Exception e) {
                Log.e(TAG, "handleGenerateAudio: Google Cloud TTS API Error or other exception", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> { /* ... xử lý UI lỗi ... */ });
                }
                return;
            }

            // Sử dụng biến final để truyền vào runOnUiThread
            final File finalAudioFileFromTTS = audioFileGeneratedByTTS;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnGenerateAudio.setEnabled(true);

                    // --- SỬA LỖI Ở ĐÂY: SỬ DỤNG finalAudioFileFromTTS ---
                    if (finalAudioFileFromTTS != null && finalAudioFileFromTTS.exists() && finalAudioFileFromTTS.length() > 0) {
                        lastGeneratedAudioFile = finalAudioFileFromTTS;
                        binding.btnDownloadAudio.setEnabled(true);
                        //binding.btnPlayAudio.setEnabled(true);
                        showToast("Audio đã được tạo: " + finalAudioFileFromTTS.getName());

                        if (currentUser != null && audioDAO != null) {
                            String textSnippet = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                            String voiceDetails = "Lang: " + selectedLanguageCode +
                                    (TextUtils.isEmpty(selectedVoiceName) ? "" : ", Voice: " + selectedVoiceName) +
                                    ", Rate: " + String.format(Locale.US, "%.2f", speakingRate) + "x";
                            String displayNameForDownload = "AutoVoice_" +
                                    (text.length() > 20 ? text.substring(0, 20) : text)
                                            .replaceAll("[^a-zA-Z0-9]", "_").toLowerCase() +
                                    "_" + System.currentTimeMillis() + ".mp3";

                            AudioFile newAudioMetadata = new AudioFile();
                            newAudioMetadata.setUserId(currentUser.getId());
                            // Sử dụng thông tin từ finalAudioFileFromTTS (file thực tế đã được tạo)
                            newAudioMetadata.setFileName(finalAudioFileFromTTS.getName());
                            newAudioMetadata.setLocalPath(finalAudioFileFromTTS.getAbsolutePath());
                            newAudioMetadata.setOriginalTextSnippet(textSnippet);
                            newAudioMetadata.setCreatedAtTimestamp(System.currentTimeMillis());
                            newAudioMetadata.setDisplayNameForDownload(displayNameForDownload);
                            newAudioMetadata.setVoiceDetails(voiceDetails);

                            long audioDbId = audioDAO.addAudioFile(newAudioMetadata);
                            if (audioDbId > 0) {
                                Log.i(TAG, "Audio metadata saved to DB with ID: " + audioDbId + " for User ID: " + currentUser.getId());
                            } else {
                                Log.e(TAG, "Failed to save audio metadata to DB. Error code: " + audioDbId + " for User ID: " + currentUser.getId());
                                showToast("Lỗi lưu thông tin audio vào CSDL.");
                            }
                        } else {
                            Log.e(TAG, "Cannot save audio metadata: currentUser or audioDAO is null.");
                        }
                    } else {
                        Log.e(TAG, "handleGenerateAudio: Failed to save audio file or file is invalid after saving.");
                        showToast("Lỗi khi lưu file audio tạm thời.");
                       // binding.btnPlayAudio.setEnabled(false);
                    }
                });
            }
        });
    }
    private File saveAudioToAppInternalStorage(ByteString audioContents, String originalText) {
        // ... (Giữ nguyên hàm saveAudioToAppInternalStorage như đã có)
        if (getContext() == null || audioContents == null || audioContents.isEmpty()) {
            Log.e(TAG, "saveAudioToAppInternalStorage: Invalid parameters.");
            return null;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String safeTextPrefix = originalText.length() > 20 ? originalText.substring(0, 20) : originalText;
        safeTextPrefix = safeTextPrefix.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        String fileName = "audio_" + safeTextPrefix + "_" + timeStamp + ".mp3";

        File audioDir = new File(getContext().getFilesDir(), "generated_audio_gcloud"); // Đổi tên thư mục nếu muốn
        if (!audioDir.exists() && !audioDir.mkdirs()) {
            Log.e(TAG, "Failed to create directory: " + audioDir.getAbsolutePath());
            return null;
        }
        File audioFile = new File(audioDir, fileName);
        try (OutputStream out = new FileOutputStream(audioFile)) {
            out.write(audioContents.toByteArray());
            Log.i(TAG, "Audio content saved to internal: " + audioFile.getAbsolutePath());
            return audioFile;
        } catch (IOException e) {
            Log.e(TAG, "Error writing audio file to internal", e);
            return null;
        }
    }

    private void requestStoragePermissionAndDownload() {
        // ... (Giữ nguyên hàm requestStoragePermissionAndDownload như đã có)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (lastGeneratedAudioFile != null && !currentTextToSpeakForFile.isEmpty()) {
                downloadAudioToPublicDirectory(lastGeneratedAudioFile, currentTextToSpeakForFile);
            } else { showToast("Không có file audio để tải."); }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                if (lastGeneratedAudioFile != null && !currentTextToSpeakForFile.isEmpty()) {
                    downloadAudioToPublicDirectory(lastGeneratedAudioFile, currentTextToSpeakForFile);
                } else { showToast("Không có file audio để tải."); }
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void downloadAudioToPublicDirectory(File sourceFile, String originalText) {
        // ... (Giữ nguyên hàm downloadAudioToPublicDirectory như đã có)
        if (getContext() == null || sourceFile == null || !sourceFile.exists()) {
            Log.e(TAG, "downloadAudio: Invalid parameters or source file does not exist.");
            showToast("Lỗi: File nguồn không tồn tại."); return;
        }
        ContentResolver resolver = getContext().getContentResolver();
        ContentValues contentValues = new ContentValues();
        String safeTextPrefix = originalText.length() > 30 ? originalText.substring(0, 30) : originalText;
        safeTextPrefix = safeTextPrefix.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "_");
        String displayName = "AutoVoice_GCT_" + safeTextPrefix + "_" + System.currentTimeMillis() + ".mp3";

        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
        Uri destinationUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "AutoVoice");
            destinationUri = resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues);
        } else {
            File publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File appDownloadsDir = new File(publicDownloadsDir, "AutoVoice");
            if (!appDownloadsDir.exists() && !appDownloadsDir.mkdirs()) {
                Log.e(TAG, "Failed to create AutoVoice directory in public Downloads.");
                showToast("Không thể tạo thư mục tải về."); return;
            }
            File targetFile = new File(appDownloadsDir, displayName);
            contentValues.put(MediaStore.MediaColumns.DATA, targetFile.getAbsolutePath());
            destinationUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (destinationUri == null) {
                try (InputStream in = new FileInputStream(sourceFile); OutputStream out = new FileOutputStream(targetFile)) {
                    byte[] buf = new byte[1024]; int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(targetFile));
                    getContext().sendBroadcast(mediaScanIntent);
                    showToast("Đã tải về: " + displayName); return;
                } catch (IOException e) {
                    Log.e(TAG, "Error copying file directly for Android < Q", e);
                    showToast("Lỗi khi tải file về."); return;
                }
            }
        }
        if (destinationUri == null) {
            Log.e(TAG, "Failed to create MediaStore entry for download.");
            showToast("Không thể tạo file trong thư mục Downloads."); return;
        }
        try (InputStream in = new FileInputStream(sourceFile); OutputStream out = resolver.openOutputStream(destinationUri)) {
            if (out == null) throw new IOException("Failed to open output stream for MediaStore URI.");
            byte[] buf = new byte[1024]; int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            Log.i(TAG, "File downloaded to: " + displayName);
            showToast("Đã tải về: " + displayName + " trong Downloads/AutoVoice");
        } catch (IOException e) {
            Log.e(TAG, "Error copying file to public Downloads", e);
            showToast("Lỗi khi tải file về: " + e.getMessage());
            resolver.delete(destinationUri, null, null);
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ttsClient != null) {
            try {
                Log.d(TAG, "Shutting down Google Cloud TextToSpeechClient.");
                ttsClient.shutdownNow();
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down TextToSpeechClient", e);
            }
        }
        if (audioDAO != null) { // <<< ĐÓNG AUDIO DAO
            audioDAO.close();
            Log.d(TAG, "AudioDAO closed in onDestroy.");
        }
        executorService.shutdown();
    }
}
