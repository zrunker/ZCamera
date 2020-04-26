package cc.ibooker.zcameralib;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 身份证正面照
 *
 * @author 邹峰立
 */
public class IDCardFrontActivity extends AppCompatActivity implements View.OnClickListener {
    private final int ONJPEGPICTURETAKEN_CODE = 1111;
    private ZCameraView zCameraView;
    private FrameLayout cameraScanCropFl;
    private LinearLayout cameraScanContainerLl;
    private ExecutorService executorService;
    private MyHandler myHandler = new MyHandler(this);
    private ProgressDialog progressDialog;
    // 扫描裁剪框背景
    private int scanCropBgRes;
    // 主题
    private String title;
    // 提示
    private String tip;
    // 页面背景颜色
    private int decorViewBgRes;
    private ImageView ivLight;
    // 是否开启闪光灯
    private boolean openFlashLight = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zcamera_activity_idcard_front);

        decorViewBgRes = getIntent().getIntExtra("decorViewBgRes", R.color.zcamera_cc000000);
        scanCropBgRes = getIntent().getIntExtra("scanCropBgRes", 0);
        title = getIntent().getStringExtra("title");
        tip = getIntent().getStringExtra("tip");

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
        if (zCameraView != null)
            zCameraView.destory();
    }

    // 初始化控件
    private void initView() {
        ivLight = findViewById(R.id.iv_light);
        ivLight.setOnClickListener(this);
        RelativeLayout titleRl = findViewById(R.id.rl_title);
        titleRl.setBackgroundResource(decorViewBgRes);
        View topView = findViewById(R.id.view_top);
        topView.setBackgroundResource(decorViewBgRes);
        View leftView = findViewById(R.id.view_left);
        leftView.setBackgroundResource(decorViewBgRes);
        View rightView = findViewById(R.id.view_right);
        rightView.setBackgroundResource(decorViewBgRes);
        TextView backTv = findViewById(R.id.tv_back);
        backTv.setOnClickListener(this);
        findViewById(R.id.tv_tip).setOnClickListener(this);
        TextView titleTv = findViewById(R.id.tv_title);
        if (!TextUtils.isEmpty(title))
            titleTv.setText(title);
        TextView tipTv = findViewById(R.id.tv_tip);
        if (!TextUtils.isEmpty(tip))
            tipTv.setText(tip);
        tipTv.setBackgroundResource(decorViewBgRes);
        cameraScanContainerLl = findViewById(R.id.ll_camera_scan_container);
        cameraScanCropFl = findViewById(R.id.fl_camera_scan_crop);
        if (scanCropBgRes != 0)
            cameraScanCropFl.setBackgroundResource(scanCropBgRes);
        ImageView takePicIv = findViewById(R.id.iv_takepic);
        takePicIv.setOnClickListener(this);
        zCameraView = findViewById(R.id.csview);
        zCameraView.requestPermissions();
        zCameraView.setCameraTakePicListener(new CameraTakePicListener() {

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
                        progressDialog = new ProgressDialog(IDCardFrontActivity.this);
                        progressDialog.setMessage("图片处理中...");
                        progressDialog.show();
                    }
                    if (myHandler == null)
                        myHandler = new MyHandler(IDCardFrontActivity.this);
                    // 将字节流写成文件 - 推荐 - 子线程
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            File file = null;
                            BufferedOutputStream bos = null;
                            Bitmap bitmap = null;
                            try {
                                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                // 将tempBm写入文件
                                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

                                    // 默认拍照之后图片为横屏 - 旋转90
                                    Matrix matrix = new Matrix();
                                    int height = bitmap.getHeight();
                                    int width = bitmap.getWidth();
                                    matrix.setRotate(zCameraView.getCameraOrientation());
                                    // 旋转后的图片
                                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

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
//                                        // 缩放
//                                        bitmap = Bitmap.createScaledBitmap(bitmap,
//                                                cameraScanContainerLl.getWidth(), cameraScanContainerLl.getHeight(), true);
                                        // 截取
                                        Rect rect = getCropRect();
                                        bitmap = Bitmap.createBitmap(bitmap,
                                                rect.left,
                                                rect.top,
                                                rect.right - rect.left,
                                                rect.bottom - rect.top);
                                        // 将图片压缩到流中
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
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
                                message.what = ONJPEGPICTURETAKEN_CODE;
                                if (file != null && file.exists())
                                    message.obj = file.getAbsolutePath();
                                myHandler.sendMessage(message);
                            }
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
    }

    /**
     * 初始化截取的矩形区域
     */
    private Rect getCropRect() {
        int cameraWidth = zCameraView.getCameraResolution().height;
        int cameraHeight = zCameraView.getCameraResolution().width;

        // 获取布局中扫描框的位置信息
        int[] location = new int[2];
        cameraScanCropFl.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        // 裁剪区域的宽高
        int cropWidth = cameraScanCropFl.getWidth();
        int cropHeight = cameraScanCropFl.getHeight();

        // 获取布局容器的宽高
        int containerWidth = cameraScanContainerLl.getWidth();
        int containerHeight = cameraScanContainerLl.getHeight();

        float dw = (float) cameraWidth / containerWidth;
        float dh = (float) cameraHeight / containerHeight;

//        // 宽度偏移量 - 适应曲面屏
//        int widthOffset = 0;
//        int widthDiff = cameraWidth - containerWidth;
//        if (widthDiff > 0)
//            widthOffset = cameraScanCropFl.getLeft() / 2;
//        if (widthOffset > widthDiff / 2)
//            widthOffset = 0;
        // 计算最终截取的矩形的左上角顶点x坐标
        int x = (int) (cropLeft * dw);
        // 计算最终截取的矩形的左上角顶点y坐标
        int y = (int) (cropTop * dh);

        // 计算最终截取的矩形的宽度
        int width = (int) (cropWidth * dw);
        // 计算最终截取的矩形的高度
        int height = (int) (cropHeight * dh);

        // 生成最终的截取的矩形
        return new Rect(x, y, width + x, height + y);
    }

//    /**
//     * 初始化截取的矩形区域 - 方案二
//     */
//    private Rect getCropRect() {
//        /** 获取布局中扫描框的位置信息 */
//        int[] location = new int[2];
//        cameraScanCropFl.getLocationInWindow(location);
//
//        int cropLeft = location[0];
//        int cropTop = location[1] - getStatusBarHeight();
//
//        // 裁剪区域的宽高
//        int cropWidth = cameraScanCropFl.getWidth();
//        int cropHeight = cameraScanCropFl.getHeight();
//
//        /** 生成最终的截取的矩形 */
//        return new Rect(cropLeft, cropTop, cropWidth + cropLeft, cropHeight + cropTop);
//    }

    // 获取状态栏高度
    private int getStatusBarHeight() {
        try {
            @SuppressLint("PrivateApi")
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 点击事件监听
    @Override
    public void onClick(View v) {
        if (ClickUtil.isFastClick()) return;
        int i = v.getId();
        if (i == R.id.tv_back) {// 返回
            finish();
        } else if (i == R.id.iv_takepic) {// 拍照
            zCameraView.takePicture();
        } else if (i == R.id.tv_tip) {// 重新聚焦
            zCameraView.executeTouchEvent();
        }  else if (i == R.id.iv_light) {// 闪光灯
            if (openFlashLight) {
                ivLight.setImageResource(R.drawable.zcamera_icon_light_off);
                zCameraView.turnOffFlash();
            } else {
                ivLight.setImageResource(R.drawable.zcamera_icon_light_on);
                zCameraView.turnOnFlash();
            }
            openFlashLight = !openFlashLight;
        }
    }

    // 自定义Handler
    private static class MyHandler extends Handler {
        private WeakReference<IDCardFrontActivity> mWeakRef;

        MyHandler(IDCardFrontActivity activity) {
            mWeakRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            IDCardFrontActivity currentActivity = mWeakRef.get();
            if (msg.what == currentActivity.ONJPEGPICTURETAKEN_CODE) {
                String filePath = (String) msg.obj;
                Intent intent = new Intent();
                intent.putExtra("filePath", filePath);
                intent.putExtra("message", TextUtils.isEmpty(filePath) ? "拍照失败！" : "success");
                currentActivity.setResult(RESULT_OK, intent);
                currentActivity.finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == zCameraView.getCameraRequestCode()) {
            if (!zCameraView.hasPermission(zCameraView.getNeedPermissions())) {
                Toast.makeText(this, "所需权限未授权！", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                // 重新渲染页面
                recreate();
            }
        }
    }
}
