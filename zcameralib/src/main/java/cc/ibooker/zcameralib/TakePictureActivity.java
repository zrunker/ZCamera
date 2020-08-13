package cc.ibooker.zcameralib;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cc.ibooker.zbitmaplib.BitmapUtil;

/**
 * 自定义拍照功能
 *
 * @author 邹峰立
 */
public class TakePictureActivity extends AppCompatActivity implements View.OnClickListener {
    private final int TAKE_PICTURE_REQUEST_CODE = 111;
    private final int BITMAP_FILE_REQUEST_CODE = 112;
    private final int TO_ROTATEPICTURE_REQUEST_CODE = 113;
    private ZCameraView cameraView;
    private ImageView ivPreview, ivArrowDown, ivTakepic, ivRotate;
    private TextView tvComplete;
    private Bitmap bitmap;
    private Uri uri;
    private MyHandler myHandler = new MyHandler(this);
    private ProgressDialog progressDialog;
    private ExecutorService executorService;
    private String msg = "success";

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
            if (msg.what == currentActivity.TAKE_PICTURE_REQUEST_CODE) {
                // 关闭进度条
                if (currentActivity.progressDialog != null)
                    currentActivity.progressDialog.cancel();
                // 刷新界面
                currentActivity.takePicAfter();
            } else if (msg.what == currentActivity.BITMAP_FILE_REQUEST_CODE) {
                String filePath = (String) msg.obj;
                Intent intent = new Intent();
                intent.putExtra("filePath", filePath);
                intent.putExtra("message", TextUtils.isEmpty(filePath) ? currentActivity.msg : "success");
                currentActivity.setResult(RESULT_OK, intent);
                currentActivity.finish();
            }
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
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
            System.gc();
        }
        if (cameraView != null)
            cameraView.destory();
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
                if (myHandler == null)
                    myHandler = new MyHandler(TakePictureActivity.this);
                if (data != null) {
                    if (progressDialog == null) {
                        progressDialog = new ProgressDialog(TakePictureActivity.this);
                        progressDialog.setMessage("图片处理中...");
                        progressDialog.show();
                    }
                    // 开启线程进行处理
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            // 默认拍照之后图片为横屏 - 旋转90
                            rotateBitmap(cameraView.getCameraOrientation());
                            if (bitmap != null) {
                                // 生成Uri
                                uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "img" + System.currentTimeMillis(), null));
                                // 刷新界面
                                if (myHandler != null) {
                                    Message message = Message.obtain();
                                    message.what = TAKE_PICTURE_REQUEST_CODE;
                                    myHandler.sendMessage(message);
                                }
                            }
                        }
                    });
                    if (executorService == null || executorService.isShutdown())
                        executorService = Executors.newSingleThreadExecutor();
                    executorService.execute(thread);
                } else {
                    msg = "拍照失败！";
                    Message message = Message.obtain();
                    message.what = BITMAP_FILE_REQUEST_CODE;
                    myHandler.sendMessage(message);
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
        if (ClickUtil.isFastClick()) return;
        int i = v.getId();
        if (i == R.id.iv_takepic) {// 拍照
            cameraView.takePicture();
        } else if (i == R.id.iv_rotate) {// 旋转
            ivRotate.setEnabled(false);
            if (uri == null)
                uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "img" + System.currentTimeMillis(), null));
            Intent intent = new Intent(this, RotatePictureActivity.class);
            intent.setData(uri);
            startActivityForResult(intent, TO_ROTATEPICTURE_REQUEST_CODE);
            ivRotate.setEnabled(true);
        } else if (i == R.id.tv_complete) {// 完成
            bitmapToFile();
        } else if (i == R.id.iv_arrow_down) {// 重新拍照
            if (tvComplete.getVisibility() == View.VISIBLE)
                recreate();
            else
                finish();
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
            try {
                // 压缩图片 - 图片不能超过8M
                bitmap = BitmapUtil.compressBitmapByQuality(bitmap, 8 * 1024);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // bitmap转File文件
    public void bitmapToFile() {
        if (myHandler == null)
            myHandler = new MyHandler(this);
        if (bitmap != null) {
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("生成文件中...");
                progressDialog.show();
            }
            // 将字节流写成文件 - 推荐 - 子线程
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    File file = null;
                    BufferedOutputStream bos = null;
                    try {
                        // 将tempBm写入文件
                        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                            // 保存图片路径
                            String targetSDPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ibooker" + File.separator;
                            File targetFile = new File(targetSDPath);
                            boolean bool = targetFile.exists();
                            if (!bool)
                                bool = targetFile.mkdirs();
                            if (bool) {
                                String filePath = targetSDPath + System.currentTimeMillis() + ".JPEG";
                                file = new File(filePath);
                                bool = file.exists();
                                if (!bool)
                                    bool = file.createNewFile();
                            }
                            if (bool) {
                                bos = new BufferedOutputStream(new FileOutputStream(file));
                                // 将图片压缩到流中
                                if (!bitmap.isRecycled())
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                            }
                        } else
                            msg = "SD卡未找到！";
                    } catch (IOException e) {
                        e.printStackTrace();
                        msg = e.getMessage();
                    } finally {
                        if (bos != null) {
                            try {
                                bos.flush();// 输出
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (bos != null) {
                            try {
                                bos.close();// 关闭
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (bitmap != null)
                            bitmap.recycle();// 回收bitmap空间
                    }
                    // 切换主线程
                    if (myHandler != null) {
                        Message message = Message.obtain();
                        message.what = BITMAP_FILE_REQUEST_CODE;
                        if (file != null && file.exists())
                            message.obj = file.getAbsolutePath();
                        myHandler.sendMessage(message);
                    }
                }
            });
            if (executorService == null || executorService.isShutdown())
                executorService = Executors.newSingleThreadExecutor();
            executorService.execute(thread);
        } else {
            msg = "图片对象丢失！";
            Message message = Message.obtain();
            message.what = BITMAP_FILE_REQUEST_CODE;
            myHandler.sendMessage(message);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == TO_ROTATEPICTURE_REQUEST_CODE) {
                Intent intent = new Intent();
                if (data != null) {
                    intent.putExtra("filePath", data.getStringExtra("filePath"));
                    intent.putExtra("message", data.getStringExtra("message"));
                } else
                    intent.putExtra("message", "发生未知异常！");
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }
}
