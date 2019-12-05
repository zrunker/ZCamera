package cc.ibooker.zcameralib;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

/**
 * 自定义相机View - 相机要设置正确的预览尺寸和图片尺寸 - 重点（大小 + 屏幕宽高比例）
 *
 * @author 邹峰立
 */
public class CameraScanView extends SurfaceView
        implements SurfaceHolder.Callback,
        Camera.AutoFocusCallback,
        Camera.ErrorCallback {
    private final int CAMERA_REQUEST_CODE = 2222;
    private int mCameraId = 0;
    private Context mContext;
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private Camera.Size pictureSize, preViewSize;
    private int mScreenWidth, mScreenHeight;
    // 屏幕旋转显示角度
    private int mDisplayOrientation;
    // 相机旋转角度
    private int mCameraOrientation;
    // 是否正在聚焦 - 防止频繁对焦
    private boolean isFoucing = false;
    // 需要申请的权限
    private String[] needPermissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public CameraScanView(Context context) {
        this(context, null);
    }

    public CameraScanView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        requestPermissions();
        init(context);
    }

    // 初始化
    private void init(Context context) {
        this.mContext = context;
        this.mHolder = getHolder();
        this.mHolder.addCallback(this);
        // 设置类型
        this.mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.mHolder.setKeepScreenOn(true);
        this.mDisplayOrientation = ((Activity) context).getWindowManager().getDefaultDisplay().getRotation();
    }

    // 布局创建
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera == null) {
            // 开启相机 0-后置摄像头，1-前置摄像头
            mCamera = Camera.open(mCameraId);

            // 设置错误监听
            mCamera.setErrorCallback(this);

            // 设置预览位置 - 摄像头画面显示在Surface上
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
                mCamera.release();
                mCamera = null;
            }
        }
    }

    // 布局更改
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        setCameraParams();
        // 开始预览
        mCamera.startPreview();
        // 设置对焦监听 - 聚焦应在开始预览之后（否则在部分机型上会报错 - 华为）
        mCamera.autoFocus(this);
    }

    // 布局销毁
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.autoFocus(null);
        // 停止预览
        mCamera.stopPreview();
        // 回收相机
        mCamera.release();
        mCamera = null;
        mHolder = null;
    }

    // 设置相机属性
    private void setCameraParams() {
        if (mCamera != null) {
            // 设置相机相关参数
            Camera.Parameters params = mCamera.getParameters();

            // 设置图片大小
//            Camera.Size pictureSize = getPictureSize(params);

            float[] screenParams = getScreenRatio();
            pictureSize = getBestSize((int) screenParams[0], (int) screenParams[1], params.getSupportedPictureSizes());
            if (pictureSize == null)
                pictureSize = params.getPictureSize();
            int picW = pictureSize.width;
            int picH = pictureSize.height;
            params.setPictureSize(picW, picH);

            // 设置预览大小
//            Camera.Size preViewSize = getPreviewSize(params);

            preViewSize = getBestSize(getWidth(), getHeight(), params.getSupportedPreviewSizes());
            if (preViewSize == null)
                preViewSize = params.getPreviewSize();
            int preW = preViewSize.width;
            int preH = preViewSize.height;
            params.setPreviewSize(preW, preH);

            // 其他设置
            params.setPictureFormat(ImageFormat.JPEG);
//            // 部分手机不支持预览图片格式
//            params.setPreviewFormat(ImageFormat.JPEG);
//            // 不设置属性旋转
//            params.setRotation(mCameraOrientation);
            // 设置图片质量
            params.setJpegQuality(100);
            // 设置连续对焦
            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            // 设置聚焦
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(params);
            // 设置预览方向 - 0, 90, 180, 270 默认摄像头是横拍
            mCameraOrientation = getCameraOri(mDisplayOrientation, mCameraId);
            mCamera.setDisplayOrientation(mCameraOrientation);
        }
    }

    // 获取图片大小
    private Camera.Size getPictureSize(Camera.Parameters params) {
        // 目标尺寸大小
        Camera.Size result = null;
        if (params != null) {
            List<Camera.Size> cameraSizeList = params.getSupportedPictureSizes();
            // 屏幕宽高比例比较
            float screenRatio = getScreenRatio()[2];
            for (Camera.Size size : cameraSizeList) {
                float currentRatio = ((float) size.height) / size.width;
                if (currentRatio - screenRatio == 0) {
                    result = size;
                    break;
                }
            }
            // 默认值，以w:h = 4:3
            if (result == null) {
                for (Camera.Size size : cameraSizeList) {
                    float currentRatio = ((float) size.width) / size.height;
                    if (currentRatio == 4f / 3) {
                        result = size;
                        break;
                    }
                }
            }
        }
        return result;
    }

    // 获取预览大小
    private Camera.Size getPreviewSize(Camera.Parameters params) {
        // 目标尺寸大小
        Camera.Size result = null;
        if (params != null) {
            int realWidth;
            int realHeight;
            int sViewW = getWidth();
            int sViewH = getHeight();
            if (isLandscape(mDisplayOrientation)) {
                realWidth = sViewH;
                realHeight = sViewW;
            } else {
                realWidth = sViewW;
                realHeight = sViewH;
            }
            // 大小比较
            List<Camera.Size> cameraSizeList = params.getSupportedPreviewSizes();
            for (Camera.Size size : cameraSizeList) {
                if (realWidth <= size.height && realHeight <= size.width) {
                    result = size;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 计算Camera.Size
     *
     * @param targetWidth  目标宽
     * @param targetHeight 目标高
     * @param sizeList     对比集合
     * @return 最优大小
     */
    private Camera.Size getBestSize(int targetWidth, int targetHeight, List<Camera.Size> sizeList) {
        Camera.Size result = null;
        // 目标大小的宽高比
        float targetRatio = (float) targetHeight / targetWidth;
        float minDiff = targetRatio;

        for (Camera.Size size : sizeList) {
            if (size.width == targetHeight && size.height == targetWidth) {
                result = size;
                break;
            }
            float supportedRatio = (float) size.width / size.height;
            float absValue = Math.abs(supportedRatio - targetRatio);
            if (absValue < minDiff) {
                minDiff = absValue;
                result = size;
            }
        }
        return result;
    }

    // 获取相机旋转角度
    public int getCameraOrientation() {
        return mCameraOrientation;
    }

    // 当相机的倾斜度数为90/270时是横屏
    private boolean isLandscape(int orientationDegrees) {
        return orientationDegrees == 90 || orientationDegrees == 270;
    }

    // 得到屏幕比例
    private float[] getScreenRatio() {
        WindowManager wM = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wM.getDefaultDisplay().getMetrics(outMetrics);
        mScreenHeight = outMetrics.heightPixels;
        mScreenWidth = outMetrics.widthPixels;
        return new float[]{mScreenWidth, mScreenHeight, (float) outMetrics.widthPixels / outMetrics.heightPixels};
    }

    // 开启相机
    public void takePicture() {
//        setCameraParams();
        if (mCamera != null)
            mCamera.takePicture(shutter, raw, jpeg);
        else {
            Toast.makeText(mContext, "拍照失败！", Toast.LENGTH_SHORT).show();
            ((Activity) mContext).finish();
        }
    }

    // 拍照瞬间调用
    private Camera.ShutterCallback shutter = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            // 默认有“咔嚓”的声音
            if (cameraTakePicListener != null)
                cameraTakePicListener.onShutter();
        }
    };

    // 获得没有压缩过的图片数据
    private Camera.PictureCallback raw = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (cameraTakePicListener != null)
                cameraTakePicListener.onRawPictureTaken(data, camera);
        }
    };

    // 创建jpeg图片回调数据对象
    private Camera.PictureCallback jpeg = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (cameraTakePicListener != null)
                cameraTakePicListener.onJpegPictureTaken(data, camera);
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isFoucing) {
                isFoucing = true;
                if (mCamera != null)
                    mCamera.autoFocus(this);
            }
        }
        return super.onTouchEvent(event);
    }

    // 监听焦点事件
    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        isFoucing = false;
        if (cameraTakePicListener != null)
            cameraTakePicListener.onAutoFocus(success, camera);
    }

    // 开启闪光灯
    public void turnOnFlash() {
        if (mCamera != null) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 关闭闪光灯
    public void turnOffFlash() {
        if (mCamera != null) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onError(int error, Camera camera) {
        if (cameraTakePicListener != null)
            cameraTakePicListener.onError(error, camera);
    }

    // 获取相机图片分辨率
    public Camera.Size getCameraResolution() {
        return pictureSize;
    }

    /**
     * 获取相机的旋转角度
     *
     * @param rotation 屏幕的旋转角度
     * @param cameraId 相机ID
     */
    private int getCameraOri(int rotation, int cameraId) {
        int degrees = rotation * 90;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }

        // result 即为在camera.setDisplayOrientation(int)的参数
        int result;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * 权限检查方法，false代表没有该权限，ture代表有该权限
     */
    public boolean hasPermission(String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 申请权限
     */
    public void requestPermissions() {
        if (!hasPermission(needPermissions))
            ActivityCompat.requestPermissions((Activity) getContext(), needPermissions, CAMERA_REQUEST_CODE);
    }

    // 获取权限请求码
    public int getCameraRequestCode() {
        return CAMERA_REQUEST_CODE;
    }

    // 获取拍照所需权限组
    public String[] getNeedPermissions() {
        return needPermissions;
    }

    // 拍照回调
    public CameraTakePicListener cameraTakePicListener;

    public void setCameraTakePicListener(CameraTakePicListener cameraTakePicListener) {
        this.cameraTakePicListener = cameraTakePicListener;
    }

}
