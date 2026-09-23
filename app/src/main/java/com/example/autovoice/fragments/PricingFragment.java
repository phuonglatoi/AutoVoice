package com.example.autovoice.fragments; // Thay bằng package của bạn

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.autovoice.R;

// Giả sử bạn có tệp layout là fragment_pricing.xml
// và một hình ảnh trong drawable tên là success_image (hoặc tên khác)

public class PricingFragment extends Fragment {

    private Button btnSubscribePersonal;
    private Button btnSubscribeCommercial;
    private Button btnContactCorporate;
    private Button btnBuyPayAsYouGo;

    public PricingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pricing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnSubscribePersonal = view.findViewById(R.id.btnSubscribePersonal);
        btnSubscribeCommercial = view.findViewById(R.id.btnSubscribeCommercial);
        btnContactCorporate = view.findViewById(R.id.btnContactCorporate);
        btnBuyPayAsYouGo = view.findViewById(R.id.btnBuyPayAsYouGo);

        View.OnClickListener subscriptionListener = v -> {
            String planName = "";
            if (v.getId() == R.id.btnSubscribePersonal) {
                planName = "Personal Pack";
            } else if (v.getId() == R.id.btnSubscribeCommercial) {
                planName = "Commercial Pack";
            } else if (v.getId() == R.id.btnBuyPayAsYouGo) {
                planName = "Pay as you go";
            }
            showSubscriptionDialog("You have chosen the plan: " + planName + "\nContact me at: phuongletu0110@gmail.com!", R.drawable.success_image); // Thay success_image bằng tên hình ảnh của bạn
        };

        btnSubscribePersonal.setOnClickListener(subscriptionListener);
        btnSubscribeCommercial.setOnClickListener(subscriptionListener);
        btnBuyPayAsYouGo.setOnClickListener(subscriptionListener);

        btnContactCorporate.setOnClickListener(v -> {
            // Xử lý cho nút "CONTACT US", ví dụ: mở email client hoặc form liên hệ
            Toast.makeText(getContext(), "Chuyển đến trang liên hệ...", Toast.LENGTH_SHORT).show();
            // Hoặc hiển thị một dialog khác
            // showSubscriptionDialog("Vui lòng liên hệ với chúng tôi để biết thêm chi tiết về gói Corporate.", R.drawable.contact_us_image);
        });
    }

    private void showSubscriptionDialog(String message, int imageResId) {
        if (getContext() == null) return;

        final Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // Bỏ tiêu đề mặc định của Dialog
        dialog.setCancelable(true); // Cho phép đóng dialog bằng nút back hoặc chạm ra ngoài
        dialog.setContentView(R.layout.dialog_subscription_result); // Tạo layout cho dialog

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Nền trong suốt cho dialog (nếu layout dialog có bo góc)
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvDialogMessage = dialog.findViewById(R.id.tvDialogMessage);
        ImageView ivDialogImage = dialog.findViewById(R.id.ivDialogImage);
        Button btnDialogOk = dialog.findViewById(R.id.btnDialogOk);

        tvDialogMessage.setText(message);
        ivDialogImage.setImageResource(imageResId); // Đảm bảo bạn có hình ảnh này trong drawable

        btnDialogOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}