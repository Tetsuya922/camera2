package jp.shsit.camera2;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;

import android.view.Surface;

import android.view.TextureView;
import android.view.View;

import android.widget.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private CameraDevice yCameraDevice;
    private TextureView yTextureView;
    private Handler yBackgroundHandler = new Handler();
    private CameraCaptureSession yCaptureSession = null;

    private CaptureRequest.Builder yPreviewRequestBuilder;
    private CaptureRequest yPreviewRequest;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        yTextureView = (TextureView) findViewById(R.id.textureview);
        yTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                // 先ほどのカメラを開く部分をメソッド化した
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        Button capture = (Button) findViewById(R.id.button_capture);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    yCaptureSession.stopRepeating(); // プレビューの更新を止める
                    if (yTextureView.isAvailable()) {
                        File file = new File(getFilesDir(), "surface_text.jpg");
                        FileOutputStream fos = new FileOutputStream(file);
                        Bitmap bitmap = yTextureView.getBitmap();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
                        fos.close();
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String selectedCameraId = "";
        try {
            selectedCameraId = manager.getCameraIdList()[0];

            // https://github.com/googlesamples/android-Camera2Basic/blob/5dad16c103715b5e7e3c001cc5f6067f8d23f29e/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java#L499
            // あたりにあるのですが、顔用カメラを使いたくないなどがあれば、CameraCharacteristicsを経由して確認可能
            //            CameraCharacteristics characteristics
            //                    = manager.getCameraCharacteristics(selectedCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        //パーミッションチェック
        try {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                //ここで、苦戦した
                final int REQUEST_CODE = 1;
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                        Manifest.permission.CAMERA
                }, REQUEST_CODE);
                /**************************************************************************/
                return;
            }
            manager.openCamera(selectedCameraId, mStateCallback, yBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            yCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            yCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            yCameraDevice = null;
        }

    };

    private void createCameraPreviewSession() {
        SurfaceTexture texture = yTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(320, 240); // 自分の手元のデバイスで決めうちしてます
        Surface surface = new Surface(texture);

        try {
            yPreviewRequestBuilder = yCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            yPreviewRequestBuilder.addTarget(surface);
            yPreviewRequest = yPreviewRequestBuilder.build();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        try {
            yCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    // カメラがcloseされている場合
                    if (null == yCameraDevice) {
                        return;


                    }

                    yCaptureSession = session;

                    try {
                        session.setRepeatingRequest(yPreviewRequest, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}