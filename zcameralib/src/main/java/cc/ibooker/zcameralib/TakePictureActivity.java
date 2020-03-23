package cc.ibooker.zcameralib;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自定义拍照功能
 *
 * @author 邹峰立
 */
public class TakePictureActivity extends AppCompatActivity implements View.OnClickListener {
    private ZCameraView cameraView;
    private ImageView ivPreview, ivArrowDown, ivTakepic, ivRotate;
    private TextView tvComplete;
    private Bitmap bitmap;
    private final int currentRotate = -90;
    private MyHandler myHandler;
    private ProgressDialog progressDialog;
    private ExecutorService executorService;

    // 自定义Handler
    private static class MyHandler extends Handler {
        private WeakReference<TakePictureActivity> mWeakRef;

        MyHandler(TakePictureActivity activity) {
            mWeakRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            TakePictureActivity currentActivity = mWeakRef.get();
            // 关闭进度条
            if (currentActivity.progressDialog != null)
                currentActivity.progressDialog.cancel();
            // 刷新界面
            currentActivity.takePicAfter();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zcamera_activity_take_picture);

        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null)
            progressDialog.cancel();
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        if (myHandler != null) {
            myHandler.removeCallbacks(null);
            myHandler = null;
        }
    }

    // 初始化View
    private void initView() {
        cameraView = findViewById(R.id.cameraView);
        cameraView.requestPermissions();
        cameraView.setCameraTakePicListener(new CameraTakePicListener() {
            @Override
            public void onShutter() {

            }

            @Override
            public void onRawPictureTaken(byte[] data, Camera camera) {

            }

            @Override
            public void onJpegPictureTaken(final byte[] data, Camera camera) {
                if (data != null) {
                    if (progressDialog == null) {
                        progressDialog = new ProgressDialog(TakePictureActivity.this);
                        progressDialog.setMessage("图片处理中...");
                        progressDialog.show();
                    }
                    if (myHandler == null)
                        myHandler = new MyHandler(TakePictureActivity.this);
                    // 开启线程进行处理
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            // 默认拍照之后图片为横屏 - 旋转90
                            rotateBitmap(cameraView.getCameraOrientation());
                            // 刷新界面
                            Message message = Message.obtain();
                            message.what = 100;
                            myHandler.sendMessage(message);
                        }
                    });
                    if (executorService == null)
                        executorService = Executors.newSingleThreadExecutor();
                    executorService.execute(thread);
                }
            }

            @Override
            public void onError(int error, Camera camera) {

            }

            @Override
            public void onAutoFocus(boolean success, Camera camera) {

            }
        });
        ivPreview = findViewById(R.id.iv_preview);
        ivArrowDown = findViewById(R.id.iv_arrow_down);
        ivArrowDown.setOnClickListener(this);
        ivTakepic = findViewById(R.id.iv_takepic);
        ivTakepic.setOnClickListener(this);
        tvComplete = findViewById(R.id.tv_complete);
        tvComplete.setOnClickListener(this);
        ivRotate = findViewById(R.id.iv_rotate);
        ivRotate.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.iv_takepic) {// 拍照
            cameraView.takePicture();
        } else if (i == R.id.iv_rotate) {// 旋转
            rotateBitmap(currentRotate);
            ivPreview.setImageBitmap(bitmap);
        } else if (i == R.id.tv_complete) {// 完成

        } else if (i == R.id.iv_arrow_down) {// 重新拍照
            recreate();
        }
    }

    // 点击拍照之后 - 刷新界面
    private void takePicAfter() {
        if (bitmap != null) {
            ivPreview.setImageBitmap(bitmap);
            ivPreview.setVisibility(View.VISIBLE);
            cameraView.setVisibility(View.GONE);
            tvComplete.setVisibility(View.VISIBLE);
            ivTakepic.setVisibility(View.GONE);
            ivArrowDown.setImageResource(R.mipmap.zcamera_icon_arrow_left_white);
            ivRotate.setVisibility(View.VISIBLE);
        }
    }

    // 旋转Bitmap
    private void rotateBitmap(int rotate) {
        if (bitmap != null) {
            Matrix matrix = new Matrix();
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();
            matrix.setRotate(rotate);
            // 旋转后的图片
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == cameraView.getCameraRequestCode()) {
            if (!cameraView.hasPermission(cameraView.getNeedPermissions())) {
                Toast.makeText(this, "所需权限未授权！", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                // 重新渲染页面
                recreate();
            }
        }
    }
}
