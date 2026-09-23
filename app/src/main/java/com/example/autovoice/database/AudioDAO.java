package com.example.autovoice.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteConstraintException; // Import nếu bạn bắt lỗi này cụ thể
import android.text.TextUtils;
import android.util.Log;

import com.example.autovoice.models.AudioFile; // Import model AudioFile
import com.example.autovoice.database.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class AudioDAO { // <<< TÊN LỚP ĐÃ ĐỔI
    private static final String TAG = "AudioDAO_Debug"; // <<< CẬP NHẬT TAG CHO NHẤT QUÁN
    private SQLiteDatabase database;
    private DBHelper dbHelper;

    private String[] allAudioFileColumns = {
            DBHelper.COLUMN_AUDIO_ID,
            DBHelper.COLUMN_AUDIO_USER_ID_FK,
            DBHelper.COLUMN_AUDIO_FILE_NAME,
            DBHelper.COLUMN_AUDIO_LOCAL_PATH,
            DBHelper.COLUMN_AUDIO_ORIGINAL_TEXT_SNIPPET,
            DBHelper.COLUMN_AUDIO_CREATED_AT_TS, // Đổi tên hằng số trong DBHelper nếu cần
            DBHelper.COLUMN_AUDIO_DISPLAY_NAME_DOWNLOAD,
            DBHelper.COLUMN_AUDIO_VOICE_DETAILS
    };

    public AudioDAO(Context context) { // <<< TÊN CONSTRUCTOR ĐÃ ĐỔI
        dbHelper = new DBHelper(context.getApplicationContext());
        try {
            open();
        } catch (SQLException e) {
            Log.e(TAG, "DATABASE ERROR: Failed to open database during DAO initialization: " + e.getMessage(), e);
        }
    }

    public void open() throws SQLException {
        if (database == null || !database.isOpen()) {
            database = dbHelper.getWritableDatabase();
            Log.i(TAG, "AudioDAO: Database connection opened successfully.");
        }
    }

    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        Log.i(TAG, "AudioDAO: Database connection closed.");
    }

    public long addAudioFile(AudioFile audioFile) {
        if (audioFile == null || database == null || !database.isOpen()) {
            Log.e(TAG, "Invalid params or DB not open in addAudioFile.");
            return -1;
        }
        if (audioFile.getUserId() <= 0 || TextUtils.isEmpty(audioFile.getFileName()) || TextUtils.isEmpty(audioFile.getLocalPath())) {
            Log.e(TAG, "addAudioFile: Missing required fields (userId, fileName, localPath).");
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_AUDIO_USER_ID_FK, audioFile.getUserId());
        values.put(DBHelper.COLUMN_AUDIO_FILE_NAME, audioFile.getFileName());
        values.put(DBHelper.COLUMN_AUDIO_LOCAL_PATH, audioFile.getLocalPath());
        values.put(DBHelper.COLUMN_AUDIO_ORIGINAL_TEXT_SNIPPET, audioFile.getOriginalTextSnippet());
        values.put(DBHelper.COLUMN_AUDIO_CREATED_AT_TS, audioFile.getCreatedAtTimestamp()); // Đảm bảo hằng số này đúng
        values.put(DBHelper.COLUMN_AUDIO_DISPLAY_NAME_DOWNLOAD, audioFile.getDisplayNameForDownload());
        values.put(DBHelper.COLUMN_AUDIO_VOICE_DETAILS, audioFile.getVoiceDetails());

        long insertId = -1;
        try {
            Log.d(TAG, "addAudioFile: Attempting to insert audio file: " + audioFile.getFileName() + " for UserID: " + audioFile.getUserId());
            insertId = database.insertOrThrow(DBHelper.TABLE_AUDIO_FILES, null, values);
            if (insertId > 0) {
                Log.i(TAG, "addAudioFile: AudioFile added successfully with ID: " + insertId);
            }
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "addAudioFile: SQLiteConstraintException for audio file: " + audioFile.getFileName() + ". Message: " + e.getMessage(), e);
            insertId = -2; // Mã lỗi cho vi phạm ràng buộc UNIQUE (thường là local_path)
        } catch (SQLException e) {
            Log.e(TAG, "addAudioFile: SQLException for audio file: " + audioFile.getFileName() + ". Message: " + e.getMessage(), e);
            insertId = -1;
        }
        return insertId;
    }

    public List<AudioFile> getAudioFilesByUserId(int userId) {
        if (database == null || !database.isOpen() || userId <= 0) {
            Log.w(TAG, "DB not open or invalid userId for getAudioFilesByUserId.");
            return new ArrayList<>();
        }
        List<AudioFile> audioFiles = new ArrayList<>();
        Cursor cursor = null;
        String selection = DBHelper.COLUMN_AUDIO_USER_ID_FK + " = ?";
        String[] selectionArgs = {String.valueOf(userId)};
        String orderBy = DBHelper.COLUMN_AUDIO_CREATED_AT_TS + " DESC";

        Log.d(TAG, "getAudioFilesByUserId: Querying for UserID: " + userId);
        try {
            cursor = database.query(DBHelper.TABLE_AUDIO_FILES, allAudioFileColumns,
                    selection, selectionArgs, null, null, orderBy);

            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "getAudioFilesByUserId: Found " + cursor.getCount() + " files for UserID: " + userId);
                while (!cursor.isAfterLast()) {
                    AudioFile audioFile = cursorToAudioFile(cursor);
                    if (audioFile != null) {
                        audioFiles.add(audioFile);
                    }
                    cursor.moveToNext();
                }
            } else {
                Log.d(TAG, "getAudioFilesByUserId: No audio files found for UserID: " + userId);
            }
        } catch (Exception e) {
            Log.e(TAG, "getAudioFilesByUserId: Error querying audio files for UserID: " + userId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return audioFiles;
    }

    public boolean deleteAudioFile(int audioId) {
        if (database == null || !database.isOpen() || audioId <= 0) {
            Log.w(TAG, "deleteAudioFile: DB not open or invalid audioId: " + audioId);
            return false;
        }
        Log.d(TAG, "Attempting to delete audio file with ID: " + audioId);
        int rowsDeleted = 0;
        try {
            rowsDeleted = database.delete(DBHelper.TABLE_AUDIO_FILES,
                    DBHelper.COLUMN_AUDIO_ID + " = ?",
                    new String[]{String.valueOf(audioId)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting audio file with ID: " + audioId, e);
        }
        if (rowsDeleted > 0) {
            Log.i(TAG, "Audio file deleted successfully. ID: " + audioId);
        } else {
            Log.w(TAG, "No audio file deleted for ID: " + audioId + " (might not exist).");
        }
        return rowsDeleted > 0;
    }

    @SuppressLint("Range")
    private AudioFile cursorToAudioFile(Cursor cursor) {
        if (cursor == null) {
            Log.w(TAG, "cursorToAudioFile: Cursor is null.");
            return null;
        }
        AudioFile audioFile = new AudioFile();
        try {
            audioFile.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_AUDIO_ID)));
            audioFile.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_AUDIO_USER_ID_FK)));
            audioFile.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_AUDIO_FILE_NAME)));
            audioFile.setLocalPath(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_AUDIO_LOCAL_PATH)));

            int textSnippetColIdx = cursor.getColumnIndex(DBHelper.COLUMN_AUDIO_ORIGINAL_TEXT_SNIPPET);
            if (textSnippetColIdx != -1) audioFile.setOriginalTextSnippet(cursor.getString(textSnippetColIdx));
            else Log.w(TAG, "Column " + DBHelper.COLUMN_AUDIO_ORIGINAL_TEXT_SNIPPET + " not found in cursor.");


            audioFile.setCreatedAtTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_AUDIO_CREATED_AT_TS)));

            int displayNameColIdx = cursor.getColumnIndex(DBHelper.COLUMN_AUDIO_DISPLAY_NAME_DOWNLOAD);
            if (displayNameColIdx != -1) audioFile.setDisplayNameForDownload(cursor.getString(displayNameColIdx));
            else Log.w(TAG, "Column " + DBHelper.COLUMN_AUDIO_DISPLAY_NAME_DOWNLOAD + " not found in cursor.");

            int voiceDetailsColIdx = cursor.getColumnIndex(DBHelper.COLUMN_AUDIO_VOICE_DETAILS);
            if (voiceDetailsColIdx != -1) audioFile.setVoiceDetails(cursor.getString(voiceDetailsColIdx));
            else Log.w(TAG, "Column " + DBHelper.COLUMN_AUDIO_VOICE_DETAILS + " not found in cursor.");

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "cursorToAudioFile: Column not found in cursor - " + e.getMessage(), e);
            return null;
        }
        return audioFile;
    }
}
