package wei.yuan.video_decrypt.cache;

import android.net.Uri;
import android.util.Log;

import java.util.List;

import com.danikula.videocache.file.FileNameGenerator;

public class CacheFileNameGenerator implements FileNameGenerator {

    private static final String TAG = "CacheFileNameGenerator";

    /**
     *
     * @param url
     * @return
     */
    @Override
    public String generate(String url) {
        Log.d(TAG, "url: " + url);
        Uri uri = Uri.parse(url);
        List<String> pathSegList = uri.getPathSegments();
        String path = null;
        if (pathSegList != null && pathSegList.size() > 0) {
            path = pathSegList.get(pathSegList.size() - 1);
            String s = pathSegList.toString();
            Log.d(TAG, "path seg list: " + s);
            if (s.contains("storage") && s.contains("emulated") && pathSegList.size() > 4) {
                path = pathSegList.get(4) + pathSegList.get(5) + ".m3u8";
            }
        } else {
            path = url;
        }
        Log.v(TAG, "generate return " + path);
        return path;
    }
}
