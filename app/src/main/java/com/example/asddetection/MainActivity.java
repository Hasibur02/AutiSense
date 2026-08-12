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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
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

    private LinearLayout selectedImageContainer;

    private TextView resultText;
    private TextView instructionText;

    private ProgressBar progressBar;

    private FrameLayout cameraContainer;

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

    private ProcessCameraProvider cameraProvider;

    private ImageCapture imageCapture;

    private ExecutorService cameraExecutor;


    // ============================================================
    // Camera State
    // ============================================================

    private boolean isCameraOpen = false;

    private boolean isCapturing = false;


    // ============================================================
    // Camera Selector
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

                        if (uri == null) {
                            return;
                        }

                        selectedImageUri = uri;

                        hideCamera();

                        showGalleryImage(uri);

                        resultText.setText(
                                "Image selected.\nPress Predict."
                        );
                    }
            );


    // ============================================================
    // Camera Permission Launcher
    // ============================================================

    private final ActivityResultLauncher<String>
            cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {

                        if (granted) {

                            startCamera();

                        } else {

                            hideCamera();

                            cameraButton.setEnabled(true);

                            Toast.makeText(
                                    MainActivity.this,
                                    "Camera permission is required.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );


    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        // ========================================================
        // Find Views
        // ========================================================

        cameraContainer =
                findViewById(R.id.cameraContainer);

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

        selectedImageContainer =
                findViewById(R.id.selectedImageContainer);

        resultText =
                findViewById(R.id.resultText);

        instructionText =
                findViewById(R.id.instructionText);

        progressBar =
                findViewById(R.id.progressBar);


        // ========================================================
        // Camera Executor
        // ========================================================

        cameraExecutor =
                Executors.newSingleThreadExecutor();


        // ========================================================
        // Camera Preview Configuration
        // ========================================================

        cameraPreview.setImplementationMode(
                PreviewView.ImplementationMode.COMPATIBLE
        );

        cameraPreview.setScaleType(
                PreviewView.ScaleType.FILL_CENTER
        );


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

            if (isCameraOpen) {

                return;
            }

            requestCameraPermissionAndStart();

        });


        // ========================================================
        // Switch Camera
        // ========================================================

        switchCameraButton.setOnClickListener(v -> {

            if (!isCameraOpen) {

                Toast.makeText(
                        MainActivity.this,
                        "Camera is not open.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            switchCamera();

        });


        // ========================================================
        // Capture Button
        // ========================================================

        captureButton.setOnClickListener(v -> {

            if (!isCameraOpen) {

                Toast.makeText(
                        MainActivity.this,
                        "Please open the camera first.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

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
    // REQUEST CAMERA PERMISSION
    // ============================================================

    private void requestCameraPermissionAndStart() {

        if (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                )
                        == PackageManager.PERMISSION_GRANTED
        ) {

            startCamera();

        } else {

            cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
            );
        }
    }


    // ============================================================
    // START CAMERA
    // ============================================================

    private void startCamera() {

        cameraButton.setEnabled(false);

        cameraContainer.setVisibility(
                View.VISIBLE
        );

        switchCameraButton.setVisibility(
                View.VISIBLE
        );

        captureButton.setVisibility(
                View.VISIBLE
        );

        switchCameraButton.setEnabled(false);

        captureButton.setEnabled(false);

        instructionText.setText(
                "Camera is starting..."
        );

        resultText.setText(
                "Starting camera..."
        );


        ListenableFuture<ProcessCameraProvider>
                cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);


        cameraProviderFuture.addListener(
                () -> {

                    try {

                        cameraProvider =
                                cameraProviderFuture.get();


                        bindCamera();


                    } catch (Exception e) {

                        isCameraOpen = false;

                        hideCamera();

                        cameraButton.setEnabled(true);

                        instructionText.setText(
                                "Upload or capture an image to begin."
                        );

                        resultText.setText(
                                "Camera could not be started."
                        );

                        Toast.makeText(
                                MainActivity.this,
                                "Camera start failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }

                },
                ContextCompat.getMainExecutor(this)
        );
    }


    // ============================================================
    // BIND CAMERA
    // ============================================================

    private void bindCamera() {

        if (cameraProvider == null) {

            Toast.makeText(
                    this,
                    "Camera provider is not ready.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        try {

            // ----------------------------------------------------
            // Unbind previous camera
            // ----------------------------------------------------

            cameraProvider.unbindAll();


            // ----------------------------------------------------
            // Check requested camera
            // ----------------------------------------------------

            CameraSelector selectedCamera =
                    cameraSelector;


            if (
                    !cameraProvider.hasCamera(
                            selectedCamera
                    )
            ) {

                // Try back camera

                if (
                        cameraProvider.hasCamera(
                                CameraSelector.DEFAULT_BACK_CAMERA
                        )
                ) {

                    cameraSelector =
                            CameraSelector.DEFAULT_BACK_CAMERA;

                    selectedCamera =
                            cameraSelector;

                }

                // Try front camera

                else if (
                        cameraProvider.hasCamera(
                                CameraSelector.DEFAULT_FRONT_CAMERA
                        )
                ) {

                    cameraSelector =
                            CameraSelector.DEFAULT_FRONT_CAMERA;

                    selectedCamera =
                            cameraSelector;

                }

                else {

                    throw new Exception(
                            "No usable camera found."
                    );
                }
            }


            // ----------------------------------------------------
            // Preview
            // ----------------------------------------------------

            Preview preview =
                    new Preview.Builder()
                            .build();


            preview.setSurfaceProvider(
                    cameraPreview.getSurfaceProvider()
            );


            // ----------------------------------------------------
            // Image Capture
            // ----------------------------------------------------

            imageCapture =
                    new ImageCapture.Builder()
                            .setCaptureMode(
                                    ImageCapture
                                            .CAPTURE_MODE_MINIMIZE_LATENCY
                            )
                            .build();


            // ----------------------------------------------------
            // Bind
            // ----------------------------------------------------

            cameraProvider.bindToLifecycle(
                    this,
                    selectedCamera,
                    preview,
                    imageCapture
            );


            // ----------------------------------------------------
            // Camera successfully opened
            // ----------------------------------------------------

            isCameraOpen = true;

            switchCameraButton.setEnabled(true);

            captureButton.setEnabled(true);

            cameraButton.setEnabled(true);

            instructionText.setText(
                    "Position your face inside the frame."
            );

            resultText.setText(
                    "Camera ready. Capture your photo."
            );


        } catch (Exception e) {

            isCameraOpen = false;

            imageCapture = null;

            hideCamera();

            cameraButton.setEnabled(true);

            Toast.makeText(
                    MainActivity.this,
                    "Camera binding failed:\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // ============================================================
    // SWITCH CAMERA
    // ============================================================

    private void switchCamera() {

        if (
                cameraProvider == null
                        || !isCameraOpen
        ) {

            return;
        }


        switchCameraButton.setEnabled(false);

        captureButton.setEnabled(false);


        try {

            // ----------------------------------------------------
            // Stop current camera
            // ----------------------------------------------------

            cameraProvider.unbindAll();

            imageCapture = null;

            isCameraOpen = false;


            // ----------------------------------------------------
            // Change camera
            // ----------------------------------------------------

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


            instructionText.setText(
                    "Switching camera..."
            );


            // ----------------------------------------------------
            // Re-bind
            // ----------------------------------------------------

            cameraPreview.postDelayed(
                    () -> {

                        bindCamera();

                    },
                    150
            );


        } catch (Exception e) {

            switchCameraButton.setEnabled(true);

            captureButton.setEnabled(true);

            Toast.makeText(
                    MainActivity.this,
                    "Unable to switch camera:\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // ============================================================
    // TAKE PHOTO
    // ============================================================

    private void takePhoto() {

        if (
                imageCapture == null
                        || !isCameraOpen
        ) {

            Toast.makeText(
                    this,
                    "Camera is not ready.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (isCapturing) {

            return;
        }


        isCapturing = true;

        captureButton.setEnabled(false);

        switchCameraButton.setEnabled(false);


        // --------------------------------------------------------
        // File name
        // --------------------------------------------------------

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
                        "RAW_ASD_"
                                + fileName
                                + ".jpg"
                );


        // --------------------------------------------------------
        // Output
        // --------------------------------------------------------

        ImageCapture.OutputFileOptions
                outputOptions =
                new ImageCapture
                        .OutputFileOptions
                        .Builder(
                        rawPhotoFile
                )
                        .build();


        // --------------------------------------------------------
        // Capture
        // --------------------------------------------------------

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(
                            @NonNull
                            ImageCapture.OutputFileResults
                                    outputFileResults
                    ) {

                        isCapturing = false;


                        try {

                            File normalizedFile =
                                    normalizeCameraImage(
                                            rawPhotoFile
                                    );


                            if (
                                    !rawPhotoFile.equals(
                                            normalizedFile
                                    )
                            ) {

                                rawPhotoFile.delete();
                            }


                            selectedImageUri =
                                    Uri.fromFile(
                                            normalizedFile
                                    );


                            // ------------------------------------------------
                            // Show captured image
                            // ------------------------------------------------

                            showCameraImage(
                                    normalizedFile
                            );


                            // ------------------------------------------------
                            // Close camera
                            // ------------------------------------------------

                            hideCamera();


                            cameraButton.setEnabled(true);

                            captureButton.setEnabled(true);

                            switchCameraButton.setEnabled(true);


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

                            switchCameraButton.setEnabled(true);

                            cameraButton.setEnabled(true);

                            Toast.makeText(
                                    MainActivity.this,
                                    "Image processing failed:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }


                    @Override
                    public void onError(
                            @NonNull
                            ImageCaptureException exception
                    ) {

                        isCapturing = false;

                        captureButton.setEnabled(true);

                        switchCameraButton.setEnabled(true);

                        cameraButton.setEnabled(true);


                        Toast.makeText(
                                MainActivity.this,
                                "Capture failed:\n"
                                        + exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }


    // ============================================================
    // HIDE CAMERA
    // ============================================================

    private void hideCamera() {

        isCameraOpen = false;

        isCapturing = false;

        imageCapture = null;


        if (cameraProvider != null) {

            try {

                cameraProvider.unbindAll();

            } catch (Exception ignored) {
            }
        }


        cameraContainer.setVisibility(
                View.GONE
        );

        switchCameraButton.setVisibility(
                View.GONE
        );

        captureButton.setVisibility(
                View.GONE
        );


        switchCameraButton.setEnabled(
                false
        );

        captureButton.setEnabled(
                false
        );
    }


    // ============================================================
    // RESET HOME PAGE
    // ============================================================

    private void resetHomePage() {

        selectedImageUri = null;


        // --------------------------------------------------------
        // Clear selected image
        // --------------------------------------------------------

        selectedImage.setImageDrawable(null);

        selectedImage.setVisibility(
                View.GONE
        );


        selectedImageContainer.setVisibility(
                View.GONE
        );


        // --------------------------------------------------------
        // Hide camera
        // --------------------------------------------------------

        hideCamera();


        // --------------------------------------------------------
        // Buttons
        // --------------------------------------------------------

        cameraButton.setEnabled(true);

        galleryButton.setEnabled(true);

        predictButton.setEnabled(true);


        predictButton.setVisibility(
                View.GONE
        );


        // --------------------------------------------------------
        // Default camera
        // --------------------------------------------------------

        cameraSelector =
                CameraSelector.DEFAULT_FRONT_CAMERA;


        // --------------------------------------------------------
        // Status
        // --------------------------------------------------------

        instructionText.setText(
                "Select an image or capture a new one to begin."
        );

        resultText.setText(
                "Ready for image analysis."
        );


        // --------------------------------------------------------
        // Progress
        // --------------------------------------------------------

        progressBar.setVisibility(
                View.GONE
        );
    }


    // ============================================================
    // SHOW GALLERY IMAGE
    // ============================================================

    private void showGalleryImage(Uri uri) {

        try {

            Bitmap bitmap =
                    loadCorrectlyOrientedBitmap(uri);


            if (bitmap == null) {

                Toast.makeText(
                        this,
                        "Unable to display selected image.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            selectedImage.setImageBitmap(bitmap);

            selectedImage.setVisibility(
                    View.VISIBLE
            );


            selectedImageContainer.setVisibility(
                    View.VISIBLE
            );


            predictButton.setVisibility(
                    View.VISIBLE
            );

            predictButton.setEnabled(true);


            selectedImage.requestLayout();

            selectedImage.invalidate();


        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Image display error:\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // ============================================================
    // SHOW CAMERA IMAGE
    // ============================================================

    private void showCameraImage(File imageFile) {

        try {

            if (
                    imageFile == null
                            || !imageFile.exists()
            ) {

                Toast.makeText(
                        this,
                        "Captured image file not found.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            Bitmap bitmap =
                    BitmapFactory.decodeFile(
                            imageFile.getAbsolutePath()
                    );


            if (bitmap == null) {

                Toast.makeText(
                        this,
                        "Unable to display captured image.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            selectedImage.setImageBitmap(bitmap);

            selectedImage.setVisibility(
                    View.VISIBLE
            );


            selectedImageContainer.setVisibility(
                    View.VISIBLE
            );


            predictButton.setVisibility(
                    View.VISIBLE
            );

            predictButton.setEnabled(true);


            selectedImage.requestLayout();

            selectedImage.invalidate();


        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Camera image display error:\n"
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    // ============================================================
    // NORMALIZE CAMERA IMAGE
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
                            ExifInterface.ORIENTATION_NORMAL
                    );

        } catch (Exception ignored) {
        }


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


        if (
                normalizedBitmap != bitmap
        ) {

            bitmap.recycle();
        }


        return outputFile;
    }


    // ============================================================
    // LOAD GALLERY IMAGE
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
                                ExifInterface.ORIENTATION_NORMAL
                        );


                exifStream.close();
            }

        } catch (Exception ignored) {
        }


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


            if (rotatedBitmap != bitmap) {

                bitmap.recycle();
            }


            return rotatedBitmap;
        }


        return bitmap;
    }


    // ============================================================
    // UPLOAD IMAGE
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


            RequestBody requestFile =
                    RequestBody.create(
                            MediaType.parse(
                                    "image/jpeg"
                            ),
                            imageFile
                    );


            MultipartBody.Part body =
                    MultipartBody.Part
                            .createFormData(
                                    "file",
                                    imageFile.getName(),
                                    requestFile
                            );


            ApiService apiService =
                    RetrofitClient
                            .getApiService();


            Call<ResponseBody> call =
                    apiService.predict(body);


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


                                    double confidencePercent =
                                            confidence * 100;


                                    double asdPercent =
                                            asdProbability * 100;


                                    double nonAsdPercent =
                                            nonAsdProbability * 100;


                                    Intent intent =
                                            new Intent(
                                                    MainActivity.this,
                                                    ResultActivity.class
                                            );


                                    intent.putExtra(
                                            "original_image_uri",
                                            imageUri.toString()
                                    );


                                    intent.putExtra(
                                            "prediction",
                                            prediction
                                    );


                                    intent.putExtra(
                                            "confidence",
                                            String.format(
                                                    Locale.US,
                                                    "%.2f",
                                                    confidencePercent
                                            )
                                    );


                                    intent.putExtra(
                                            "asd_probability",
                                            String.format(
                                                    Locale.US,
                                                    "%.2f",
                                                    asdPercent
                                            )
                                    );


                                    intent.putExtra(
                                            "non_asd_probability",
                                            String.format(
                                                    Locale.US,
                                                    "%.2f",
                                                    nonAsdPercent
                                            )
                                    );


                                    intent.putExtra(
                                            "gradcam_overlay",
                                            gradcamOverlay
                                    );


                                    intent.putExtra(
                                            "gradcam_heatmap",
                                            gradcamHeatmap
                                    );


                                    resetWhenResume = true;


                                    startActivity(intent);


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
    // URI -> FILE
    // ============================================================

    private File createTempFileFromUri(
            Uri uri
    ) throws IOException {

        // --------------------------------------------------------
        // Camera file
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // Gallery file
        // --------------------------------------------------------

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
    // GET FILE NAME
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
    // ON RESUME
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
    // ON PAUSE
    // ============================================================

    @Override
    protected void onPause() {

        super.onPause();


        // Stop camera when activity goes to background

        if (cameraProvider != null) {

            try {

                cameraProvider.unbindAll();

            } catch (Exception ignored) {
            }

            isCameraOpen = false;

            imageCapture = null;
        }
    }


    // ============================================================
    // ON DESTROY
    // ============================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();


        if (cameraProvider != null) {

            try {

                cameraProvider.unbindAll();

            } catch (Exception ignored) {
            }
        }


        if (cameraExecutor != null) {

            cameraExecutor.shutdown();
        }
    }
}
