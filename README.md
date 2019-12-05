# ZCamera
自定义相机实现扫描银行卡界面

### 一、引入ZCamera：

1. gradle引入：
```
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.zrunker:ZCamera:v1.0.2'
}
```

2. maven引入：
```
<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>

	<dependency>
	    <groupId>com.github.zrunker</groupId>
	    <artifactId>ZCamera</artifactId>
	    <version>v1.0.2</version>
	</dependency>
```
3. 直接下载工程文件：
在build.gradle中引入组件：
```
dependencies {
    implementation project(':zcameralib')
}
```

### 二、使用（推荐）：
在相应的Activity中添加如下代码：
```
1. 跳转拍照扫描：
Intent intent = new Intent(this, ScanBankCardActivity.class);
// // 扫描裁剪框背景
// intent.putExtra("scanCropBgRes", R.drawable.zcamera_bg_layerl_h_87000000_2_5_h_fa3a00_2_c_10_a);
// // 主题
// intent.putExtra("title", "扫描银行卡正面");
// // 提示
// intent.putExtra("tip", "将银行卡卡号面放在此区域，扫描卡片");
startActivityForResult(intent, 111);

2. 监听返回：
@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK) {
        if (requestCode == 111 && data != null) {
            // 返回扫描图片地址
            String filePath = data.getStringExtra("filePath");
            Toast.makeText(this, filePath, Toast.LENGTH_SHORT).show();
        }
    }
}
```
### 最终效果图：
![扫描银行卡效果图](https://github.com/zrunker/ZCamera/blob/master/device-2019-12-05-103721.png)
