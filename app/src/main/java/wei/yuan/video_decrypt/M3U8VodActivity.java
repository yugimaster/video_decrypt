package wei.yuan.video_decrypt;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.arialyy.annotations.Download;
import com.arialyy.annotations.M3U8;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadTaskListener;
import com.arialyy.aria.core.download.M3U8Entity;
import com.arialyy.aria.core.download.m3u8.M3U8VodOption;
import com.arialyy.aria.core.processor.ITsMergeHandler;
import com.arialyy.aria.core.processor.IVodTsUrlConverter;
import com.arialyy.aria.core.scheduler.M3U8PeerTaskListener;
import com.arialyy.aria.core.task.DownloadTask;
import com.arialyy.aria.util.ALog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import wei.yuan.video_decrypt.m3u8server.M3u8Server;
import wei.yuan.video_decrypt.util.CommonUtil;

public class M3U8VodActivity extends Activity implements View.OnClickListener {

    private final static String TAG = "M3U8VodActivity";
    private final static String EXTRA_STORAGE = Environment.getExternalStorageDirectory().getPath();

    private EditText mETDir;
    private EditText mETUrl;
    private Button mBtnPlay;
    private Button mBtnClear;
    private Button mBtnLocalPlay;

    private String downloadDir = "";
    private String mM3u8Url = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
        setContentView(R.layout.activity_m3u8vod);

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("Info");
        downloadDir = bundle.getString("directory");

        initView();
        // 注册aria
        Aria.download(this).register();

        M3u8Server.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");

        // 取消注册aria
        Aria.download(this).unRegister();

