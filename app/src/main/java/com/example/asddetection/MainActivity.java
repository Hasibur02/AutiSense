package com.example.asddetection;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {

    // ============================================================
    // UI
    // ============================================================

    private ImageView selectedImage;
    private TextView resultText;
    private ProgressBar progressBar;

    private PreviewView cameraPreview;

    private Button captureButton;
    private Button cameraButton;
    private Button switchCameraButton;
    private Button galleryButton;
    private Button predictButton;


    // ============================================================
    // Selected Image
    // ============================================================

    private Uri selectedImageUri;


    // ============================================================
    // CameraX
    // ============================================================

    private ImageCapture imageCapture;

    private ExecutorService cameraExecutor;


    // ============================================================
    // Camera Selector
    // Front Camera = Default
    // ============================================================

    private CameraSelector cameraSelector =
            CameraSelector.DEFAULT_FRONT_CAMERA;


    // ============================================================
    // Reset after ResultActivity
    // ============================================================

    private boolean resetWhenResume = false;


    // ============================================================
    // Gallery Launcher
    // ============================================================

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {

                        if (uri != null) {

                            selectedImageUri = uri;

                            showSelectedImage(uri);

                            resultText.setText(
                                    "Image selected.\nPress Predict."
                            );
                        }
                    }
            );


    // ============================================================
    // Camera Permission
    // ============================================================

    private final ActivityResultLauncher<String>
            cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {

                        if (granted) {

                            openCamera();

                        } else {

                            hideCameraUI();

                            cameraButton.setEnabled(true);

                            Toast.makeText(
                                    MainActivity.this,
                                    "Camera permission required",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );


    // ============================================================
    // onCreate
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        // ========================================================
        // Find Views
        // ========================================================

        cameraPreview =
                findViewById(R.id.cameraPreview);

        captureButton =
                findViewById(R.id.captureButton);

        cameraButton =
                findViewById(R.id.cameraButton);

        switchCameraButton =
                findViewById(R.id.switchCameraButton);

        galleryButton =
                findViewById(R.id.galleryButton);

        predictButton =
                findViewById(R.id.predictButton);

        selectedImage =
                findViewById(R.id.selectedImage);

        resultText =
                findViewById(R.id.resultText);

        progressBar =
                findViewById(R.id.progressBar);


        // ========================================================
        // Camera Executor
        // ========================================================

        cameraExecutor =
                Executors.newSingleThreadExecutor();


        // ========================================================
        // Initial State
        // ========================================================

        resetHomePage();


        // ========================================================
        // Gallery Button
        // ========================================================

        galleryButton.setOnClickListener(v -> {

            galleryLauncher.launch("image/*");

        });


        // ========================================================
        // Camera Button
        // ========================================================

        cameraButton.setOnClickListener(v -> {

            cameraButton.setEnabled(false);

            cameraPreview.setVisibility(
                    View.VISIBLE
            );

            captureButton.setVisibility(
                    View.VISIBLE
            );

            switchCameraButton.setVisibility(
                    View.VISIBLE
            );


            if (
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                    )
                            == PackageManager.PERMISSION_GRANTED
            ) {

                openCamera();

            } else {

                cameraPermissionLauncher.launch(
                        Manifest.permission.CAMERA
                );
            }

        });


        // ========================================================
        // Switch Camera
        // ========================================================

        switchCameraButton.setOnClickListener(v -> {

            switchCamera();

        });


        // ========================================================
        // Capture Button
        // ========================================================

        captureButton.setOnClickListener(v -> {

            takePhoto();

        });


        // ========================================================
        // Predict Button
        // ========================================================

        predictButton.setOnClickListener(v -> {

            if (selectedImageUri == null) {

                Toast.makeText(
                        MainActivity.this,
                        "Please select or capture an image first.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            uploadImage(selectedImageUri);

        });

    }


    // ============================================================
    // Reset Home Page
    // ============================================================

    private void resetHomePage() {

        selectedImageUri = null;


        // Clear image

        selectedImage.setImageDrawable(null);

        selectedImage.setVisibility(
                View.GONE
        );


        // Hide camera

        hideCameraUI();


        // Buttons

        cameraButton.setEnabled(true);

        galleryButton.setEnabled(true);

        predictButton.setEnabled(true);


        // Predict hidden until image selected

        predictButton.setVisibility(
                View.GONE
        );


        // Front camera default

        cameraSelector =
                CameraSelector.DEFAULT_FRONT_CAMERA;


        // Status

        resultText.setText(
                "Upload or capture an image to begin."
        );


        // Progress

        progressBar.setVisibility(
                View.GONE
        );
    }


    // ============================================================
    // Hide Camera UI
    // ============================================================

    private void hideCameraUI() {

        cameraPreview.setVisibility(
                View.GONE
        );

        captureButton.setVisibility(
                View.GONE
        );

        switchCameraButton.setVisibility(
                View.GONE
        );
    }


    // ============================================================
    // Show Selected Image
    // ============================================================

    private void showSelectedImage(Uri uri) {

        try {

            Bitmap bitmap =
                    loadCorrectlyOrientedBitmap(uri);

            if (bitmap != null) {

                selectedImage.setImageBitmap(
                        bitmap
                );

            } else {

                selectedImage.setImageURI(uri);
            }

        } catch (Exception e) {

            selectedImage.setImageURI(uri);
        }


        // Show image

        selectedImage.setVisibility(
                View.VISIBLE
        );


        // Show Predict button

        predictButton.setVisibility(
                View.VISIBLE
        );

        predictButton.setEnabled(true);
    }


    // ============================================================
    // Switch Camera
    // ============================================================

    private void switchCamera() {

        if (
                cameraSelector.equals(
                        CameraSelector.DEFAULT_FRONT_CAMERA
                )
        ) {

            cameraSelector =
                    CameraSelector.DEFAULT_BACK_CAMERA;

        } else {

            cameraSelector =
                    CameraSelector.DEFAULT_FRONT_CAMERA;
        }


        openCamera();
    }


    // ============================================================
    // Open Camera
    // ============================================================

    private void openCamera() {

        ListenableFuture<ProcessCameraProvider>
                cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);


        cameraProviderFuture.addListener(
                () -> {

                    try {

                        ProcessCameraProvider
                                cameraProvider =
                                cameraProviderFuture.get();


                        // ====================================================
                        // Preview
                        // ====================================================

                        Preview preview =
                                new Preview.Builder()
                                        .build();


                        preview.setSurfaceProvider(
                                cameraPreview
                                        .getSurfaceProvider()
                        );


                        // ====================================================
                        // Image Capture
                        // ====================================================

                        imageCapture =
                                new ImageCapture.Builder()
                                        .setCaptureMode(
                                                ImageCapture
                                                        .CAPTURE_MODE_MINIMIZE_LATENCY
                                        )
                                        .setTargetRotation(
                                                cameraPreview
                                                        .getDisplay()
                                                        .getRotation()
                                        )
                                        .build();


                        // ====================================================
                        // Selected Camera
                        // ====================================================

                        CameraSelector selectedCamera =
                                cameraSelector;


                        // ====================================================
                        // Check Camera
                        // ====================================================

                        if (
                                !cameraProvider.hasCamera(
                                        selectedCamera
                                )
                        ) {

                            if (
                                    cameraProvider.hasCamera(
                                            CameraSelector
                                                    .DEFAULT_BACK_CAMERA
                                    )
                            ) {

                                cameraSelector =
                                        CameraSelector
                                                .DEFAULT_BACK_CAMERA;

                            } else if (
                                    cameraProvider.hasCamera(
                                            CameraSelector
                                                    .DEFAULT_FRONT_CAMERA
                                    )
                            ) {

                                cameraSelector =
                                        CameraSelector
                                                .DEFAULT_FRONT_CAMERA;

                            } else {

                                throw new Exception(
                                        "No camera found."
                                );
                            }


                            selectedCamera =
                                    cameraSelector;
                        }


                        // ====================================================
                        // Unbind Previous Camera
                        // ====================================================

                        cameraProvider.unbindAll();


                        // ====================================================
                        // Bind Camera
                        // ====================================================

                        cameraProvider.bindToLifecycle(
                                this,
                                selectedCamera,
                                preview,
                                imageCapture
                        );


                        cameraButton.setEnabled(true);


                    } catch (Exception e) {

                        hideCameraUI();

                        cameraButton.setEnabled(true);

                        Toast.makeText(
                                MainActivity.this,
                                "Camera error: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }

                },
                ContextCompat.getMainExecutor(this)
        );
    }


    // ============================================================
    // Take Photo
    // ============================================================

    private void takePhoto() {

        if (imageCapture == null) {

            Toast.makeText(
                    this,
                    "Camera is not ready.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        captureButton.setEnabled(false);


        // ========================================================
        // File Name
        // ========================================================

        String fileName =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.US
                ).format(
                        System.currentTimeMillis()
                );


        File rawPhotoFile =
                new File(
                        getCacheDir(),
                        "RAW_ASD_" +
                                fileName +
                                ".jpg"
                );


        // ========================================================
        // Output
        // ========================================================

        ImageCapture.OutputFileOptions
                outputOptions =
                new ImageCapture
                        .OutputFileOptions
                        .Builder(
                        rawPhotoFile
                )
                        .build();


        // ========================================================
        // Capture
        // ========================================================

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(
                            ImageCapture.OutputFileResults
                                    outputFileResults) {

                        try {

                            // =================================================
                            // Normalize orientation physically
                            // =================================================

                            File normalizedFile =
                                    normalizeCameraImage(
                                            rawPhotoFile
                                    );


                            // =================================================
                            // Delete RAW file
                            // =================================================

                            if (
                                    !rawPhotoFile
                                            .equals(normalizedFile)
                            ) {

                                rawPhotoFile.delete();
                            }


                            // =================================================
                            // URI of normalized image
                            // =================================================

                            selectedImageUri =
                                    Uri.fromFile(
                                            normalizedFile
                                    );


                            // =================================================
                            // IMPORTANT:
                            // Show actual captured image
                            // on Home Page
                            // =================================================

                            showSelectedImage(
                                    selectedImageUri
                            );


                            // =================================================
                            // Hide Camera
                            // =================================================

                            hideCameraUI();


                            // =================================================
                            // Enable buttons
                            // =================================================

                            cameraButton.setEnabled(true);

                            captureButton.setEnabled(true);


                            // =================================================
                            // Status
                            // =================================================

                            resultText.setText(
                                    "Photo captured.\nPress Predict."
                            );


                            Toast.makeText(
                                    MainActivity.this,
                                    "Photo captured successfully.",
                                    Toast.LENGTH_SHORT
                            ).show();


                        } catch (Exception e) {

                            captureButton.setEnabled(true);

                            cameraButton.setEnabled(true);

                            Toast.makeText(
                                    MainActivity.this,
                                    "Image processing failed: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }


                    @Override
                    public void onError(
                            ImageCaptureException exception) {

                        captureButton.setEnabled(true);

                        cameraButton.setEnabled(true);


                        Toast.makeText(
                                MainActivity.this,
                                "Capture failed: "
                                        + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }

                }
        );
    }


    // ============================================================
    // Normalize Camera Image
    //
    // This is the important fix for the rotation problem.
    //
    // It reads EXIF orientation, physically rotates the bitmap,
    // then saves a NEW normal JPEG.
    //
    // Therefore backend + ResultActivity receive a normal image.
    // ============================================================

    private File normalizeCameraImage(
            File inputFile
    ) throws IOException {

        Bitmap bitmap =
                BitmapFactory.decodeFile(
                        inputFile.getAbsolutePath()
                );


        if (bitmap == null) {

            throw new IOException(
                    "Unable to decode captured image."
            );
        }


        // ========================================================
        // Read EXIF
        // ========================================================

        int orientation =
                ExifInterface.ORIENTATION_NORMAL;


        try {

            ExifInterface exif =
                    new ExifInterface(
                            inputFile.getAbsolutePath()
                    );


            orientation =
                    exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface
                                    .ORIENTATION_NORMAL
                    );

        } catch (Exception ignored) {
        }


        // ========================================================
        // Transformation
        // ========================================================

        Matrix matrix =
                new Matrix();


        switch (orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:

                matrix.postRotate(90);

                break;


            case ExifInterface.ORIENTATION_ROTATE_180:

                matrix.postRotate(180);

                break;


            case ExifInterface.ORIENTATION_ROTATE_270:

                matrix.postRotate(270);

                break;


            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:

                matrix.preScale(-1, 1);

                break;


            case ExifInterface.ORIENTATION_FLIP_VERTICAL:

                matrix.preScale(1, -1);

                break;


            case ExifInterface.ORIENTATION_TRANSPOSE:

                matrix.postRotate(90);

                matrix.postScale(-1, 1);

                break;


            case ExifInterface.ORIENTATION_TRANSVERSE:

                matrix.postRotate(270);

                matrix.postScale(-1, 1);

                break;


            default:

                break;
        }


        Bitmap normalizedBitmap;


        if (!matrix.isIdentity()) {

            normalizedBitmap =
                    Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            bitmap.getWidth(),
                            bitmap.getHeight(),
                            matrix,
                            true
                    );

        } else {

            normalizedBitmap =
                    bitmap;
        }


        // ========================================================
        // Create normalized file
        // ========================================================

        String normalizedName =
                inputFile.getName()
                        .replace(
                                "RAW_ASD_",
                                "ASD_"
                        );


        File outputFile =
                new File(
                        getCacheDir(),
                        normalizedName
                );


        FileOutputStream outputStream =
                new FileOutputStream(
                        outputFile
                );


        normalizedBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                95,
                outputStream
        );


        outputStream.flush();

        outputStream.close();


        // ========================================================
        // Recycle
        // ========================================================

        if (
                normalizedBitmap != bitmap
        ) {

            bitmap.recycle();
        }


        normalizedBitmap.recycle();


        // ========================================================
        // IMPORTANT:
        // The new JPEG has normal pixel orientation.
        // No EXIF rotation is needed.
        // ========================================================

        return outputFile;
    }


    // ============================================================
    // Load Bitmap With Correct Orientation
    // For Gallery Images
    // ============================================================

    private Bitmap loadCorrectlyOrientedBitmap(
            Uri uri
    ) throws IOException {

        InputStream inputStream =
                getContentResolver()
                        .openInputStream(uri);


        if (inputStream == null) {

            return null;
        }


        Bitmap bitmap =
                BitmapFactory.decodeStream(
                        inputStream
                );


        inputStream.close();


        if (bitmap == null) {

            return null;
        }


        // ========================================================
        // Read EXIF
        // ========================================================

        int orientation =
                ExifInterface.ORIENTATION_NORMAL;


        try {

            InputStream exifStream =
                    getContentResolver()
                            .openInputStream(uri);


            if (exifStream != null) {

                ExifInterface exif =
                        new ExifInterface(
                                exifStream
                        );


                orientation =
                        exif.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface
                                        .ORIENTATION_NORMAL
                        );


                exifStream.close();
            }

        } catch (Exception ignored) {
        }


        // ========================================================
        // Matrix
        // ========================================================

        Matrix matrix =
                new Matrix();


        switch (orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:

                matrix.postRotate(90);

                break;


            case ExifInterface.ORIENTATION_ROTATE_180:

                matrix.postRotate(180);

                break;


            case ExifInterface.ORIENTATION_ROTATE_270:

                matrix.postRotate(270);

                break;


            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:

                matrix.preScale(-1, 1);

                break;


            case ExifInterface.ORIENTATION_FLIP_VERTICAL:

                matrix.preScale(1, -1);

                break;


            case ExifInterface.ORIENTATION_TRANSPOSE:

                matrix.postRotate(90);

                matrix.postScale(-1, 1);

                break;


            case ExifInterface.ORIENTATION_TRANSVERSE:

                matrix.postRotate(270);

                matrix.postScale(-1, 1);

                break;


            default:

                break;
        }


        // ========================================================
        // Apply
        // ========================================================

        if (!matrix.isIdentity()) {

            Bitmap rotatedBitmap =
                    Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            bitmap.getWidth(),
                            bitmap.getHeight(),
                            matrix,
                            true
                    );


            if (
                    rotatedBitmap != bitmap
            ) {

                bitmap.recycle();
            }


            return rotatedBitmap;
        }


        return bitmap;
    }


    // ============================================================
    // Upload Image
    // ============================================================

    private void uploadImage(
            Uri imageUri
    ) {

        progressBar.setVisibility(
                View.VISIBLE
        );


        resultText.setText(
                "Analyzing image..."
        );


        predictButton.setEnabled(false);


        try {

            File imageFile =
                    createTempFileFromUri(
                            imageUri
                    );


            // ====================================================
            // Request Body
            // ====================================================

            RequestBody requestFile =
                    RequestBody.create(
                            MediaType.parse(
                                    "image/jpeg"
                            ),
                            imageFile
                    );


            // ====================================================
            // Multipart
            // ====================================================

            MultipartBody.Part body =
                    MultipartBody.Part
                            .createFormData(
                                    "file",
                                    imageFile.getName(),
                                    requestFile
                            );


            // ====================================================
            // API
            // ====================================================

            ApiService apiService =
                    RetrofitClient
                            .getApiService();


            Call<ResponseBody> call =
                    apiService.predict(body);


            // ====================================================
            // API Call
            // ====================================================

            call.enqueue(
                    new Callback<ResponseBody>() {

                        @Override
                        public void onResponse(
                                Call<ResponseBody> call,
                                Response<ResponseBody> response
                        ) {

                            progressBar.setVisibility(
                                    View.GONE
                            );


                            if (response.isSuccessful()) {

                                try {

                                    if (
                                            response.body() == null
                                    ) {

                                        predictButton
                                                .setEnabled(true);

                                        resultText.setText(
                                                "Empty server response."
                                        );

                                        return;
                                    }


                                    String result =
                                            response.body()
                                                    .string();


                                    // ======================================
                                    // JSON
                                    // ======================================

                                    JSONObject json =
                                            new JSONObject(
                                                    result
                                            );


                                    String prediction =
                                            json.getString(
                                                    "prediction"
                                            );


                                    double confidence =
                                            json.getDouble(
                                                    "confidence"
                                            );


                                    double asdProbability =
                                            json.getDouble(
                                                    "asd_probability"
                                            );


                                    double nonAsdProbability =
                                            json.getDouble(
                                                    "non_asd_probability"
                                            );


                                    String gradcamOverlay =
                                            json.optString(
                                                    "gradcam_overlay",
                                                    ""
                                            );


                                    String gradcamHeatmap =
                                            json.optString(
                                                    "gradcam_heatmap",
                                                    ""
                                            );


                                    // ======================================
                                    // Percentages
                                    // ======================================

                                    double confidencePercent =
                                            confidence * 100;


                                    double asdPercent =
                                            asdProbability * 100;


                                    double nonAsdPercent =
                                            nonAsdProbability * 100;


                                    // ======================================
                                    // Result Activity
                                    // ======================================

                                    Intent intent =
                                            new Intent(
                                                    MainActivity.this,
                                                    ResultActivity.class
                                            );


                                    // Original image

                                    intent.putExtra(
                                            "original_image_uri",
                                            imageUri.toString()
                                    );


                                    // Prediction

                                    intent.putExtra(
                                            "prediction",
                                            prediction
                                    );


                                    // Confidence

                                    intent.putExtra(
                                            "confidence",
                                            String.format(
                                                    Locale.US,
                                                    "%.2f",
                                                    confidencePercent
                                            )
                                    );


                                    // ASD Probability

                                    intent.putExtra(
                                            "asd_probability",
                                            String.format(
                                                    Locale.US,
                                                    "%.2f",
                                                    asdPercent
                                            )
                                    );


                                    // Non-ASD Probability

                                    intent.putExtra(
                                            "non_asd_probability",
                                            String.format(
                                                    Locale.US,
                                                    "%.2f",
                                                    nonAsdPercent
                                            )
                                    );


                                    // Grad-CAM Overlay

                                    intent.putExtra(
                                            "gradcam_overlay",
                                            gradcamOverlay
                                    );


                                    // Grad-CAM Heatmap

                                    intent.putExtra(
                                            "gradcam_heatmap",
                                            gradcamHeatmap
                                    );


                                    // ======================================
                                    // Reset after returning
                                    // ======================================

                                    resetWhenResume = true;


                                    // ======================================
                                    // Open Result
                                    // ======================================

                                    startActivity(
                                            intent
                                    );


                                } catch (Exception e) {

                                    predictButton
                                            .setEnabled(true);

                                    resultText.setText(
                                            "Error processing result:\n"
                                                    + e.getMessage()
                                    );
                                }


                            } else {

                                predictButton
                                        .setEnabled(true);


                                resultText.setText(
                                        "Server Error: "
                                                + response.code()
                                );
                            }
                        }


                        // ==================================================
                        // Connection Failure
                        // ==================================================

                        @Override
                        public void onFailure(
                                Call<ResponseBody> call,
                                Throwable t
                        ) {

                            progressBar.setVisibility(
                                    View.GONE
                            );


                            predictButton
                                    .setEnabled(true);


                            resultText.setText(
                                    "Connection Failed:\n"
                                            + t.getMessage()
                            );
                        }

                    }
            );


        } catch (Exception e) {

            progressBar.setVisibility(
                    View.GONE
            );


            predictButton.setEnabled(true);


            resultText.setText(
                    "Image Error:\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // Convert URI → File
    // ============================================================

    private File createTempFileFromUri(
            Uri uri
    ) throws IOException {

        // ========================================================
        // Camera File
        // ========================================================

        if ("file".equals(uri.getScheme())) {

            File cameraFile =
                    new File(
                            uri.getPath()
                    );


            if (!cameraFile.exists()) {

                throw new IOException(
                        "Camera image file not found."
                );
            }


            return cameraFile;
        }


        // ========================================================
        // Gallery File
        // ========================================================

        ContentResolver resolver =
                getContentResolver();


        String fileName =
                getFileName(uri);


        File file =
                new File(
                        getCacheDir(),
                        "UPLOAD_" + fileName
                );


        try (

                InputStream inputStream =
                        resolver.openInputStream(uri);

                FileOutputStream outputStream =
                        new FileOutputStream(file)

        ) {

            if (inputStream == null) {

                throw new IOException(
                        "Unable to open image."
                );
            }


            byte[] buffer =
                    new byte[8192];


            int bytesRead;


            while (
                    (bytesRead =
                            inputStream.read(buffer))
                            != -1
            ) {

                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }
        }


        return file;
    }


    // ============================================================
    // Get File Name
    // ============================================================

    private String getFileName(
            Uri uri
    ) {

        String result = null;


        if ("content".equals(uri.getScheme())) {

            Cursor cursor =
                    getContentResolver().query(
                            uri,
                            null,
                            null,
                            null,
                            null
                    );


            try {

                if (
                        cursor != null
                                && cursor.moveToFirst()
                ) {

                    int index =
                            cursor.getColumnIndex(
                                    OpenableColumns
                                            .DISPLAY_NAME
                            );


                    if (index >= 0) {

                        result =
                                cursor.getString(
                                        index
                                );
                    }
                }

            } finally {

                if (cursor != null) {

                    cursor.close();
                }
            }
        }


        if (result == null) {

            result =
                    uri.getLastPathSegment();
        }


        if (result == null) {

            result =
                    "image.jpg";
        }


        return result;
    }


    // ============================================================
    // onResume
    // ============================================================

    @Override
    protected void onResume() {

        super.onResume();


        if (resetWhenResume) {

            resetHomePage();

            resetWhenResume = false;
        }
    }


    // ============================================================
    // onDestroy
    // ============================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();


        if (cameraExecutor != null) {

            cameraExecutor.shutdown();
        }
    }
}