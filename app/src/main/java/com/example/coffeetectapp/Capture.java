package com.example.coffeetectapp;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Capture extends AppCompatActivity implements TextureView.SurfaceTextureListener  {

    private Camera camera;
    private TextureView textureView;
    private Button captureButton;
    private Interpreter interpreter;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private static final int numClasses = 16;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        try {
            interpreter = new Interpreter(loadModelFile()); // Implement loadModelFile() method
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Check for camera permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                initializeCamera();
            }
        }

        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(this);

        captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capturePhoto();
            }
        });
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("your_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void initializeCamera() {
        try {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        try {
            if (camera != null) {
                camera.setPreviewTexture(surface);
                camera.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        // Handle surface size changes (if needed)
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        releaseCamera();
        return true;
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        // Handle texture updates (if needed)
    }

    private void capturePhoto() {
        if (camera != null) {
            try {
                camera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        // Save the image to a temporary file
                        String imagePath = saveImageLocally(data);

                        // Perform model inference on the captured image
                        try {
                            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                            ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);

                            // Run model inference and get the result
                            float[] result = runInference(byteBuffer);

                            // Process the inference result as needed
                            processInferenceResult(result);

                            // Set up the Intent for ResultActivity and put imagePath in extras
                            Intent intent = new Intent(Capture.this, Result_Activity.class);
                            intent.putExtra("imagePath", imagePath);
                            intent.putExtra("classificationResult", result); // Pass the result to the next activity
                            startActivity(intent);

                            // Restart the camera preview
                            camera.startPreview();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int imageSizeX = 32;
        int imageSizeY = 32;
        int[] intValues = new int[imageSizeX * imageSizeY];
        float[] floatValues = new float[imageSizeX * imageSizeY * 3];

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, imageSizeX, imageSizeY, true);
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());

        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
            floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSizeX * imageSizeY * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.rewind();
        for (int i = 0; i < floatValues.length; ++i) {
            byteBuffer.putFloat(floatValues[i]);
        }
        return byteBuffer;
    }

    private float[] runInference(ByteBuffer input) {
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 32, 32, 3}, DataType.FLOAT32);
        inputFeature0.loadBuffer(input);

        TensorBuffer outputFeature0 = TensorBuffer.createFixedSize(new int[]{1, numClasses}, DataType.FLOAT32);

        // Run the inference
        interpreter.run(inputFeature0.getBuffer(), outputFeature0.getBuffer().rewind());

        // Convert the result to a float array
        float[] result = outputFeature0.getFloatArray();
        return result;
    }

    private void processInferenceResult(float[] result) {
        // Implement logic to process the model inference result, e.g., displaying the result or taking appropriate action.
    }


    private String saveImageLocally(byte[] data) {
        FileOutputStream fos = null;
        File imagePath = null;
        try {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            String imageFileName = "CoffeeDetect_" + System.currentTimeMillis() + ".jpg";
            imagePath = new File(storageDir, imageFileName);

            fos = new FileOutputStream(imagePath);
            fos.write(data);

            return imagePath.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}