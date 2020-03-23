package cc.ibooker.zcameralib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zcamera_activity_take_picture);

        initView();
    }

    // 初始化View
    private void initView() {
        cameraView = findViewById(R.id.cameraView);
        cameraView.setCameraTakePicListener(new CameraTakePicListener() {
            @Override
            public void onShutter() {

            }

            @Override
            public void onRawPictureTaken(byte[] data, Camera camera) {

            }

            @Override
            public void onJpegPictureTaken(byte[] data, Camera camera) {
                if (data != null) {
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    // 默认拍照之后图片为横屏 - 旋转90
                    Matrix matrix = new Matrix();
                    int height = bitmap.getHeight();
                    int width = bitmap.getWidth();
                    matrix.setRotate(cameraView.getCameraOrientation());
                    // 旋转后的图片
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

                    // 刷新界面
                    takePicAfter();
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
}
