package wei.yuan.video_decrypt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.arialyy.annotations.M3U8;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadTaskListener;
import com.arialyy.aria.core.download.m3u8.M3U8VodOption;

import java.io.File;
import java.util.List;

public class M3U8VodActivity extends Activity implements View.OnClickListener {

    private final static String TAG = "M3U8VodActivity";
    private final static String EXTRA_STORAGE = Environment.getExternalStorageDirectory().getPath();

    private EditText mETDir;
    private EditText mETUrl;
    private Button mBtnPlay;
    private Button mBtnClear;

    private String downloadDir = "";
    private String m3u8Url = "";

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
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:
                playM3U8Vod();
                break;
            case R.id.btn_clear:
                clearDownloadTasks();
                break;
            default:
                break;
        }
    }

//    @M3U8.onPeerStart
//    void onPeerStart(String m3u8Url, String peerPath, int peerIndex) {
//        Log.v(TAG, "peer start, path: " + peerPath + ", index: " + peerIndex);
//    }

    private void initView() {
        mETDir = (EditText) findViewById(R.id.et_dir);
        mETDir.setText(downloadDir);
        mETUrl = (EditText) findViewById(R.id.et_url);
        mBtnPlay = (Button) findViewById(R.id.btn_play);
        mBtnPlay.setOnClickListener(this);
        mBtnClear = (Button) findViewById(R.id.btn_clear);
        mBtnClear.setOnClickListener(this);
    }

    private void playM3U8Vod() {
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
        m3u8Url = mETUrl.getText().toString().trim().replace("\n", "");
        if (m3u8Url.isEmpty()) {
            Toast.makeText(getApplicationContext(), "请输入m3u8地址！", Toast.LENGTH_LONG).show();
            return;
        }

        // m3u8点播配置
        M3U8VodOption option = new M3U8VodOption();
        // 设置密钥文件的保存路径
        option.setKeyPath(fileDir.getPath() + "/key.key");
        // 不合并ts文件
        option.merge(false);
        // 生成m3u8索引文件
        option.generateIndexFile();
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
        long taskId = Aria.download(this)
                .load(m3u8Url)
                .setFilePath(fileDir.getPath() + "/" + downloadDir + ".ts")
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
}
