package com.example.autovoice.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autovoice.R;
import com.example.autovoice.models.User;

import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;
    private OnUserActionListener listener;

    // Cập nhật Interface để thêm các hành động mới
    public interface OnUserActionListener {
        void onDeleteUser(User user);
        void onEditUserPassword(User user); // <<< Sửa mật khẩu
        void onUpgradeUserPackage(User user); // <<< Nâng cấp gói
    }

    public AdminUserAdapter(Context context, List<User> userList, OnUserActionListener listener) {
        this.context = context;
        this.userList = userList != null ? new ArrayList<>(userList) : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Đảm bảo bạn đang dùng layout item đã có các nút mới
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_admin, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.tvUsername.setText(user.getUsername());
        holder.tvEmail.setText(user.getEmail());
        holder.tvSubscription.setText("Gói: " + user.getSubscriptionStatus());

        if (user.isAdmin()) {
            holder.tvRole.setText("Vai trò: Admin");
            holder.ivIcon.setImageResource(R.drawable.ic_admin_placeholder); // Cần tạo drawable
        } else {
            holder.tvRole.setText("Vai trò: User");
            holder.ivIcon.setImageResource(R.drawable.ic_user_placeholder); // Cần tạo drawable
        }

        // Không cho xóa hoặc sửa user "admin" chính
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            holder.btnDeleteUser.setVisibility(View.GONE);
            holder.btnEditUserPassword.setVisibility(View.GONE);
            holder.btnUpgradePackage.setVisibility(View.GONE);
        } else {
            holder.btnDeleteUser.setVisibility(View.VISIBLE);
            holder.btnEditUserPassword.setVisibility(View.VISIBLE);
            holder.btnUpgradePackage.setVisibility(View.VISIBLE);

            holder.btnDeleteUser.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteUser(user);
                }
            });

            // Xử lý sự kiện click nút sửa mật khẩu
            holder.btnEditUserPassword.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditUserPassword(user);
                }
            });

            // Xử lý sự kiện click nút nâng cấp gói
            holder.btnUpgradePackage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUpgradeUserPackage(user);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateUserList(List<User> newUserList) {
        if (newUserList != null) {
            this.userList = new ArrayList<>(newUserList);
        } else {
            this.userList = new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvUsername, tvRole, tvEmail, tvSubscription;
        ImageButton btnDeleteUser, btnEditUserPassword, btnUpgradePackage; // Đổi tên cho nhất quán

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_user_icon);
            tvUsername = itemView.findViewById(R.id.tv_item_username);
            tvRole = itemView.findViewById(R.id.tv_item_role);
            tvEmail = itemView.findViewById(R.id.tv_item_email);
            tvSubscription = itemView.findViewById(R.id.tv_item_subscription);
            // Đảm bảo ID các nút này khớp với item_user_admin.xml
            btnDeleteUser = itemView.findViewById(R.id.btn_delete_user);
            btnEditUserPassword = itemView.findViewById(R.id.btn_edit_user_password);
            btnUpgradePackage = itemView.findViewById(R.id.btn_upgrade_package);
        }
    }
}