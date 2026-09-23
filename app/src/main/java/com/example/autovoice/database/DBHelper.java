package com.example.autovoice.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.database.Cursor; // Import Cursor nếu bạn dùng trong onUpgrade để kiểm tra cột


import androidx.annotation.Nullable;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "autovoice.db";
    // --- QUAN TRỌNG: TĂNG DATABASE_VERSION LÊN 8 ---
    // Giả sử phiên bản trước đó của bạn là 7 và chưa có bảng audio_files
    private static final int DATABASE_VERSION = 8; // <<< ĐÃ NÂNG LÊN 8

    // Bảng Users (giữ nguyên các định nghĩa cột)
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USER_EMAIL = "email";
    public static final String COLUMN_USER_USERNAME = "username";
    public static final String COLUMN_USER_PASSWORD = "password";
    public static final String COLUMN_USER_SUBSCRIPTION_STATUS = "subscription_status";
    public static final String COLUMN_USER_IS_ADMIN = "is_admin";
    public static final String COLUMN_USER_PROFILE_IMAGE_LOCAL_PATH = "profile_image_local_path";
    public static final String COLUMN_USER_AUTH_PROVIDER = "auth_provider";

    private static final String SQL_CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_USER_EMAIL + " TEXT UNIQUE NOT NULL," +
                    COLUMN_USER_USERNAME + " TEXT UNIQUE NOT NULL," +
                    COLUMN_USER_PASSWORD + " TEXT," + // Cho phép NULL
                    COLUMN_USER_SUBSCRIPTION_STATUS + " TEXT DEFAULT 'Free'," +
                    COLUMN_USER_IS_ADMIN + " INTEGER NOT NULL DEFAULT 0," +
                    COLUMN_USER_PROFILE_IMAGE_LOCAL_PATH + " TEXT," +
                    COLUMN_USER_AUTH_PROVIDER + " TEXT DEFAULT 'EMAIL'" +
                    ");";

    // Định nghĩa bảng AUDIO_FILES
    public static final String TABLE_AUDIO_FILES = "audio_files";
    public static final String COLUMN_AUDIO_ID = "audio_id";
    public static final String COLUMN_AUDIO_USER_ID_FK = "user_id";
    public static final String COLUMN_AUDIO_FILE_NAME = "file_name";
    public static final String COLUMN_AUDIO_LOCAL_PATH = "local_path";
    public static final String COLUMN_AUDIO_ORIGINAL_TEXT_SNIPPET = "original_text_snippet";
    public static final String COLUMN_AUDIO_CREATED_AT_TS = "created_at_ts";
    public static final String COLUMN_AUDIO_DISPLAY_NAME_DOWNLOAD = "display_name_download";
    public static final String COLUMN_AUDIO_VOICE_DETAILS = "voice_details";

    private static final String SQL_CREATE_TABLE_AUDIO_FILES =
            "CREATE TABLE " + TABLE_AUDIO_FILES + " (" +
                    COLUMN_AUDIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_AUDIO_USER_ID_FK + " INTEGER NOT NULL," +
                    COLUMN_AUDIO_FILE_NAME + " TEXT NOT NULL," +
                    COLUMN_AUDIO_LOCAL_PATH + " TEXT UNIQUE NOT NULL," +
                    COLUMN_AUDIO_ORIGINAL_TEXT_SNIPPET + " TEXT," +
                    COLUMN_AUDIO_CREATED_AT_TS + " INTEGER NOT NULL," +
                    COLUMN_AUDIO_DISPLAY_NAME_DOWNLOAD + " TEXT," +
                    COLUMN_AUDIO_VOICE_DETAILS + " TEXT," +
                    "FOREIGN KEY(" + COLUMN_AUDIO_USER_ID_FK + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_USER_ID + ") ON DELETE CASCADE" +
                    ");";

    public DBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("DBHelper", "onCreate: Creating database tables (version " + DATABASE_VERSION + ")");
        db.execSQL(SQL_CREATE_TABLE_USERS);
        Log.i("DBHelper", "onCreate: Table " + TABLE_USERS + " created.");
        db.execSQL(SQL_CREATE_TABLE_AUDIO_FILES); // Tạo bảng audio_files khi DB được tạo lần đầu
        Log.i("DBHelper", "onCreate: Table " + TABLE_AUDIO_FILES + " created.");
        addDefaultAdminUser(db);
        addDefaultNormalUser(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("DBHelper", "onUpgrade: Upgrading database from version " + oldVersion + " to " + newVersion);

        // Xử lý nâng cấp cho bảng USERS (ví dụ: thêm cột auth_provider nếu oldVersion < 6)
        if (oldVersion < 6) {
            try {
                Cursor cursor = db.rawQuery("PRAGMA table_info(" + TABLE_USERS + ")", null);
                boolean authProviderColumnExists = false;
                if (cursor != null) {
                    int nameColumnIndex = cursor.getColumnIndex("name");
                    while(cursor.moveToNext()){
                        if (nameColumnIndex != -1 && COLUMN_USER_AUTH_PROVIDER.equals(cursor.getString(nameColumnIndex))){
                            authProviderColumnExists = true;
                            break;
                        }
                    }
                    cursor.close();
                }
                if (!authProviderColumnExists) {
                    db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_AUTH_PROVIDER + " TEXT DEFAULT 'EMAIL';");
                    Log.i("DBHelper", "onUpgrade: Column " + COLUMN_USER_AUTH_PROVIDER + " added to " + TABLE_USERS + ".");
                } else {
                    Log.i("DBHelper", "onUpgrade: Column " + COLUMN_USER_AUTH_PROVIDER + " already exists in " + TABLE_USERS + ".");
                }
            } catch (Exception e) {
                Log.e("DBHelper", "onUpgrade: Error adding " + COLUMN_USER_AUTH_PROVIDER + " column. Recreating users table as fallback.", e);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS); // Phải xóa audio_files trước nếu có FK
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUDIO_FILES);
                db.execSQL(SQL_CREATE_TABLE_USERS);
                db.execSQL(SQL_CREATE_TABLE_AUDIO_FILES); // Tạo lại cả hai
                addDefaultAdminUser(db);
                addDefaultNormalUser(db);
                return; // Đã tạo lại, không cần chạy logic dưới
            }
        }
        // Thêm các nâng cấp khác cho bảng USERS nếu có giữa version 6 và version 8

        // Tạo bảng AUDIO_FILES nếu đang nâng cấp từ phiên bản chưa có nó (ví dụ oldVersion < 8)
        if (oldVersion < 8 && newVersion >= 8) { // Đảm bảo chỉ chạy khi nâng cấp qua version 8
            try {
                Log.i("DBHelper", "onUpgrade: Creating " + TABLE_AUDIO_FILES + " table as oldVersion ("+oldVersion+") < 8.");
                db.execSQL(SQL_CREATE_TABLE_AUDIO_FILES);
                Log.i("DBHelper", "onUpgrade: Table " + TABLE_AUDIO_FILES + " created successfully.");
            } catch (Exception e) {
                Log.e("DBHelper", "onUpgrade: Error creating " + TABLE_AUDIO_FILES + " table. " + e.getMessage());
                // Nếu bạn muốn an toàn hơn trong trường hợp này, có thể không xóa bảng users
                // mà chỉ log lỗi và chấp nhận bảng audio_files có thể không được tạo.
                // Hoặc, nếu đang phát triển, xóa và tạo lại toàn bộ là một lựa chọn:
                // Log.e("DBHelper", "Recreating all tables due to error creating audio_files table.");
                // db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUDIO_FILES);
                // db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
                // onCreate(db);
            }
        }
    }

    private void addDefaultAdminUser(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_EMAIL, "admin@example.com".toLowerCase(Locale.ROOT));
        values.put(COLUMN_USER_USERNAME, "admin".toLowerCase(Locale.ROOT));
        String adminHashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());
        values.put(COLUMN_USER_PASSWORD, adminHashedPassword);
        values.put(COLUMN_USER_SUBSCRIPTION_STATUS, "Premium");
        values.put(COLUMN_USER_IS_ADMIN, 1);
        values.put(COLUMN_USER_AUTH_PROVIDER, "EMAIL");
        db.insert(TABLE_USERS, null, values);
        Log.i("DBHelper", "Default admin user added.");
    }

    private void addDefaultNormalUser(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_EMAIL, "user@example.com".toLowerCase(Locale.ROOT));
        values.put(COLUMN_USER_USERNAME, "user".toLowerCase(Locale.ROOT));
        String userHashedPassword = BCrypt.hashpw("user123", BCrypt.gensalt());
        values.put(COLUMN_USER_PASSWORD, userHashedPassword);
        values.put(COLUMN_USER_SUBSCRIPTION_STATUS, "Free");
        values.put(COLUMN_USER_IS_ADMIN, 0);
        values.put(COLUMN_USER_AUTH_PROVIDER, "EMAIL");
        db.insert(TABLE_USERS, null, values);
        Log.i("DBHelper", "Default normal user added.");
    }
}
