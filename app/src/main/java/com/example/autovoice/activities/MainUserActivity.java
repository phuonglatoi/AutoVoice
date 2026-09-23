package com.example.autovoice.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech; // Nếu vẫn dùng TTS ở đây
import android.util.Log;
import android.widget.Toast;
// ... các import khác cho Fragment, BottomNavigationView ...
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import com.example.autovoice.R;
import com.example.autovoice.databinding.ActivityMainUserBinding; // Giả sử dùng ViewBinding
import com.example.autovoice.fragments.AudioFilesFragment;
import com.example.autovoice.fragments.AutoVoiceFragment; // Ví dụ
import com.example.autovoice.fragments.BillingFragment;
import com.example.autovoice.fragments.PricingFragment;
import com.example.autovoice.fragments.ProfileFragment;
// ... import các fragment khác ...
import com.example.autovoice.presenters.MainUserPresenter; // Presenter cho MainUserActivity
import com.example.autovoice.utils.SessionManager;

import java.util.Locale; // Nếu dùng TTS

// Implement MainUserPresenter.MainUserView
public class MainUserActivity extends AppCompatActivity
        implements TextToSpeech.OnInitListener, MainUserPresenter.MainUserView {

    private ActivityMainUserBinding binding; // ViewBinding cho Activity
    private MainUserPresenter presenter;
    private TextToSpeech textToSpeech; // Nếu logic TTS vẫn ở đây

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d("MainUserActivity", "onCreate");

        presenter = new MainUserPresenter(this); // Khởi tạo Presenter

        // textToSpeech = new TextToSpeech(this, this); // Nếu dùng TTS

        if (savedInstanceState == null) {
            loadFragment(new AutoVoiceFragment()); // Hoặc ProfileFragment làm mặc định
            binding.bottomNavigationView.setSelectedItemId(R.id.nav_tts); // ID của tab mặc định
        }

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tts) selectedFragment = new AutoVoiceFragment();
            else if (itemId == R.id.nav_profile) selectedFragment = new ProfileFragment();
            else if (itemId == R.id.nav_audio_files) selectedFragment = new AudioFilesFragment();
            else if (itemId==R.id.nav_billing) selectedFragment = new BillingFragment();
            else if (itemId == R.id.nav_pricing) selectedFragment = new PricingFragment();
            // ... các case khác cho các fragment ...
            else { Log.w("MainUserActivity", "Unknown navigation item selected: " + itemId); }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        if (fragment == null) return;
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // DÒNG NÀY ĐỂ THÊM HIỆU ỨNG CHO FRAGMENT
        transaction.setCustomAnimations(
                android.R.anim.fade_in,  // Hiệu ứng cho fragment mới đi vào
                android.R.anim.fade_out  // Hiệu ứng cho fragment cũ đi ra
        );

        transaction.replace(R.id.fragment_container, fragment); // Đảm bảo ID container đúng
        transaction.commit();
    }

    // --- PHƯƠNG THỨC ĐƯỢC PROFILEFRAGMENT GỌI ĐỂ LOGOUT ---
    public void performUserLogout() {
        if (presenter != null) {
            Log.d("MainUserActivity", "performUserLogout called, delegating to presenter.");
            presenter.logout(); // Gọi logout của MainUserPresenter
        } else {
            Log.e("MainUserActivity", "Presenter is null in performUserLogout.");
            // Xử lý dự phòng
            new SessionManager(getApplicationContext()).clearSession();
            navigateToLoginScreen(); // Tự điều hướng nếu presenter null
        }
    }

    // --- IMPLEMENT MainUserPresenter.MainUserView ---
    @Override
    public void navigateToLoginScreen() {
        Log.i("MainUserActivity", "navigateToLoginScreen called by Presenter.");
        Intent intent = new Intent(MainUserActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }
    // --------------------------------------------

    // --- TextToSpeech OnInitListener (Nếu vẫn dùng TTS ở Activity) ---
    @Override
    public void onInit(int status) {
        if (textToSpeech == null) return; // Kiểm tra null
        if (status == TextToSpeech.SUCCESS) {
            int langResult = textToSpeech.setLanguage(new Locale("vi_VN")); // Tiếng Việt
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Ngôn ngữ không được hỗ trợ cho TTS", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("TTS_Main", "TextToSpeech Initialization failed!");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (presenter != null) {
            presenter.onDestroy(); // Gọi onDestroy của presenter
        }
        binding = null;
        Log.d("MainUserActivity", "onDestroy called.");
    }
}
