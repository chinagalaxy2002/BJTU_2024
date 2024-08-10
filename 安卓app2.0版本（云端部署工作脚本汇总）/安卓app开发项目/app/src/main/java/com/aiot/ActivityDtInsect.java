package com.aiot;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import me.nereo.multi_image_selector.MultiImageSelector;
import me.nereo.multi_image_selector.MultiImageSelectorActivity;

public class ActivityDtInsect extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PHOTO = 1001;
    private static final int REQUEST_IMAGE_SMALL_CUTTING = 1004;

    private Activity activity;
    private ImageView ivImage;
    private TextView infoTxtViw;
    private RelativeLayout relativeLayout;
    private Button btn_select;
    private Button btn_detect;
    private Button btn_video;
    Uri mSrcImageUri = null;
    String imagePath = null;
    boolean bPermission = false;
    private HashMap InsectTrans = new HashMap();

    private Context context;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSessions;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private WebSocketClientManager webSocketClientManager;
    private boolean isStreaming = false; // 添加一个标志位来控制流的开始和停止
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                btn_detect.setEnabled(true);
                String result = (String) msg.obj;

                try {
                    if (result == null || result.isEmpty()) {
                        infoTxtViw.setText("服务器未返回有效数据或连接失败。");
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(result);
                    if (!jsonResponse.has("image")) {
                        infoTxtViw.setText("与服务器通信失败，未包含图像数据。");
                        return;
                    }

                    String imgString = jsonResponse.getString("image");
                    byte[] bitmapByte = Base64.decode(imgString, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapByte, 0, bitmapByte.length);
                    ivImage.setImageBitmap(bitmap);
                    saveImageToGallery(bitmap, context); // 使用正确的context替换getContext()

                    // 根据需要更新这里来显示额外的信息，如有

                } catch (JSONException e) {
                    e.printStackTrace();
                    infoTxtViw.setText("解析服务器响应时出错: " + e.getMessage());
                }
            }

        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_d_insect);
        this.activity = this;
        context=ActivityDtInsect.this;

        requestPermission();
        putValue();
        infoTxtViw=findViewById(R.id.txt_result);
        btn_select=findViewById(R.id.btn_Select);
        btn_detect=findViewById(R.id.btn_Detect);


        ivImage = findViewById(R.id.idImage);
        relativeLayout=findViewById(R.id.Rll_img);
        btn_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infoTxtViw.setText("");


                if (bPermission == false) {
                    requestPermission();
                } else {
                    MultiImageSelector.create()
                            .showCamera(true)
                            .count(1)
                            .start(activity, REQUEST_IMAGE_PHOTO);
                }
            }
        });

        // 初始化WebSocketClientManager
        webSocketClientManager = new WebSocketClientManager(new WebSocketClientManager.Callback() {
            @Override
            public void onReceived(byte[] data) {
                Log.d("onReceived", data + "");
            }
        });

    }


    private void saveImageToGallery(Bitmap bitmap, Context context) {
        // 首先，获取系统相册的路径
        String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();

        // 创建一个文件夹名，这里以“YourAppImages”作为例子
        String folderName = "/YourAppImages/";
        File fileDir = new File(galleryPath + folderName);
        if (!fileDir.exists()) {
            fileDir.mkdirs(); // 如果文件夹不存在，则创建文件夹
        }

        // 创建文件名，这里以当前时间戳命名图片
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(fileDir, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos); // 将bitmap写入文件中
            fos.flush();
            fos.close();

            // 更新图库，这样图片就能在相册应用中看到了
            MediaScannerConnection.scanFile(context, new String[]{file.toString()}, null, null);

            // 可选：给用户一个保存成功的提示
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> Toast.makeText(context, "图片已保存至相册", Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET
            }, REQUEST_CAMERA_PERMISSION);
        } else {
            bPermission = true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        openCamera();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketClientManager != null) {
            webSocketClientManager.close(); // 关闭WebSocket连接
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onImageEvent(ImageEvent event) {
        Bitmap bitmap = event.bitmap;
        Map<String, Integer> classCounts = event.classCounts;

        if (bitmap != null) {
            ivImage.setImageBitmap(bitmap);
            Log.d("Bitmap", "Bitmap成功");
        } else {
            Log.d("Bitmap", "Bitmap解码失败");
        }

        // 构建要显示的字符串
        StringBuilder stringShow = new StringBuilder();
        for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            // 使用InsectTrans映射表获取正确的类别名称
            String readableName = (String) InsectTrans.get(key); // 如果InsectTrans中没有key，则返回key本身
            stringShow.append("名称: ").append(readableName).append("\r\n数量: ").append(value).append("\r\n\r\n");
            Log.d("ClassCounts", "Class: " + readableName + ", Count: " + value);
        }

        // 如果没有检测到对象，可以设置一个默认消息
        if(classCounts.isEmpty()) {
            stringShow.append("未检测到任何对象。");
        }

        // 更新infoTxtViw显示类别名称和数量
        infoTxtViw.setText(stringShow.toString());
    }


    private void toggleStreaming() {
        if (!isStreaming) {
            if (bPermission) {
                startBackgroundThread();
                openCamera();
                btn_video.setText("停止");
            } else {
                requestPermission();
            }
        } else {
            closeCamera();
            stopBackgroundThread();
            btn_video.setText("实时");
        }
        isStreaming = !isStreaming;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PHOTO && resultCode == RESULT_OK) {//从相册选择完图片
            //压缩图片
            ArrayList<String> images = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);

            try {
                File pictureFile = new File(images.get(0));
                imagePath = images.get(0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                {
                    mSrcImageUri = FileProvider.getUriForFile(this, "me.nereo.multi_image_selector.fileprovider", pictureFile);
                } else {
                    mSrcImageUri = Uri.fromFile(pictureFile);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "图片路径非法", Toast.LENGTH_SHORT).show();
                return;
            }

            Glide.with(activity.getApplicationContext()).load(mSrcImageUri).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(ivImage);
        }
    }


    public void onCapture_insect(View view)
    {
        btn_detect.setEnabled(false);

        if (imagePath == null) {
            return;
        }
        new Thread(){
            @Override
            public void run() {
                String result = ImageBase64Converter.post(imagePath);
                Message msg = new Message();
                msg.what = 1;
                msg.obj = result;
                handler.sendMessage(msg);

            }
        }.start();
    }

    public void putValue()
    {
        InsectTrans.put("blast", "稻瘟病");
        InsectTrans.put("blight", "白叶枯病");
        InsectTrans.put("brown", "胡麻斑病");
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission request was denied.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 3000) { // 假设这是选择图片所需的权限请求代码
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                bPermission = true;
                MultiImageSelector.create()
                        .showCamera(true)
                        .count(1)
                        .multi()
                        .start(this, REQUEST_IMAGE_PHOTO);
            } else {
                bPermission = false;
                Toast.makeText(this, "Permission request was denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
                return;
            }
            // 只有在开始流式传输时才连接WebSocket
            if (!isStreaming) {
                // 初始化WebSocketClientManager
                webSocketClientManager = new WebSocketClientManager(new WebSocketClientManager.Callback() {
                    @Override
                    public void onReceived(byte[] data) {
                        Log.d("onReceived", data + "");
                    }
                });
                webSocketClientManager.connect("ws://49.232.151.58:10086/ws");
//                webSocketClientManager.connect("ws://192.168.16.78:10086/ws");
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };

    private void createCameraPreviewSession() {
        try {
            imageReader = ImageReader.newInstance(400, 200, ImageFormat.YUV_420_888, 2); // 根据需要调整分辨率
            imageReader.setOnImageAvailableListener(readerListener, backgroundHandler);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Surface surface = imageReader.getSurface();
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (null == cameraDevice) {
                        return; // 摄像头已关闭
                    }
                    cameraCaptureSessions = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(ActivityDtInsect.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (null == cameraDevice) {
            Log.e("CameraActivity", "updatePreview error, cameraDevice is null");
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraCaptureSessions) {
            cameraCaptureSessions.close();
            cameraCaptureSessions = null;
        }
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private final ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
        private int frameCounter = 0; // 增加一个帧计数器

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null && isStreaming) {
                    frameCounter++; // 每次调用时帧计数器加1

                    // 只有当计数器达到5时才处理和发送图像
                    if (frameCounter % 5 == 0) {
                        // 将YUV_420_888图像转换为字节数组
                        byte[] imageBytes = convertYUV420ToNV21(image);

                        // 使用正确的YUV格式（NV21）创建YuvImage
                        YuvImage yuvImage = new YuvImage(imageBytes, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 50, out);
                        byte[] jpegBytes = out.toByteArray();

                        // 将JPEG字节流转换为Bitmap
                        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);

                        // 旋转Bitmap
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);
                        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                        // 将旋转后的Bitmap转换回JPEG字节流
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                        byte[] rotatedJpegBytes = stream.toByteArray();

                        // 发送旋转后的JPEG字节流到服务器
                        webSocketClientManager.sendFrame(rotatedJpegBytes);

                        Log.d("ActivityDtInsect", "Sending rotated image data: size=" + rotatedJpegBytes.length);
                    }
                }
            } catch (Exception e) {
                Log.e("ActivityDtInsect", "Error processing image", e);
            } finally {
                if (image != null) {
                    image.close();
                }
                // 注意不要在这里重置帧计数器
            }

            // 只有发送数据后才重置帧计数器，以确保每隔5帧发送一次数据
            if (frameCounter % 5 == 0) {
                frameCounter = 0;
            }
        }
    };



    private byte[] convertYUV420ToNV21(Image imgYUV420) {
        byte[] nv21;
        ByteBuffer yBuffer = imgYUV420.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = imgYUV420.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = imgYUV420.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }


}
