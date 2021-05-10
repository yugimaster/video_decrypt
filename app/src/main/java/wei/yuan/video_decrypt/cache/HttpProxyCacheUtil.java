package wei.yuan.video_decrypt.cache;

import com.danikula.videocache.HttpProxyCacheServer;

import wei.yuan.video_decrypt.DecryptApp;
import wei.yuan.video_decrypt.util.CacheUtil;

public class HttpProxyCacheUtil {

    private static HttpProxyCacheServer videoProxy;

    public static HttpProxyCacheServer getVideoProxy() {
        if (videoProxy == null) {
            videoProxy = new HttpProxyCacheServer.Builder(DecryptApp.getContext())
                    .cacheDirectory(CacheUtil.getMediaCacheFile(CacheUtil.VIDEO_CACHE))
                    .maxCacheSize(1024 * 1024 * 1024)   // 缓存大小
                    .fileNameGenerator(new CacheFileNameGenerator())
                    .build();
        }

        return videoProxy;
    }
}
