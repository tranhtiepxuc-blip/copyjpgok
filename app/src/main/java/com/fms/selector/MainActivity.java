package com.fms.selector;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 2001;
    private static final int STORAGE_PERMISSION_REQUEST = 2002;
    private static final String FAKE_FOLDER_PATH = "/sdcard/FMS_Fake";
    private static final String FAKE_IMAGE_PATH = "/sdcard/FMS_Fake/fake.jpg";

    private ImageView imgPreview;
    private Button btnPickImage;
    private Button btnRequestPermission;
    private Spinner spinnerResolution;
    private TextView txtStatus;

    // Các tùy chọn độ phân giải
    private final String[] resolutions = {
        "Chuẩn 4:3 (1280 x 960) - Khuyên dùng",
        "Chuẩn 16:9 (1920 x 1080)",
        "Chuẩn 16:9 (1280 x 720)",
        "Giữ nguyên gốc của ảnh chọn"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgPreview = findViewById(R.id.imgPreview);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnRequestPermission = findViewById(R.id.btnRequestPermission);
        spinnerResolution = findViewById(R.id.spinnerResolution);
        txtStatus = findViewById(R.id.txtStatus);

        // Thiết lập dữ liệu cho Spinner chọn kích thước
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, resolutions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerResolution.setAdapter(adapter);

        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkStoragePermission()) {
                    openGallery();
                } else {
                    requestStoragePermission();
                }
            }
        });

        btnRequestPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestStoragePermission();
            }
        });

        checkPermissionAndShowButtons();
        loadCurrentImage();
    }

    private void checkPermissionAndShowButtons() {
        if (checkStoragePermission()) {
            btnRequestPermission.setVisibility(View.GONE);
            btnPickImage.setEnabled(true);
        } else {
            btnRequestPermission.setVisibility(View.VISIBLE);
            btnPickImage.setEnabled(false);
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true; 
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                startActivityForResult(intent, STORAGE_PERMISSION_REQUEST);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, STORAGE_PERMISSION_REQUEST);
            }
        } else {
            checkPermissionAndShowButtons();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            checkPermissionAndShowButtons();
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            processAndSaveImage(selectedImageUri);
        }
    }

    private void processAndSaveImage(Uri selectedImageUri) {
        try {
            // Đảm bảo thư mục đích tồn tại
            File folder = new File(FAKE_FOLDER_PATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File destFile = new File(FAKE_IMAGE_PATH);

            // 1. Giải mã hình ảnh đã chọn từ Uri
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }

            if (originalBitmap == null) {
                Toast.makeText(this, "Không thể đọc dữ liệu ảnh!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Xác định kích thước mục tiêu dựa trên lựa chọn của người dùng
            int selectedPosition = spinnerResolution.getSelectedItemPosition();
            int targetWidth = originalBitmap.getWidth();
            int targetHeight = originalBitmap.getHeight();

            if (selectedPosition == 0) { // 1280 x 960 (4:3)
                targetWidth = 1280;
                targetHeight = 960;
            } else if (selectedPosition == 1) { // 1920 x 1080 (16:9)
                targetWidth = 1920;
                targetHeight = 1080;
            } else if (selectedPosition == 2) { // 1280 x 720 (16:9)
                targetWidth = 1280;
                targetHeight = 720;
            }

            // 3. Thực hiện co giãn (Resize) ảnh chất lượng cao
            Bitmap finalBitmap;
            if (selectedPosition != 3) { // Nếu không chọn giữ nguyên gốc
                finalBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
                XposedBridgeLog("Đã resize ảnh từ " + originalBitmap.getWidth() + "x" + originalBitmap.getHeight() + " thành " + targetWidth + "x" + targetHeight);
            } else {
                finalBitmap = originalBitmap;
            }

            // 4. Lưu trực tiếp xuống bộ nhớ máy với định dạng JPEG chất lượng cao (95%)
            FileOutputStream outputStream = new FileOutputStream(destFile);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
            outputStream.flush();
            outputStream.close();

            // Thu hồi tài nguyên RAM
            if (finalBitmap != originalBitmap) {
                finalBitmap.recycle();
            }
            originalBitmap.recycle();

            Toast.makeText(this, "Đã tối ưu hóa kích thước và lưu ảnh thành công!", Toast.LENGTH_SHORT).show();
            loadCurrentImage(); 

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý hình ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadCurrentImage() {
        try {
            File file = new File(FAKE_IMAGE_PATH);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(FAKE_IMAGE_PATH);
                if (bitmap != null) {
                    imgPreview.setImageBitmap(bitmap);
                    txtStatus.setText("Độ phân giải: " + bitmap.getWidth() + " x " + bitmap.getHeight() + " pixels");
                }
            } else {
                imgPreview.setImageResource(android.R.drawable.ic_menu_gallery);
                txtStatus.setText("Đường dẫn: " + FAKE_IMAGE_PATH);
            }
        } catch (Exception e) {
            imgPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private void XposedBridgeLog(String message) {
        // Hàm phụ trợ ghi log hệ thống
        android.util.Log.d("FMS_SELECTOR", message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionAndShowButtons();
        loadCurrentImage();
    }
}


