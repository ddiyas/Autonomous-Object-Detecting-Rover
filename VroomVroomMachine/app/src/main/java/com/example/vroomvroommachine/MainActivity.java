package com.example.vroomvroommachine;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 11;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN};

    private TextureView textureView;
    private CameraManager cameraManager;
    private Handler handler;
    private Handler bluetoothHandler;
    private CameraDevice cameraDevice;
    private ImageView imageView;
    private Bitmap bitmap;
    private ObjectDetector objectDetector;
    private ImageProcessor imageProcessor;
    private Paint paint;
    private Button startDetection;
    private EditText targetObjectEditText;
    private boolean isDetecting = false;
    private String targetObject;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private Message bluetoothMessage;

    private BluetoothConnector bluetoothConnector;
    private BluetoothConnected bluetoothConnected;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String MAC = "00:00:00:00:00:00";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermissions();

        imageView = findViewById(R.id.imageView);
        textureView = findViewById(R.id.textureView);
        startDetection = findViewById(R.id.startDetectionButton);
        targetObjectEditText = findViewById(R.id.targetObjectEditText);

        HandlerThread handlerThread = new HandlerThread("stream");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        paint = new Paint();

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        imageProcessor = new ImageProcessor.Builder().add(new ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build();

        try {
            objectDetector = ObjectDetector.createFromFile(this, "1.tflite");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                try {
                    openCamera();
                } catch (CameraAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                if (isDetecting) {
                    processImage();
                }
            }
        });

        startDetection.setOnClickListener(v -> {
            targetObject = targetObjectEditText.getText().toString().trim();
            if (!targetObject.isEmpty()) {
                isDetecting = true;
                Toast.makeText(getBaseContext(), "We're looking for " + targetObject, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getBaseContext(), "What are we looking for though...", Toast.LENGTH_SHORT).show();
            }
        });

        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bluetoothHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                bluetoothMessage = msg;
            }
        };

        connectToArduino();
    }

    private void openCamera() throws CameraAccessException {
        CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                camera.close();
                cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                camera.close();
                cameraDevice = null;
                Log.d("CAMERA_BEEP_BOOP", "We have a problem..." + error);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.openCamera(cameraManager.getCameraIdList()[1], stateCallback, handler);
        } else {
            getPermissions();
        }
    }

    private void processImage() {
        bitmap = textureView.getBitmap();
        TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
        tensorImage = imageProcessor.process(tensorImage);
        List<Detection> detections = objectDetector.detect(tensorImage);

        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        float scaleX = (float) mutableBitmap.getWidth() / 300;
        float scaleY = (float) mutableBitmap.getHeight() / 300;

        paint.setTextSize(50);
        paint.setStrokeWidth(5);

        for (Detection d : detections) {
            System.out.println(d.getCategories().get(0).getLabel() + " : " + d.getCategories().get(0).getScore());
            if (d.getCategories().get(0).getScore() > 0.6) {
                if (d.getCategories().get(0).getLabel().equalsIgnoreCase(targetObject)) {
                    isDetecting = false;
                    sendCommandToArduino("S");
                    Toast.makeText(getBaseContext(), "WE FOUND THE " + targetObject, Toast.LENGTH_LONG).show();
                }
                RectF boundingBox = d.getBoundingBox();
                boundingBox.left *= scaleX;
                boundingBox.top *= scaleY;
                boundingBox.right *= scaleX;
                boundingBox.bottom *= scaleY;

                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(boundingBox, paint);

                paint.setStyle(Paint.Style.FILL);
                canvas.drawText(d.getCategories().get(0).getLabel(), boundingBox.left, boundingBox.top, paint);
            }
        }

        runOnUiThread(() -> imageView.setImageBitmap(mutableBitmap));
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) return;
            texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
            Surface surface = new Surface(texture);

            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) return;

                    CaptureRequest captureRequest = captureRequestBuilder.build();
                    try {
                        session.setRepeatingRequest(captureRequest, null, handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectToArduino() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(MAC);
        bluetoothConnector = new BluetoothConnector(device, MY_UUID, bluetoothHandler);
        bluetoothConnector.start();

        new Handler().postDelayed(() -> {
            BluetoothSocket socket = bluetoothConnector.getBluetoothSocket();
            if (socket != null && socket.isConnected()) {
                try {
                    bluetoothConnected = new BluetoothConnected(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Failed to connect to Arduino", Toast.LENGTH_SHORT).show();
            }
        }, 3000);
    }

    private void sendCommandToArduino(String command) {
        if (bluetoothConnected != null) {
            try {
                bluetoothConnected.getOutputStream().write(command.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to send command", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Not connected to Arduino", Toast.LENGTH_SHORT).show();
        }
    }

    private void getPermissions() {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (allPermissionsGranted()) {
                try {
                    openCamera();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "You gotta give permissions for this to work...", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (objectDetector != null) {
            objectDetector.close();
        }
        if (bluetoothConnector != null) {
            bluetoothConnector.cancel();
        }
        if (bluetoothConnected != null) {
            bluetoothConnected.cancel();
        }
    }
}