        M3u8Server.close();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:
                playM3U8Vod(false);
                break;
            case R.id.btn_clear:
                clearDownloadTasks();
                break;
            case R.id.btn_local_play:
                playM3U8Vod(true);
                break;
            default:
                break;
        }
    }

    @M3U8.onPeerStart
    void onPeerStart(String m3u8Url, String peerPath, int peerIndex) {
        Log.v(TAG, "peer start, path: " + peerPath + ", index: " + peerIndex);
    }

    @M3U8.onPeerComplete
    void onPeerComplete(String m3u8Url, String peerPath, int peerIndex) {
        Log.v(TAG, "peer fail, path: " + peerPath + ", index: " + peerIndex);
    }

    @M3U8.onPeerFail
    void onPeerFail(String m3u8Url, String peerPath, int peerIndex) {
        Log.v(TAG, "peer complete, path: " + peerPath + ", index: " + peerIndex);
    }

    @Download.onTaskStart
    void taskStart(DownloadTask task) {
        Log.v(TAG, "task start: " + task.getDownloadEntity().getUrl());
    }

    @Download.onTaskComplete
    void taskComplete(DownloadTask task) {
        Log.v(TAG, "task complete: " + task.getDownloadEntity().getFileName());
        String tsDownloadPath = EXTRA_STORAGE + "/dmm/" + downloadDir + "/." + downloadDir + ".m3u8_0";
        if (isFileExist(tsDownloadPath)) {
            Log.d(TAG, tsDownloadPath + " is exist");
            Log.d(TAG, "files size in ts dir is: " + getDirFilesNum(tsDownloadPath));
        }
    }

    @Download.onTaskFail
    void taskFail(DownloadTask task) {
        Log.v(TAG, "task fail: " + task.getDownloadEntity().getFileName());
    }

    private static class VodTsConverter implements IVodTsUrlConverter {
        @Override
        public List<String> convert(String m3u8Url, List<String> tsUrls) {
            // 转换ts文件的url地址
            String parentUrl = CommonUtil.getParentUrl(m3u8Url);
            List<String> newUrls = new ArrayList<>();
            for (String url : tsUrls) {
                Log.d(TAG, "ts url: " + url);
                if (url.startsWith("https://cdn-mso4.kiseouhgf.info")
                        || url.startsWith(" https://cflstc.r18.com")) {
                    newUrls.add(url);
                } else {
                    Log.d(TAG, "new ts url: " + parentUrl + url);
                    newUrls.add(parentUrl + url);
                }
            }

            // 返回有效的ts文件url集合
            return newUrls;
        }
    }

    private static class TsMergeHandler implements ITsMergeHandler {
        @Override
        public boolean merge(M3U8Entity m3U8Entity, List<String> tsPath) {
            String keyFormat = m3U8Entity.getKeyFormat();
            String keyFormatVer = m3U8Entity.getKeyFormatVersion();
            String keyIv = m3U8Entity.getIv();
            String cacheDir = m3U8Entity.getCacheDir();
            String method = m3U8Entity.getMethod();
            Log.d(TAG, "keyFormat: " + keyFormat + ", keyFormatVersion: " + keyFormatVer
                    + "\n" + "keyIv: " + keyIv + "\n" + "cacheDir: " + cacheDir + "\n" + "method: "
                    + method);
            return false;
        }
    }

    private void initView() {
        mETDir = (EditText) findViewById(R.id.et_dir);
        mETDir.setText(downloadDir);
        mETUrl = (EditText) findViewById(R.id.et_url);
        mBtnPlay = (Button) findViewById(R.id.btn_play);
        mBtnPlay.setOnClickListener(this);
        mBtnClear = (Button) findViewById(R.id.btn_clear);
        mBtnClear.setOnClickListener(this);
        mBtnLocalPlay = (Button) findViewById(R.id.btn_local_play);
        mBtnLocalPlay.setOnClickListener(this);
    }

    private void playM3U8Vod(boolean isLocal) {
        Log.v(TAG, "playM3U8Vod()");

        downloadDir = mETDir.getText().toString().trim();
        if (downloadDir.isEmpty()) {
            Toast.makeText(getApplicationContext(), "请输入下载目录！", Toast.LENGTH_LONG).show();
            return;
        }
        File fileDir = new File(EXTRA_STORAGE + "/dmm/" + downloadDir);
        Log.d(TAG, fileDir.getPath());
        if (!fileDir.exists() || !fileDir.isDirectory()) {
            Toast.makeText(getApplicationContext(), "无效的下载目录！", Toast.LENGTH_LONG).show();
            return;
        }
        if (isLocal) {
            mM3u8Url = getLocalM3u8Url(fileDir.getPath());
        } else {
            mM3u8Url = mETUrl.getText().toString().trim().replace("\n", "");
        }
        if (mM3u8Url.isEmpty()) {
            Toast.makeText(getApplicationContext(), "请输入m3u8地址！", Toast.LENGTH_LONG).show();
            return;
        }

        // m3u8点播配置
        M3U8VodOption option = new M3U8VodOption();
        // 设置密钥文件的保存路径
//        option.setKeyPath(fileDir.getPath() + "/key.key");

        /*
         * 是否使用默认转换器
         * true: 使用
         * false: 不使用
         */
        //
        option.setUseDefConvert(true);

        /*
         * 是否生成本地m3u8索引文件
         */
        option.generateIndexFile();

        /*
         * 是否合并ts文件
         * true: 合并
         * false: 不合并
         */
        option.merge(false);

        // 设置同时下载的ts分片数量
        option.setMaxTsQueueNum(10);

        /*
         * 设置启动任务时初始化索引位置
         *
         * 1、优先下载指定索引后的切片
         * 2、如果指定的切片索引大于切片总数，则此操作无效
         * 3、如果指定的切片索引小于当前正在下载的切片索引，并且指定索引和当前索引区间内有未下载的切片，
         *    则优先下载该区间的切片；否则此操作无效
         * 4、如果指定索引后的切片已经全部下载完成，但是索引前有未下载的切片，间会自动下载未下载的切片
         */
//        option.setPeerIndex(0);
        // 设置自定义TS转换器
//        option.setVodTsUrlConvert(new VodTsConverter());
//        option.setMergeHandler(new TsMergeHandler());
        // 忽略下载失败的ts文件
//        option.ignoreFailureTs();
        String lowestDir = getLowestDirName(fileDir.getPath());
        long taskId = Aria.download(this)
                .load(mM3u8Url)
                .setFilePath(fileDir.getPath() + "/" + lowestDir + ".m3u8")
                .ignoreFilePathOccupy()
                .m3u8VodOption(option)
                .create();
        Log.v(TAG, "new task id: " + taskId);
    }

    private void clearDownloadTasks() {
        Log.v(TAG, "clearDownloadTasks()");
        boolean flag = true;
        List<DownloadEntity> entities = Aria.download(this).getTaskList();
        if (entities != null && entities.size() > 0) {
            for (DownloadEntity entity : entities) {
                long taskId = entity.getId();
                Log.v(TAG, "DownloadEntity taskId: " + taskId);
                Aria.download(this).load(taskId).cancel();
            }
        } else {
            Log.v(TAG, "no tasks!");
        }
    }

    private boolean isFileExist(String path) {
        File file = new File(path);

        return file.exists();
    }

    private int getDirFilesNum(String path) {
        File file = new File(path);
        int size = 0;

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            size = files.length;
        }

        return size;
    }

    private String getLowestDirName(String downloadPath) {
        String name = "";
        int index = downloadPath.lastIndexOf("/");
        if (index == -1) {
            name = downloadPath;
        } else {
            name = downloadPath.substring(index + 1);
        }

        return name;
    }

    private String getLocalM3u8Url(String filePath) {
        String m3u8Path = filePath + File.separator + "local.m3u8";
        String localUrl = String.format("http://127.0.0.1:%d%s", M3u8Server.PORT, m3u8Path);

        return localUrl;
    }
}
