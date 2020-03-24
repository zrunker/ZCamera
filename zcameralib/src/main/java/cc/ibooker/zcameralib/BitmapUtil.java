package cc.ibooker.zcameralib;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Bitmap处理工具类-压缩图片
 * 当生成一个4M的bitmap就会出现oom问题
 *
 * @author 邹峰立
 */
public class BitmapUtil {
    public static int max = 0;
    public static boolean act_bool = true;
    public static List<Bitmap> bmp = new ArrayList<>();

    // 图片SD地址 上传服务器时把图片调用下面方法压缩后 保存到临时文件夹 图片压缩后小于100KB，失真度不明显
    public static List<String> drr = new ArrayList<>();

    // 图片转变成Bitmap
    public static Bitmap revitionImageSize(String path) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(path)));

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(in, null, options);
        in.close();
        int i = 0;
        Bitmap bitmap;
        while (true) {
            if ((options.outWidth >> i <= 800) && (options.outHeight >> i <= 800)) {
                in = new BufferedInputStream(new FileInputStream(new File(path)));
                options.inSampleSize = (int) Math.pow(2.0D, i);
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeStream(in, null, options);
                break;
            }
            i += 1;
        }
        return bitmap;
    }

    /**
     * 按照质量来压缩图片，压缩到<=size
     *
     * @param image
     * @param maxSize 最大大小（K）
     * @return
     */
    public static Bitmap compressImageByQuality(Bitmap image, int maxSize) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > maxSize) { // 循环判断如果压缩后图片是否大于maxSize kb,大于继续压缩
            if (options > 0) {
                baos.reset();// 重置baos，即清空baos
                options -= 10;// 每次都减少10
                image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
            } else {
                break;
            }
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
        baos.close();
        isBm.close();
        return bitmap;
    }

    /**
     * 图片按比例大小压缩方法
     *
     * @param image
     * @param pixelW
     * @param pixelH
     * @return
     */
    public static Bitmap compressImageByRatio(Bitmap image, float pixelW, float pixelH) {
        if (image == null) {
            return null;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, os);
        if (os.toByteArray().length / 1024 > 2048) {// 判断如果图片大于2M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            os.reset();// 重置baos即清空baos
            image.compress(Bitmap.CompressFormat.PNG, 60, os);// 这里压缩60%，把压缩后的数据存放到baos中
        }
        ByteArrayInputStream is;
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        // 开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        newOpts.inPreferredConfig = Config.RGB_565;
        Bitmap bitmap;
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        // 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;// be=1表示不缩放
        if (w > h && w > pixelW) {// 如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / pixelW);
        } else if (w < h && h > pixelH) {// 如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / pixelH);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;// 设置缩放比例
        // 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        is = new ByteArrayInputStream(os.toByteArray());
        bitmap = BitmapFactory.decodeStream(is, null, newOpts);
        // 压缩好比例大小后再进行质量压缩
        // return compress(bitmap, maxSize); // 这里再进行质量压缩的意义不大，反而耗资源，删除
        return bitmap;
    }


    /**
     * 通过uri获取图片并进行比例压缩
     *
     * @param uri
     */
    public static Bitmap getBitmapFormUri(Activity ac, Uri uri) throws Exception {
        InputStream input = ac.getContentResolver().openInputStream(uri);
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither = true;//optional
        onlyBoundsOptions.inPreferredConfig = Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        if (input != null) {
            input.close();
        }
        int originalWidth = onlyBoundsOptions.outWidth;
        int originalHeight = onlyBoundsOptions.outHeight;
        if ((originalWidth == -1) || (originalHeight == -1))
            return null;
        //图片分辨率以480x800为标准
        float hh = 800f;//这里设置高度为800f
        float ww = 480f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (originalWidth > originalHeight && originalWidth > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (originalWidth / ww);
        } else if (originalWidth < originalHeight && originalHeight > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (originalHeight / hh);
        }
        if (be <= 0)
            be = 1;
        //比例压缩
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = be;//设置缩放比例
        bitmapOptions.inDither = true;//optional
        bitmapOptions.inPreferredConfig = Config.ARGB_8888;//optional
        input = ac.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        if (input != null) {
            input.close();
        }

        // 做一次质量压缩
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        if (baos.toByteArray().length / 1024 > 1024) {// 判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            baos.reset();// 重置baos即清空baos
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, baos);// 这里压缩50%，把压缩后的数据存放到baos中
        }
//        return compressImageByQuality(bitmap, 500);//再进行质量压缩(500K)
        return bitmap;
    }

    /**
     * 把流转化为Bitmap图片
     *
     * @param url
     * @return
     */
    public static Bitmap getLoacalBitmap(String url) throws Exception {
        FileInputStream fis = new FileInputStream(url);
        return BitmapFactory.decodeStream(fis); // 把流转化为Bitmap图片
    }

    /**
     * 将bitmap转换成File，写入SD卡
     */
    public Uri saveBitmap(Bitmap bitmap, String filename) {
        File tmpDir = new File(Environment.getExternalStorageDirectory() + File.separator + "YiDai");
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        File imgFile = new File(tmpDir.getAbsolutePath() + filename);
        try {
            FileOutputStream fos = new FileOutputStream(imgFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fos);// 压缩85%，数据存放到fos中
            fos.flush();
            fos.close();
            return Uri.fromFile(imgFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将bitmap转换成图片文件，写入SD卡
     */
    public static String saveBitmapToImage(Bitmap bitmap) {
        File tmpDir = new File(Environment.getExternalStorageDirectory() + File.separator + "YiDai" + File.separator + "Image");
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        String imgPath = tmpDir + File.separator + System.currentTimeMillis() + ".png";
        File imgFile = new File(imgPath);
        try {
            FileOutputStream fos = new FileOutputStream(imgFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fos);// 压缩85%，数据存放到fos中
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return imgPath;
    }

    /**
     * 转化成带删除按钮的图片
     *
     * @param x              图像的宽度
     * @param y              图像的高度
     * @param image          源图片
     * @param outerRadiusRat 圆角的大小
     * @return 圆角图片
     */
    public static Bitmap createFramedPhoto(int x, int y, Bitmap image, float outerRadiusRat) {
        // 根据源文件新建一个darwable对象
        Drawable imageDrawable = new BitmapDrawable(image);

        // 新建一个新的输出图片
        Bitmap output = Bitmap.createBitmap(x, y, Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // 新建一个矩形
        RectF outerRect = new RectF(0, 0, x, y);

        // 产生一个红色的圆角矩形
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        canvas.drawRoundRect(outerRect, outerRadiusRat, outerRadiusRat, paint);

        // 将源图片绘制到这个圆角矩形上
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        imageDrawable.setBounds(0, 0, x, y);
        canvas.saveLayer(outerRect, paint, Canvas.ALL_SAVE_FLAG);
        imageDrawable.draw(canvas);
        canvas.restore();

        return output;
    }
}
