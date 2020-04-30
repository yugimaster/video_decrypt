package wei.yuan.video_decrypt;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import wei.yuan.video_decrypt.m3u8Download.download.M3u8DownloadFactory;
import wei.yuan.video_decrypt.m3u8Download.listener.DownloadListener;
import wei.yuan.video_decrypt.m3u8Download.utils.Constant;

public class M3u8DownloadActivity extends Activity {

    private static final String TAG = "M3u8DownloadActivity";
    private static final String M3U8URL = "http://cflstc.r18.com/r18/st1:H5lahjHlYsF6aXqYMibBOTlNe48Wkyemzh+LiVRQgFVTFws4l5cSsIH5I6-8MJ16hLt++2KCdi6hjpvRYRZj4A==/-/cdn/chunklist_b2000000.m3u8?ld=olVkBO3k%2Fwb4YuY4Y9D23iLfLhays5FWhpmcfdcWZuoaTaYOYr83XWVsOasAJxGmpUtPP%2FPnDCcbHs6Mb0j3Sl7uzP8SlPqDZRFu%2BK5LZN75KYeo5umld5k0I%2FomkBkLsIiREytPzxR9Q9N7C9m6FZnEbKrb4p2rwPA%2B3DYA8RoQJakLQK%2Bn8u6f5XHDyPKbgZRPrkMSiw5P%2F8S7JasMR%2BF2Ek7OF7w5mx3OlvWEdCv7d96c8C%2FCjRimHQBzQsMS13Pw6Fr6k0MwsJI3IlMpPm2HaJy8SO%2BcDM8xQLLKKvMAtFMNxLacKR%2FOvLti2N%2Fn";
//    private static final String M3U8URL = "https://videozmcdn.stz8.com:8091/20191127/PK7a0LKQ/index.m3u8";
    private static final String DOWNLOAD_DIR = "/dmm/agirl064";

    private EditText mEtUrl;
    private Button mDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
        setContentView(R.layout.activity_m3u8download);

        mEtUrl = (EditText) findViewById(R.id.et_url);
        mDownload = (Button) findViewById(R.id.btn_download);

        mDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadM3u8Thread();
            }
        });
    }

    private void downloadM3u8Thread() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                downloadM3u8();
            }
        };
        new Thread(runnable).start();
    }

    private void downloadM3u8() {
        Log.v(TAG, "downloadM3u8()");
        M3u8DownloadFactory.M3u8Download m3u8Download = M3u8DownloadFactory.getInstance(M3U8URL);
        //设置生成目录
        m3u8Download.setDir(Environment.getExternalStorageDirectory() + DOWNLOAD_DIR);
        //设置视频名称
        m3u8Download.setFileName("test");
        //设置线程数
        m3u8Download.setThreadCount(100);
        //设置重试次数
        m3u8Download.setRetryCount(100);
        //设置连接超时时间（单位：毫秒）
        m3u8Download.setTimeoutMillisecond(10000L);
        /*
        设置日志级别
        可选值：NONE INFO DEBUG ERROR
        */
        m3u8Download.setLogLevel(Constant.INFO);
        //设置监听器间隔（单位：毫秒）
        m3u8Download.setInterval(500L);
        //添加额外请求头
        /*  Map<String, Object> headersMap = new HashMap<>();
        headersMap.put("Content-Type", "text/html;charset=utf-8");
        m3u8Download.addRequestHeaderMap(headersMap);*/
        //添加监听器
        m3u8Download.addListener(new DownloadListener() {
            @Override
            public void start() {
                Log.v(TAG, "start downloading...");
            }

            @Override
            public void process(String downloadUrl, int finished, int sum, float percent) {
                Log.v(TAG, "download url: " + downloadUrl + "\thas downloaded: " + finished
                        + "\ttotal: " + sum + "\tcompleted: " + percent + "%");
            }

            @Override
            public void speed(String speedPerSecond) {
                Log.v(TAG, "download speed: " + speedPerSecond);
            }

            @Override
            public void end() {
                Log.v(TAG, "download finished");
            }
        });
        //开始下载
        m3u8Download.start();
    }
}
