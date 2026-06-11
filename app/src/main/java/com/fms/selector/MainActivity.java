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

    private ImageView imgPreview;
    private Button btnPickImage;
    private Button btnRequestPermission;
    private Spinner spinnerResolution;
    private TextView txtStatus;

    // Tự động phân giải thư mục bộ nhớ động thay vì gán cứng /sdcard/
    private String getFakeFolderPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/FMS_Fake";
    }

    private String getFakeImagePath() {
        return getFakeFolderPath() + "/fake.jpg";
    }

    private final String[] resolutions = {
        "Chuẩn 4:3 (1280 x 960) - Khuyên dùng",
        "Chuẩn 16:9 (1920 x 1080)",
        "Chuẩn 16:9 (1280 x 720)",
        "Giữ nguyên gốc của ảnh chọn"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            imgPreview = findViewById(R.id.imgPreview);
            btnPickImage = findViewById(R.id.btnPickImage);
            btnRequestPermission = findViewById(R.id.btnRequestPermission);
            spinnerResolution = findViewById(R.id.spinnerResolution);
            txtStatus = findViewById(R.id.txtStatus);

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
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khởi tạo giao diện: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở thư viện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
            File folder = new File(getFakeFolderPath());
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File destFile = new File(getFakeImagePath());

            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }

            if (originalBitmap == null) {
                Toast.makeText(this, "Không đọc được tệp hình ảnh!", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedPosition = spinnerResolution.getSelectedItemPosition();
            int targetWidth = originalBitmap.getWidth();
            int targetHeight = originalBitmap.getHeight();

            if (selectedPosition == 0) { 
                targetWidth = 1280;
                targetHeight = 960;
            } else if (selectedPosition == 1) { 
                targetWidth = 1920;
                targetHeight = 1080;
            } else if (selectedPosition == 2) { 
                targetWidth = 1280;
                targetHeight = 720;
            }

            Bitmap finalBitmap;
            if (selectedPosition != 3) { 
                finalBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);
            } else {
                finalBitmap = originalBitmap;
            }

            FileOutputStream outputStream = new FileOutputStream(destFile);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
            outputStream.flush();
            outputStream.close();

            if (finalBitmap != originalBitmap) {
                finalBitmap.recycle();
            }
            originalBitmap.recycle();

            Toast.makeText(this, "Đã tối ưu hóa tỉ lệ và nạp ảnh thành công!", Toast.LENGTH_SHORT).show();
            loadCurrentImage(); 

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý sao chép ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadCurrentImage() {
        try {
            File file = new File(getFakeImagePath());
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(getFakeImagePath());
                if (bitmap != null) {
                    imgPreview.setImageBitmap(bitmap);
                    txtStatus.setText("Độ phân giải hiện tại: " + bitmap.getWidth() + " x " + bitmap.getHeight() + " pixels");
                }
            } else {
                imgPreview.setImageResource(android.R.drawable.ic_menu_gallery);
                txtStatus.setText("Lưu tại: " + getFakeImagePath());
            }
        } catch (Exception e) {
            imgPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionAndShowButtons();
        loadCurrentImage();
    }
}


