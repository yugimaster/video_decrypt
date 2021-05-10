package wei.yuan.video_decrypt;

import android.app.Application;
import android.content.Context;

import com.arialyy.aria.core.Aria;
import com.danikula.videocache.HttpProxyCacheServer;

import wei.yuan.video_decrypt.util.CacheUtil;

public class DecryptApp extends Application {

    private static Context context;

    private HttpProxyCacheServer proxy;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        Aria.init(this);
    }

    public static Context getContext() {
        return context;
    }

    public static HttpProxyCacheServer getProxy(Context context) {
        DecryptApp app = (DecryptApp) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer.Builder(this)
                .cacheDirectory(CacheUtil.getVideoCacheDir(this))
                .build();
    }
}
