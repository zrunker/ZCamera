package cc.ibooker.zcameralib;

/**
 * 点击事件处理
 *
 * @author 邹峰立
 */
public class ClickUtil {
    private static long lastClickTime;

    // 判断是否为快速点击事件
    public synchronized static boolean isFastClick() {
        long time = System.currentTimeMillis();
        if (time - lastClickTime < 500)
            return true;
        lastClickTime = time;
        return false;
    }
}