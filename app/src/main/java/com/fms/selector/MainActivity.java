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
import android.widget.Button;
import android.widget.ImageView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgPreview = findViewById(R.id.imgPreview);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnRequestPermission = findViewById(R.id.btnRequestPermission);

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
            copyImageToFakeFolder(selectedImageUri);
        }
    }

    private void copyImageToFakeFolder(Uri selectedImageUri) {
        try {
            File folder = new File(FAKE_FOLDER_PATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File destFile = new File(FAKE_IMAGE_PATH);

            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            FileOutputStream outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            Toast.makeText(this, "Đã lưu ảnh giả lập mới thành công!", Toast.LENGTH_SHORT).show();
            loadCurrentImage(); 

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi sao chép ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadCurrentImage() {
        File file = new File(FAKE_IMAGE_PATH);
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(FAKE_IMAGE_PATH);
            if (bitmap != null) {
                imgPreview.setImageBitmap(bitmap);
            }
        } else {
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
