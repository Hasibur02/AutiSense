package com.example.asddetection;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ResultActivity extends AppCompatActivity {

    // ============================================================
    // UI
    // ============================================================

    private TextView predictionText;
    private TextView confidenceText;

    private TextView asdProbabilityText;
    private TextView nonAsdProbabilityText;

    private ProgressBar asdProgressBar;
    private ProgressBar nonAsdProgressBar;

    private ImageView originalImage;
    private ImageView heatmapImage;
    private ImageView gradcamImage;

    private Button saveResultButton;
    private Button backButton;


    // ============================================================
    // Result Data
    // ============================================================

    private String prediction;
    private String confidence;
    private String asdProbability;
    private String nonAsdProbability;

    private String originalImageUri;
    private String gradcamOverlay;
    private String gradcamHeatmap;


    // ============================================================
    // onCreate
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_result);


        // ========================================================
        // Find Views
        // ========================================================

        predictionText =
                findViewById(R.id.predictionText);

        confidenceText =
                findViewById(R.id.confidenceText);

        asdProbabilityText =
                findViewById(R.id.asdProbabilityText);

        nonAsdProbabilityText =
                findViewById(R.id.nonAsdProbabilityText);

        asdProgressBar =
                findViewById(R.id.asdProgressBar);

        nonAsdProgressBar =
                findViewById(R.id.nonAsdProgressBar);

        originalImage =
                findViewById(R.id.originalImage);

        heatmapImage =
                findViewById(R.id.heatmapImage);

        gradcamImage =
                findViewById(R.id.gradcamImage);

        saveResultButton =
                findViewById(R.id.saveResultButton);

        backButton =
                findViewById(R.id.backButton);


        // ========================================================
        // Get Intent Data
        // ========================================================

        prediction =
                getIntent().getStringExtra(
                        "prediction"
                );

        confidence =
                getIntent().getStringExtra(
                        "confidence"
                );

        asdProbability =
                getIntent().getStringExtra(
                        "asd_probability"
                );

        nonAsdProbability =
                getIntent().getStringExtra(
                        "non_asd_probability"
                );

        originalImageUri =
                getIntent().getStringExtra(
                        "original_image_uri"
                );

        gradcamOverlay =
                getIntent().getStringExtra(
                        "gradcam_overlay"
                );

        gradcamHeatmap =
                getIntent().getStringExtra(
                        "gradcam_heatmap"
                );


        // ========================================================
        // Display Result
        // ========================================================

        displayResult();


        // ========================================================
        // Save Result
        // ========================================================

        saveResultButton.setOnClickListener(v -> {

            saveResultImage();

        });


        // ========================================================
        // Try Another Image
        // ========================================================

        backButton.setOnClickListener(v -> {

            finish();

        });
    }


    // ============================================================
    // Display Result
    // ============================================================

    private void displayResult() {

        // --------------------------------------------------------
        // Prediction
        // --------------------------------------------------------

        if (prediction != null) {

            predictionText.setText(
                    prediction
            );


            // ASD / Non-ASD visual indication

            if (
                    prediction
                            .toLowerCase()
                            .contains("asd")
                            &&
                            !prediction
                                    .toLowerCase()
                                    .contains("non")
            ) {

                predictionText.setTextColor(
                        Color.rgb(
                                220,
                                38,
                                38
                        )
                );

            } else {

                predictionText.setTextColor(
                        Color.rgb(
                                37,
                                99,
                                235
                        )
                );
            }
        }


        // --------------------------------------------------------
        // Confidence
        // --------------------------------------------------------

        if (confidence != null) {

            confidenceText.setText(
                    "Confidence: "
                            + confidence
                            + "%"
            );
        }


        // --------------------------------------------------------
        // ASD Probability
        // --------------------------------------------------------

        if (asdProbability != null) {

            asdProbabilityText.setText(
                    asdProbability
                            + "%"
            );

            setProgressSafely(
                    asdProgressBar,
                    asdProbability
            );
        }


        // --------------------------------------------------------
        // Non-ASD Probability
        // --------------------------------------------------------

        if (nonAsdProbability != null) {

            nonAsdProbabilityText.setText(
                    nonAsdProbability
                            + "%"
            );

            setProgressSafely(
                    nonAsdProgressBar,
                    nonAsdProbability
            );
        }


        // --------------------------------------------------------
        // Original Image
        // --------------------------------------------------------

        if (originalImageUri != null) {

            try {

                Uri uri =
                        Uri.parse(
                                originalImageUri
                        );

                originalImage.setImageURI(
                        uri
                );

            } catch (Exception e) {

                originalImage.setImageDrawable(
                        null
                );
            }
        }


        // --------------------------------------------------------
        // Grad-CAM Overlay
        // --------------------------------------------------------

        if (gradcamOverlay != null) {

            Bitmap bitmap =
                    decodeBase64Image(
                            gradcamOverlay
                    );

            if (bitmap != null) {

                gradcamImage.setImageBitmap(
                        bitmap
                );
            }
        }


        // --------------------------------------------------------
        // Grad-CAM Heatmap
        // --------------------------------------------------------

        if (gradcamHeatmap != null) {

            Bitmap bitmap =
                    decodeBase64Image(
                            gradcamHeatmap
                    );

            if (bitmap != null) {

                heatmapImage.setImageBitmap(
                        bitmap
                );
            }
        }
    }


    // ============================================================
    // ProgressBar Helper
    // ============================================================

    private void setProgressSafely(
            ProgressBar progressBar,
            String value
    ) {

        try {

            double number =
                    Double.parseDouble(
                            value
                    );

            int progress =
                    (int) Math.round(
                            number
                    );


            if (progress < 0) {

                progress = 0;
            }

            if (progress > 100) {

                progress = 100;
            }


            progressBar.setProgress(
                    progress
            );

        } catch (Exception e) {

            progressBar.setProgress(
                    0
            );
        }
    }


    // ============================================================
    // Decode Base64 Image
    // ============================================================

    private Bitmap decodeBase64Image(
            String base64
    ) {

        try {

            // ----------------------------------------------------
            // Remove data:image/... prefix
            // ----------------------------------------------------

            if (
                    base64.contains(",")
            ) {

                base64 =
                        base64.substring(
                                base64.indexOf(",") + 1
                        );
            }


            // ----------------------------------------------------
            // Decode
            // ----------------------------------------------------

            byte[] decodedBytes =
                    Base64.decode(
                            base64,
                            Base64.DEFAULT
                    );


            return BitmapFactory.decodeByteArray(
                    decodedBytes,
                    0,
                    decodedBytes.length
            );

        } catch (Exception e) {

            return null;
        }
    }


    // ============================================================
    // SAVE RESULT
    // ============================================================

    private void saveResultImage() {

        saveResultButton.setEnabled(
                false
        );


        try {

            // ====================================================
            // Create Result Bitmap
            // ====================================================

            Bitmap resultBitmap =
                    createResultBitmap();


            if (resultBitmap == null) {

                Toast.makeText(
                        this,
                        "Unable to create result image",
                        Toast.LENGTH_LONG
                ).show();

                saveResultButton.setEnabled(
                        true
                );

                return;
            }


            // ====================================================
            // File Name
            // ====================================================

            String fileName =
                    "ASD_Result_"
                            + System.currentTimeMillis()
                            + ".jpg";


            // ====================================================
            // Android 10+
            // ====================================================

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q) {

                ContentResolver resolver =
                        getContentResolver();


                ContentValues values =
                        new ContentValues();


                values.put(
                        MediaStore.Images.Media.DISPLAY_NAME,
                        fileName
                );


                values.put(
                        MediaStore.Images.Media.MIME_TYPE,
                        "image/jpeg"
                );


                values.put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES
                                + "/ASD Detection"
                );


                Uri imageUri =
                        resolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                values
                        );


                if (imageUri == null) {

                    throw new Exception(
                            "Could not create image file."
                    );
                }


                OutputStream outputStream =
                        resolver.openOutputStream(
                                imageUri
                        );


                if (outputStream == null) {

                    throw new Exception(
                            "Could not open output stream."
                    );
                }


                resultBitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        95,
                        outputStream
                );


                outputStream.flush();
                outputStream.close();


            } else {

                // =================================================
                // Android 9 and below
                // =================================================

                File picturesDirectory =
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES
                        );


                File asdDirectory =
                        new File(
                                picturesDirectory,
                                "ASD Detection"
                        );


                if (!asdDirectory.exists()) {

                    asdDirectory.mkdirs();
                }


                File file =
                        new File(
                                asdDirectory,
                                fileName
                        );


                FileOutputStream outputStream =
                        new FileOutputStream(
                                file
                        );


                resultBitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        95,
                        outputStream
                );


                outputStream.flush();
                outputStream.close();


                // Add to Gallery

                MediaStore.Images.Media.insertImage(
                        getContentResolver(),
                        file.getAbsolutePath(),
                        fileName,
                        "ASD Detection Result"
                );
            }


            // ====================================================
            // Success
            // ====================================================

            Toast.makeText(
                    this,
                    "Result saved to Pictures/ASD Detection",
                    Toast.LENGTH_LONG
            ).show();


        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Save failed: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

        } finally {

            saveResultButton.setEnabled(
                    true
            );
        }
    }


    // ============================================================
    // Create Result Bitmap
    // ============================================================

    private Bitmap createResultBitmap() {

        // --------------------------------------------------------
        // Get available images
        // --------------------------------------------------------

        Bitmap original =
                getBitmapFromImageView(
                        originalImage
                );

        Bitmap heatmap =
                getBitmapFromImageView(
                        heatmapImage
                );

        Bitmap overlay =
                getBitmapFromImageView(
                        gradcamImage
                );


        // --------------------------------------------------------
        // Minimum width
        // --------------------------------------------------------

        int width = 1000;

        if (original != null) {

            width =
                    Math.max(
                            width,
                            original.getWidth()
                    );
        }


        // --------------------------------------------------------
        // Heights
        // --------------------------------------------------------

        int imageHeight = 500;

        int sectionHeight =
                imageHeight + 100;


        int finalHeight =
                650;


        if (original != null) {

            finalHeight +=
                    sectionHeight;
        }


        if (
                heatmap != null
                        ||
                        overlay != null
        ) {

            finalHeight +=
                    sectionHeight;
        }


        // --------------------------------------------------------
        // Create Canvas
        // --------------------------------------------------------

        Bitmap result =
                Bitmap.createBitmap(
                        width,
                        finalHeight,
                        Bitmap.Config.ARGB_8888
                );


        Canvas canvas =
                new Canvas(
                        result
                );


        // --------------------------------------------------------
        // Background
        // --------------------------------------------------------

        canvas.drawColor(
                Color.WHITE
        );


        Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );


        // ========================================================
        // Header
        // ========================================================

        paint.setColor(
                Color.rgb(
                        23,
                        32,
                        51
                )
        );

        paint.setTextSize(
                42
        );

        paint.setFakeBoldText(
                true
        );


        canvas.drawText(
                "ASD Detection Result",
                40,
                60,
                paint
        );


        // ========================================================
        // Prediction
        // ========================================================

        paint.setTextSize(
                32
        );


        canvas.drawText(
                "Prediction: "
                        + safe(prediction),
                40,
                120,
                paint
        );


        paint.setTextSize(
                25
        );

        paint.setFakeBoldText(
                false
        );


        canvas.drawText(
                "Confidence: "
                        + safe(confidence)
                        + "%",
                40,
                165,
                paint
        );


        canvas.drawText(
                "ASD Probability: "
                        + safe(asdProbability)
                        + "%",
                40,
                205,
                paint
        );


        canvas.drawText(
                "Non-ASD Probability: "
                        + safe(nonAsdProbability)
                        + "%",
                40,
                245,
                paint
        );


        // ========================================================
        // Original Image
        // ========================================================

        int currentY =
                280;


        if (original != null) {

            paint.setFakeBoldText(
                    true
            );

            paint.setTextSize(
                    28
            );

            canvas.drawText(
                    "Original Image",
                    40,
                    currentY + 40,
                    paint
            );


            drawBitmapCentered(
                    canvas,
                    original,
                    40,
                    currentY + 60,
                    width - 80,
                    imageHeight
            );


            currentY +=
                    sectionHeight;
        }


        // ========================================================
        // Grad-CAM
        // ========================================================

        if (
                heatmap != null
                        ||
                        overlay != null
        ) {

            paint.setTextSize(
                    28
            );

            paint.setFakeBoldText(
                    true
            );


            canvas.drawText(
                    "Explainable AI",
                    40,
                    currentY + 40,
                    paint
            );


            int halfWidth =
                    (width - 100) / 2;


            // ----------------------------------------------------
            // Heatmap
            // ----------------------------------------------------

            if (heatmap != null) {

                paint.setTextSize(
                        22
                );


                canvas.drawText(
                        "Heatmap",
                        40,
                        currentY + 80,
                        paint
                );


                drawBitmapCentered(
                        canvas,
                        heatmap,
                        40,
                        currentY + 95,
                        halfWidth,
                        imageHeight
                );
            }


            // ----------------------------------------------------
            // Overlay
            // ----------------------------------------------------

            if (overlay != null) {

                paint.setTextSize(
                        22
                );


                canvas.drawText(
                        "Overlay",
                        halfWidth + 60,
                        currentY + 80,
                        paint
                );


                drawBitmapCentered(
                        canvas,
                        overlay,
                        halfWidth + 60,
                        currentY + 95,
                        halfWidth,
                        imageHeight
                );
            }
        }


        return result;
    }


    // ============================================================
    // Get Bitmap from ImageView
    // ============================================================

    private Bitmap getBitmapFromImageView(
            ImageView imageView
    ) {

        if (imageView == null) {

            return null;
        }


        if (imageView.getDrawable() == null) {

            return null;
        }


        imageView.setDrawingCacheEnabled(
                true
        );


        imageView.buildDrawingCache();


        Bitmap bitmap =
                Bitmap.createBitmap(
                        imageView.getDrawingCache()
                );


        imageView.setDrawingCacheEnabled(
                false
        );


        return bitmap;
    }


    // ============================================================
    // Draw Bitmap
    // ============================================================

    private void drawBitmapCentered(
            Canvas canvas,
            Bitmap bitmap,
            int left,
            int top,
            int maxWidth,
            int maxHeight
    ) {

        if (bitmap == null) {

            return;
        }


        float scaleX =
                (float) maxWidth
                        / bitmap.getWidth();


        float scaleY =
                (float) maxHeight
                        / bitmap.getHeight();


        float scale =
                Math.min(
                        scaleX,
                        scaleY
                );


        int newWidth =
                (int)
                        (bitmap.getWidth()
                                * scale);


        int newHeight =
                (int)
                        (bitmap.getHeight()
                                * scale);


        int x =
                left
                        + (maxWidth - newWidth)
                        / 2;


        int y =
                top
                        + (maxHeight - newHeight)
                        / 2;


        android.graphics.Rect src =
                new android.graphics.Rect(
                        0,
                        0,
                        bitmap.getWidth(),
                        bitmap.getHeight()
                );


        android.graphics.Rect dst =
                new android.graphics.Rect(
                        x,
                        y,
                        x + newWidth,
                        y + newHeight
                );


        Paint paint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );


        canvas.drawBitmap(
                bitmap,
                src,
                dst,
                paint
        );
    }


    // ============================================================
    // Safe String
    // ============================================================

    private String safe(
            String value
    ) {

        if (value == null) {

            return "N/A";
        }

        return value;
    }
}
