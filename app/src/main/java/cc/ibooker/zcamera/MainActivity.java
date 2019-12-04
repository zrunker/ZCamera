package cc.ibooker.zcamera;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import cc.ibooker.zcameralib.ScanBankCardActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(cc.ibooker.zcamera.R.layout.activity_main);
    }

    public void onEnterCamera(View view) {
        Intent intent = new Intent(this, ScanBankCardActivity.class);
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
}
