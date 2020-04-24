package cc.ibooker.zcamera;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import cc.ibooker.zcameralib.IDCardBackActivity;
import cc.ibooker.zcameralib.IDCardFrontActivity;
import cc.ibooker.zcameralib.ScanBankCardActivity;
import cc.ibooker.zcameralib.TakePictureActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onEnterScan(View view) {
        Intent intent = new Intent(this, ScanBankCardActivity.class);
        // 扫描裁剪框背景
        intent.putExtra("scanCropBgRes", R.drawable.zcamera_bg_layerl_h_cc000000_2_5_h_fa3a00_2_c_10_a);
        // 主题
        intent.putExtra("title", "扫描银行卡正面");
        // 提示
        intent.putExtra("tip", "将银行卡卡号面放在此区域，扫描卡片");
        startActivityForResult(intent, 111);
    }

    public void onEnterCamera(View view) {
        Intent intent = new Intent(this, TakePictureActivity.class);
        startActivityForResult(intent, 111);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 111 && data != null) {
                String filePath = data.getStringExtra("filePath");
                Toast.makeText(this, filePath, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 进入身份证反面拍照
    public void onEnterIdCardBack(View view) {
        Intent intent = new Intent(this, IDCardBackActivity.class);
        // 扫描裁剪框背景
        intent.putExtra("scanCropBgRes", R.drawable.zcamera_bg_layerl_h_cc000000_1_5_h_ffffff_1_c_5_a);
        // 主题
        intent.putExtra("title", "扫描身份证反面-测试");
        // 提示
        intent.putExtra("tip", "请将身份证反面照放入框内-测试");
        startActivityForResult(intent, 111);
    }

    // 进入身份证正面拍照
    public void onEnterIdCardFront(View view) {
        Intent intent = new Intent(this, IDCardFrontActivity.class);
        // 扫描裁剪框背景
        intent.putExtra("scanCropBgRes", R.drawable.zcamera_bg_layerl_h_cc000000_1_5_h_ffffff_1_c_5_a);
        // 主题
        intent.putExtra("title", "扫描身份证正面-测试");
        // 提示
        intent.putExtra("tip", "请将身份证正面照放入框内-测试");
        startActivityForResult(intent, 111);
    }
}
