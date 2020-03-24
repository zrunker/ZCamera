package cc.ibooker.zcameralib;

import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 旋转图片
 *
 * @author 邹峰立
 */
public class RotatePictureActivity extends AppCompatActivity implements View.OnClickListener {
    private final int BITMAP_FILE_REQUEST_CODE = 112;
    private ImageView iv;
    private Bitmap bitmap;
    private int currentRotate;
    private TextView tvReset;
    private MyHandler myHandler;
    private ProgressDialog progressDialog;
    private ExecutorService executorService;
    private String msg = "success";

    // 自定义Handler
    private static class MyHandler extends Handler {
        private WeakReference<RotatePictureActivity> mWeakRef;

        MyHandler(RotatePictureActivity activity) {
            mWeakRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            RotatePictureActivity currentActivity = mWeakRef.get();
            if (msg.what == currentActivity.BITMAP_FILE_REQUEST_CODE) {
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
        setContentView(R.layout.zcamera_activity_rotate_picture);

        try {
            Uri uri = getIntent().getData();
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 初始化控件
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
    }

    // 初始化控件
    private void initView() {
        findViewById(R.id.tv_cancel).setOnClickListener(this);
        tvReset = findViewById(R.id.tv_reset);
        tvReset.setOnClickListener(this);
        findViewById(R.id.tv_ensure).setOnClickListener(this);
        findViewById(R.id.iv_rotate_left).setOnClickListener(this);
        findViewById(R.id.iv_rotate_right).setOnClickListener(this);
        iv = findViewById(R.id.iv);
        // 显示图片
        if (bitmap != null)
            iv.setImageBitmap(bitmap);
    }

    // 点击事件
    @Override
    public void onClick(View v) {
        if (ClickUtil.isFastClick()) return;
        int i = v.getId();
        if (i == R.id.tv_cancel) {// 取消
            finish();
        } else if (i == R.id.tv_reset) {// 还原
            startRotateAnim(currentRotate, 0);
        } else if (i == R.id.tv_ensure) {// 确定
            bitmapToFile();
        } else if (i == R.id.iv_rotate_left) {// 向左旋转 - 90度
            startRotateAnim(currentRotate, currentRotate - 90);
        } else if (i == R.id.iv_rotate_right) {// 向右旋转 - 90度
            startRotateAnim(currentRotate, currentRotate + 90);
        }
        if (currentRotate == 0) {
            tvReset.setTextColor(Color.parseColor("#666666"));
            tvReset.setEnabled(false);
        } else {
            tvReset.setTextColor(Color.parseColor("#ffffff"));
            tvReset.setEnabled(true);
        }
    }

    // 设置旋转动画
    private void startRotateAnim(int start, int end) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(iv, "rotation", start, end);
        anim.setDuration(1000);
        anim.start();
        currentRotate = end % 360;
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
                // 压缩图片 - 图片不能超过7M
                bitmap = BitmapUtil.compressImageByQuality(bitmap, 7 * 1024);
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
                    // 旋转
                    rotateBitmap(currentRotate);
                    // 写入文件
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
                    Message message = myHandler.obtainMessage();
                    message.what = BITMAP_FILE_REQUEST_CODE;
                    if (file != null && file.exists())
                        message.obj = file.getAbsolutePath();
                    myHandler.sendMessage(message);
                }
            });
            if (executorService == null)
                executorService = Executors.newSingleThreadExecutor();
            executorService.execute(thread);
        } else {
            msg = "图片对象丢失！";
            Message message = myHandler.obtainMessage();
            message.what = BITMAP_FILE_REQUEST_CODE;
            myHandler.sendMessage(message);
        }
    }

}