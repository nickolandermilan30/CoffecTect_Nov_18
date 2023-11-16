package com.example.coffeetectapp;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.coffeetectapp.ml.Dataset;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Capture extends AppCompatActivity {

    private ImageCapture imageCapture;
    private ImageView markerImageView;
    private RelativeLayout markerLayout;
    private TextView resultTextView;

    private float lastX, lastY;
    private boolean isResizing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        startCamera();

        Button captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        ImageButton customDialogButton = findViewById(R.id.customDialogButton);
        customDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomDialog();
            }
        });

        markerImageView = findViewById(R.id.markerImageView);
        markerLayout = findViewById(R.id.markerLayout);

        markerLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                handleTouch(event);
                return true;
            }
        });

        resultTextView = findViewById(R.id.resultTextView);
    }

    private void showCustomDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_dialog);

        TextView titleTextView = dialog.findViewById(R.id.dialogTitle);
        titleTextView.setText("Custom Dialog Title");

        TextView messageTextView = dialog.findViewById(R.id.dialogMessage);
        messageTextView.setText("Custom Dialog Message");

        Button okButton = dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void handleTouch(MotionEvent event) {
        float currX = event.getRawX();
        float currY = event.getRawY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = currX;
                lastY = currY;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isResizing) {
                    ViewGroup.LayoutParams layoutParams = markerImageView.getLayoutParams();
                    layoutParams.width = (int) (markerImageView.getWidth() + currX - lastX);
                    layoutParams.height = (int) (markerImageView.getHeight() + currY - lastY);
                    markerImageView.setLayoutParams(layoutParams);

                    lastX = currX;
                    lastY = currY;
                } else {
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) markerImageView.getLayoutParams();
                    layoutParams.leftMargin = (int) (layoutParams.leftMargin + currX - lastX);
                    layoutParams.topMargin = (int) (layoutParams.topMargin + currY - lastY);
                    markerImageView.setLayoutParams(layoutParams);

                    lastX = currX;
                    lastY = currY;
                }
                break;

            case MotionEvent.ACTION_UP:
                isResizing = false;
                break;
        }
    }

    public void onResizeClick(View view) {
        isResizing = !isResizing;
    }

    private void startCamera() {
        new Handler().postDelayed(() -> {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(((PreviewView) findViewById(R.id.viewFinder)).getSurfaceProvider());

                    imageCapture = new ImageCapture.Builder().build();

                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    cameraProvider.unbindAll();

                    Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, ContextCompat.getMainExecutor(this));
        }, 1000);
    }

    private void takePhoto() {
        if (imageCapture != null) {
            File photoFile = createPhotoFile();

            ImageCapture.OutputFileOptions outputFileOptions =
                    new ImageCapture.OutputFileOptions.Builder(photoFile).build();

            imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            Bitmap capturedImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                            Bitmap croppedImage = cropToMarkerBounds(capturedImage);
                            classifyImage(croppedImage, photoFile);
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            exception.printStackTrace();
                        }
                    });
        }
    }

    private Bitmap cropToMarkerBounds(Bitmap originalBitmap) {
        int[] location = new int[2];
        markerImageView.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + markerImageView.getWidth();
        int bottom = top + markerImageView.getHeight();

        left = Math.max(0, left);
        top = Math.max(0, top);
        right = Math.min(right, originalBitmap.getWidth());
        bottom = Math.min(bottom, originalBitmap.getHeight());

        // Calculate the width and height of the marker
        int markerWidth = markerImageView.getWidth();
        int markerHeight = markerImageView.getHeight();

        // Calculate the width and height of the cropped area
        int croppedWidth = right - left;
        int croppedHeight = bottom - top;

        // Calculate the starting point for cropping
        int startX = (markerWidth - croppedWidth) / 2;
        int startY = (markerHeight - croppedHeight) / 2;

        // Create a new Bitmap with the cropped area
        return Bitmap.createBitmap(originalBitmap, left, top, croppedWidth, croppedHeight);
    }

// ...

    private void classifyImage(Bitmap image, File photoFile) {
        try {
            Dataset model = Dataset.newInstance(this);

            Bitmap resizedImage = Bitmap.createScaledBitmap(image, 32, 32, true);

            ByteBuffer byteBuffer = convertBitmapToByteBuffer(resizedImage);

            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 32, 32, 3}, DataType.FLOAT32);
            inputFeature0.loadBuffer(byteBuffer);

            Dataset.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();

            int maxConfidenceIndex = getMaxConfidenceIndex(confidences);

            String[] classes = {
                    "Cercospora Critical",
                    "Cercospora Moderate",
                    "Cercospora Mild",
                    "Healthy Leaf",
                    "Leaf Miner Critical",
                    "Leaf Miner Mild",
                    "Leaf Miner Moderate",
                    "Leaf Rust Critical",
                    "Leaf Rust Mild",
                    "Leaf Rust Moderate",
                    "Phoma Critical",
                    "Phoma Mild",
                    "Phoma Moderate",
                    "Sooty Mold Critical",
                    "Sooty Mold Mild",
                    "Sooty Mold Moderate",
            };

            String topPrediction = classes[maxConfidenceIndex];

            resultTextView.setVisibility(View.VISIBLE);
            resultTextView.setText(topPrediction); // Ito ang nilagay ko
            saveResultAndImagePath(topPrediction, photoFile.getAbsolutePath());

            Intent intent = new Intent(Capture.this, Result_Activity2.class);
            intent.putExtra("result", topPrediction);
            intent.putExtra("imagePath", photoFile.getAbsolutePath());
            intent.putExtra("image", imageToByteArray(image));
            intent.putExtra("topPrediction", topPrediction);
            startActivity(intent);

            model.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private int getMaxConfidenceIndex(float[] array) {
        int maxIndex = 0;
        float maxConfidence = array[0];

        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxConfidence) {
                maxConfidence = array[i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    private byte[] imageToByteArray(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private File createPhotoFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(getPackageName() + "/images/");

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File photoFile = null;
        try {
            photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return photoFile;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int[] intValues = new int[32 * 32];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 32 * 32 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                int val = intValues[i * 32 + j];
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }

        return byteBuffer;
    }

    private void saveResultAndImagePath(String prediction, String imagePath) {
        Toast.makeText(this, "Prediction: " + prediction + "\nImage Path: " + imagePath, Toast.LENGTH_LONG).show();
    }
}
