package cc.ibooker.zcameralib;

import android.hardware.Camera;

/**
 * 拍照监听接口
 *
 * @author 邹峰立
 */
public interface CameraTakePicListener {

    // 拍照瞬间调用
    void onShutter();

    // 获得没有压缩过的图片数据
    void onRawPictureTaken(byte[] data, Camera camera);

    // 创建jpeg图片回调数据对象
    void onJpegPictureTaken(byte[] data, Camera camera);

    // 相机出错
    void onError(int error, Camera camera);

    // 相机获取焦点
    void onAutoFocus(boolean success, Camera camera);
}
