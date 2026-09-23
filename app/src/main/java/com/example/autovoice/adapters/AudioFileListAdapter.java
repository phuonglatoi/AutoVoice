package com.example.autovoice.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log; // Import Log

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.autovoice.R; // Đảm bảo import R đúng
import com.example.autovoice.models.AudioFile; // Import model AudioFile

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AudioFileListAdapter extends ArrayAdapter<AudioFile> {

    private static final String TAG = "AudioFileListAdapter";
    private Context mContext;
    private List<AudioFile> mAudioFiles; // Danh sách các file audio
    private OnAudioFileActionListener mListener; // Listener để xử lý hành động

    // Interface để Fragment (hoặc Activity) xử lý các hành động click trên item
    public interface OnAudioFileActionListener {
        void onPlayAudioClicked(AudioFile audioFile, ImageButton playButton); // Truyền cả nút Play để cập nhật icon
        void onDownloadAudioClicked(AudioFile audioFile);
        // void onDeleteAudioClicked(AudioFile audioFile); // Thêm nếu muốn có chức năng xóa từ danh sách
    }

    public AudioFileListAdapter(@NonNull Context context, @NonNull List<AudioFile> audioFiles, OnAudioFileActionListener listener) {
        super(context, 0, audioFiles); // resource là 0 vì chúng ta tự inflate layout item
        this.mContext = context;
        this.mAudioFiles = audioFiles;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        ViewHolder viewHolder;

        if (listItemView == null) {
            listItemView = LayoutInflater.from(mContext).inflate(R.layout.list_item_audio_file, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.ivAudioIcon = listItemView.findViewById(R.id.ivAudioIcon);
            viewHolder.tvAudioFileName = listItemView.findViewById(R.id.tvAudioFileName);
            viewHolder.tvAudioOriginalText = listItemView.findViewById(R.id.tvAudioOriginalText);
            viewHolder.tvAudioDate = listItemView.findViewById(R.id.tvAudioDate);
            viewHolder.btnItemPlayAudio = listItemView.findViewById(R.id.btnItemPlayAudio);
            viewHolder.btnItemDownloadAudio = listItemView.findViewById(R.id.btnItemDownloadAudio);
            listItemView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) listItemView.getTag();
        }

        AudioFile currentAudioFile = getItem(position); // Lấy AudioFile tại vị trí hiện tại

        if (currentAudioFile != null) {
            // Đặt dữ liệu cho các View
            // viewHolder.ivAudioIcon.setImageResource(R.drawable.ic_audio_file); // Icon mặc định

            String displayName = currentAudioFile.getDisplayNameForDownload();
            if (TextUtils.isEmpty(displayName)) {
                displayName = currentAudioFile.getFileName(); // Dùng fileName nếu displayName rỗng
            }
            viewHolder.tvAudioFileName.setText(displayName != null ? displayName : "N/A");

            String snippet = currentAudioFile.getOriginalTextSnippet();
            viewHolder.tvAudioOriginalText.setText("Text: " + (snippet != null ? snippet : "N/A"));

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            viewHolder.tvAudioDate.setText("Create Date: " + sdf.format(new Date(currentAudioFile.getCreatedAtTimestamp())));

            // Xử lý sự kiện click cho nút Play
            viewHolder.btnItemPlayAudio.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onPlayAudioClicked(currentAudioFile, viewHolder.btnItemPlayAudio);
                }
            });

            // Xử lý sự kiện click cho nút Download
            viewHolder.btnItemDownloadAudio.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onDownloadAudioClicked(currentAudioFile);
                }
            });

            // TODO: Cập nhật icon Play/Pause cho viewHolder.btnItemPlayAudio
            // dựa trên trạng thái phát của file audio này (cần Fragment quản lý)
            // Ví dụ: if (fragment.isCurrentlyPlaying(currentAudioFile)) {
            //            viewHolder.btnItemPlayAudio.setImageResource(R.drawable.ic_pause);
            //        } else {
            //            viewHolder.btnItemPlayAudio.setImageResource(R.drawable.ic_play_arrow);
            //        }

        } else {
            Log.w(TAG, "getView: currentAudioFile is null at position " + position);
        }

        return listItemView;
    }

    // ViewHolder pattern để tối ưu hiệu suất
    private static class ViewHolder {
        ImageView ivAudioIcon;
        TextView tvAudioFileName;
        TextView tvAudioOriginalText;
        TextView tvAudioDate;
        ImageButton btnItemPlayAudio;
        ImageButton btnItemDownloadAudio;
    }

    // Cập nhật dữ liệu cho Adapter (nếu cần thiết từ bên ngoài)
    public void setData(List<AudioFile> audioFiles) {
        // this.mAudioFiles.clear(); // ArrayAdapter tự quản lý list bên trong
        // if (audioFiles != null) {
        //    this.mAudioFiles.addAll(audioFiles);
        // }
        // notifyDataSetChanged();
        // Đối với ArrayAdapter, cách tốt hơn là dùng clear() và addAll() của chính adapter
        super.clear();
        if (audioFiles != null) {
            super.addAll(audioFiles);
        }
        notifyDataSetChanged(); // Thông báo cho ListView cập nhật
        Log.d(TAG, "Adapter data updated. New size: " + (audioFiles != null ? audioFiles.size() : 0));
    }

    // Ghi đè getItem để đảm bảo trả về đúng đối tượng từ mAudioFiles
    // (ArrayAdapter mặc định đã làm điều này, nhưng để rõ ràng hơn)
    @Nullable
    @Override
    public AudioFile getItem(int position) {
        if (mAudioFiles != null && position >= 0 && position < mAudioFiles.size()) {
            return mAudioFiles.get(position);
        }
        return null; // Hoặc super.getItem(position) nếu bạn không quản lý list riêng
    }

    @Override
    public int getCount() {
        return mAudioFiles != null ? mAudioFiles.size() : 0;
    }
}
