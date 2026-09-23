package com.example.autovoice.fragments; // Thay com.example.yourapp bằng package của ứng dụng bạn

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.autovoice.R;

// Giả sử bạn có một lớp UserSessionManager hoặc tương tự để lấy thông tin người dùng
// import com.example.yourapp.auth.UserSessionManager;
// Giả sử bạn có một lớp BillingService hoặc ViewModel để lấy thông tin thanh toán
// import com.example.yourapp.billing.BillingViewModel;


public class BillingFragment extends Fragment {

    // Khai báo các Views
    private TextView tvFragmentTitle;
    private TextView tvAccountEmailValue;
    private TextView tvUserNameValue;
    private TextView tvSubscriptionStatusValue;
    // TextView tvCurrentPackValue; // ID này không có trong layout bạn cung cấp ở lần này, nhưng có trong layout trước.
    // Hãy kiểm tra lại layout của bạn. Tôi sẽ comment nó ra.
    // Nếu nhãn "Mặc Định:" tương ứng với "Gói hiện tại:", bạn cần thêm ID cho TextView giá trị của nó.
    // Giả sử nhãn "Số dư:" tương ứng với TextView có id là tvBalanceValue
    private TextView tvBalanceValue; // ID này bị thiếu một TextView giá trị tương ứng cho nhãn "Mặc định:"
    // và TextView giá trị cho "Số dư:" hiện có ID tvBalanceValue
    // Tôi sẽ ánh xạ tvBalanceValue cho giá trị của "Số dư:" như trong layout.
    // TextView cho giá trị "Mặc định:" cần được thêm ID trong XML nếu bạn muốn cập nhật nó.

    private TextView tvPaymentHistoryTitle;
    private TextView tvPaymentHistoryContent;

    // Ví dụ về ViewModel (Tùy chọn, nhưng được khuyến nghị)
    // private BillingViewModel billingViewModel;
    // private UserSessionManager sessionManager;


    public BillingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // Hãy đảm bảo tên layout là chính xác (ví dụ: R.layout.fragment_billing)
        return inflater.inflate(R.layout.fragment_billing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ Views từ layout
        tvFragmentTitle = view.findViewById(R.id.tvFragmentTitle);
        tvAccountEmailValue = view.findViewById(R.id.tvAccountEmailValue);
        tvUserNameValue = view.findViewById(R.id.tvUserNameValue);
        tvSubscriptionStatusValue = view.findViewById(R.id.tvSubscriptionStatusValue);
        // tvCurrentPackValue = view.findViewById(R.id.tvCurrentPackValue); // Đã comment ở trên
        tvBalanceValue = view.findViewById(R.id.tvBalanceValue); // ID này trong layout của bạn là giá trị cho "Số dư:"

        tvPaymentHistoryTitle = view.findViewById(R.id.tvPaymentHistoryTitle);
        tvPaymentHistoryContent = view.findViewById(R.id.tvPaymentHistoryContent);

        // Khởi tạo ViewModel hoặc các dịch vụ khác (nếu có)
        // sessionManager = new UserSessionManager(requireContext());
        // billingViewModel = new ViewModelProvider(this).get(BillingViewModel.class);

        // Tải và hiển thị dữ liệu
        loadBillingData();
    }

    private void loadBillingData() {
        // ----- PHẦN NÀY BẠN SẼ THAY THẾ BẰNG LOGIC LẤY DỮ LIỆU THỰC TẾ -----

        // Ví dụ: Lấy thông tin người dùng từ SessionManager (nếu có)
        // User currentUser = sessionManager.getCurrentUser();
        // if (currentUser != null) {
        //     tvAccountEmailValue.setText(currentUser.getEmail());
        //     tvUserNameValue.setText(currentUser.getUsername());
        // } else {
        //     tvAccountEmailValue.setText("N/A");
        //     tvUserNameValue.setText("N/A");
        // }

        // Ví dụ: Lấy thông tin thanh toán từ ViewModel hoặc API
        // billingViewModel.getBillingInfo().observe(getViewLifecycleOwner(), billingInfo -> {
        //     if (billingInfo != null) {
        //         tvSubscriptionStatusValue.setText(billingInfo.getSubscriptionStatus());
        //         // tvCurrentPackValue.setText(billingInfo.getCurrentPack()); // Nếu bạn dùng ID này
        //         // Giả sử bạn có một TextView riêng cho giá trị "Mặc định:"
        //         // tvDefaultPackValue.setText(billingInfo.getDefaultPackInfo());
        //         tvBalanceValue.setText(billingInfo.getBalance()); // Giá trị cho "Số dư:"
        //         tvPaymentHistoryContent.setText(formatPaymentHistory(billingInfo.getPaymentHistoryList()));
        //     } else {
        //         // Xử lý trường hợp không có thông tin thanh toán
        //         tvSubscriptionStatusValue.setText("Không có thông tin");
        //         tvBalanceValue.setText("Không có thông tin");
        //         tvPaymentHistoryContent.setText("Chưa có lịch sử thanh toán.");
        //     }
        // });

        // Dữ liệu mẫu (như trong XML) để bạn thấy nó hoạt động
        tvAccountEmailValue.setText("user1@example.com");
        tvUserNameValue.setText("user1");
        tvSubscriptionStatusValue.setText("No active Subscription");
        // tvCurrentPackValue.setText("1,000,000 characters"); // Ví dụ nếu có ID này
        // Giả sử bạn có một TextView giá trị cho "Mặc Định:" (hiện tại chưa có ID trong XML)
        // someTextViewForDefaultValue.setText("Thông tin mặc định của gói");

        // TextView có ID tvBalanceValue trong XML của bạn hiện đang hiển thị "500 characters / Converter"
        // và nó tương ứng với nhãn "Số dư:" trong hình ảnh gốc.
        // Tuy nhiên, trong XML bạn cung cấp, nhãn "Mặc Định:" đứng trước nhãn "Số dư:",
        // nhưng chỉ có một TextView giá trị là tvBalanceValue nằm ở vị trí thứ 4 (tương ứng với "Mặc Định:" nếu theo thứ tự).
        // Hãy kiểm tra lại ID và vị trí các TextView giá trị trong XML của bạn.
        // Dựa theo ID tvBalanceValue và text trong XML, nó đang hiển thị thông tin cho "Mặc Định:"
        tvBalanceValue.setText("500 characters / Converter");


        tvPaymentHistoryContent.setText(" - 2024-05-10: Thanh toán gói Premium - $10\n - 2024-04-10: Thanh toán gói Basic - $5");

        // Nếu bạn muốn thay đổi tiêu đề động
        // tvFragmentTitle.setText("Thông tin Thanh toán");
    }

    // Ví dụ hàm định dạng lịch sử thanh toán (nếu bạn lấy dưới dạng List)
    // private String formatPaymentHistory(List<PaymentRecord> historyList) {
    //    if (historyList == null || historyList.isEmpty()) {
    //        return "Chưa có lịch sử thanh toán.";
    //    }
    //    StringBuilder sb = new StringBuilder();
    //    for (PaymentRecord record : historyList) {
    //        sb.append(" - ").append(record.getDate())
    //          .append(": ").append(record.getDescription())
    //          .append(" - ").append(record.getAmount())
    //          .append("\n");
    //    }
    //    return sb.toString().trim();
    // }
}