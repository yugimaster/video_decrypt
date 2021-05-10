package wei.yuan.video_decrypt.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import wei.yuan.video_decrypt.DecryptApp;

public class CacheUtil {

    private final static String TAG = "CacheUtil";

    public final static String VIDEO_CACHE = "videoCache";

    /**
     * 获取视频缓冲目录
     * @param context
     * @return
     */
    public static File getVideoCacheDir(Context context) {
        return new File(context.getExternalCacheDir(), VIDEO_CACHE);
    }

    /**
     * 获取媒体缓存文件
     * @param child
     * @return
     */
    public static File getMediaCacheFile(String child) {
        String directoryPath = "";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 外部储存可用
            directoryPath = DecryptApp.getContext().getExternalFilesDir(child).getAbsolutePath();
        } else {
            directoryPath = DecryptApp.getContext().getFilesDir().getAbsolutePath() + File.separator
                    + child;
        }
        File file = new File(directoryPath);
        // 判断文件目录是否存在
        if (!file.exists()) {
            file.mkdirs();
        }
        Log.d(TAG, "getMediaCacheFile ====> " + directoryPath);
        return file;
    }
}
