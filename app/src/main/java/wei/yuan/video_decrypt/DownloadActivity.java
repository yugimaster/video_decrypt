package wei.yuan.video_decrypt;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.arialyy.annotations.Download;
import com.arialyy.annotations.DownloadGroup;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.task.DownloadGroupTask;
import com.arialyy.aria.core.task.DownloadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import wei.yuan.video_decrypt.util.CommonUtil;

public class DownloadActivity extends Activity implements View.OnClickListener {

    private final static String TAG = "Downloader";
    private final static String EXTRA_STORAGE = Environment.getExternalStorageDirectory().getPath();
    private final static String DOWNLOAD_URL = "https://cflstc.r18.com/r18/st1:QLStGdxAZ+3RxS4J3YJ96SR2r-hLULKOwUhNlqr9WjrHIZGJOB5ySoIJQ+Qka8Fv/-/cdn-1582504584/media_b1500000_151.ts";
    private final static String DOWNLOAD_DIRECTORY = "/dmm/swd134/ts";

    private EditText mEtDir;
    private EditText mEtUrl;
    private EditText mEtOffset;
    private Button mBtnDownload;
    private Button mBtnClear;
    private TextView mTvConsole;

    private String downloadDir;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
        setContentView(R.layout.activity_download);

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
            case R.id.btn1:
                downloadButtonClick();
                break;
            case R.id.btn2:
                clearButtonClick();
                break;
            default:
                break;
        }
    }

    @Download.onWait void onWait(DownloadTask task) {
        Log.d(TAG, "wait ==> " + task.getDownloadEntity().getFileName());
    }

    @Download.onPre protected void onPre(DownloadTask task) {
        Log.d(TAG, "onPre");
    }

    @Download.onTaskStart void taskStart(DownloadTask task) {
        Log.d(TAG, "onStart");
    }

    @Download.onTaskRunning protected void running(DownloadTask task) {
        String taskFileName = task.getDownloadEntity().getFileName();
        Log.d(TAG, taskFileName + " running");
        int p = task.getPercent();
        String speed = task.getConvertSpeed();
        long speed1 = task.getSpeed();
        Log.d(TAG, "percent: " + p + "\n" + "speed: " + speed + "\n" + "speed1: " + speed1);
    }

    @Download.onTaskResume void taskResume(DownloadTask task) {
        Log.d(TAG, "resume");
    }

    @Download.onTaskStop void taskStop(DownloadTask task) {
        Log.d(TAG, "stop");
    }

    @Download.onTaskCancel void taskCancel(DownloadTask task) {
        Log.d(TAG, "cancel");
    }

    @Download.onTaskFail void taskFail(DownloadTask task) {
        Log.d(TAG, "fail");
    }

    @Download.onTaskComplete void taskComplete(DownloadTask task) {
        Log.d(TAG, "path ==> " + task.getDownloadEntity().getFilePath());
        showDebugLog(mTvConsole, "task complete: " + task.getDownloadEntity().getFilePath());
    }

    @DownloadGroup.onPre void onPre(DownloadGroupTask task) {
        Log.v(TAG, "DownloadGroup prepare");
    }

    @DownloadGroup.onTaskStart void taskStart(DownloadGroupTask task) {
        Log.v(TAG, "DownloadGroup start");
    }

    @DownloadGroup.onTaskResume void taskResume(DownloadGroupTask task) {
        Log.v(TAG, "DownloadGroup resume");
    }

    @DownloadGroup.onTaskRunning void running(DownloadGroupTask task) {
        Log.v(TAG, "DownloadGroup running");
    }

    @DownloadGroup.onWait void onWait(DownloadGroupTask task){
        Log.v(TAG, "DownloadGroup wait");
    }

    @DownloadGroup.onTaskStop void taskStop(DownloadGroupTask task) {
        Log.v(TAG, "DownloadGroup stop");
    }

    @DownloadGroup.onTaskCancel void taskCancel(DownloadGroupTask task) {
        Log.v(TAG, "DownloadGroup cancel");
    }

    @DownloadGroup.onTaskFail void taskFail(DownloadGroupTask task) {
        String msg = "DownloadGroup fail";
        Log.v(TAG, msg);
        setSpannableString(mTvConsole, msg + "\n", "#FF0000");
        clearDownloadGroupTasks();
    }

    @DownloadGroup.onTaskComplete void taskComplete(DownloadGroupTask task) {
        String msg = "DownloadGroup complete";
        Log.v(TAG, "DownloadGroup complete");
        setSpannableString(mTvConsole, msg + "\n", "#4D8ADE");
        clearDownloadGroupTasks();
    }

    @DownloadGroup.onSubTaskRunning void onSubTaskRunning(DownloadGroupTask groupTask, DownloadEntity subEntity) {
        String fileName = subEntity.getFileName();
        String percent = subEntity.getPercent() + "%";
        String speed = String.valueOf(subEntity.getSpeed());
        Log.d(TAG, String.format("[%s]: percent %s, speed %sB/s", fileName, percent, speed));
    }

    @DownloadGroup.onSubTaskPre void onSubTaskPre(DownloadGroupTask groupTask, DownloadEntity subEntity) {
        Log.d(TAG, "[" + subEntity.getFileName() + "] prepare");
    }

    @DownloadGroup.onSubTaskStart void onSubTaskStart(DownloadGroupTask groupTask, DownloadEntity subEntity) {
        Log.d(TAG, "[" + subEntity.getFileName() + "] start");
    }

    @DownloadGroup.onSubTaskStop void onSubTaskStop(DownloadGroupTask groupTask, DownloadEntity subEntity) {
        Log.d(TAG, "[" + subEntity.getFileName() + "] stop");
    }

    @DownloadGroup.onSubTaskComplete void onSubTaskComplete(DownloadGroupTask groupTask, DownloadEntity subEntity) {
        String msg = "[" + subEntity.getFileName() + "] complete";
        Log.d(TAG, msg);
        mTvConsole.append(msg + "\n");
    }

    @DownloadGroup.onSubTaskFail void onSubTaskFail(DownloadGroupTask groupTask, DownloadEntity subEntity) {
        String msg = "[" + subEntity.getFileName() + "] fail";
        Log.d(TAG, msg);
        setSpannableString(mTvConsole, msg + "\n", "#FF0000");
    }

    private void initView() {
        mEtDir = (EditText) findViewById(R.id.et_dir);
        mEtUrl = (EditText) findViewById(R.id.et_url);
        mEtOffset = (EditText) findViewById(R.id.et_offset);
        mBtnDownload = (Button) findViewById(R.id.btn1);
        mBtnDownload.setOnClickListener(this);
        mBtnClear = (Button) findViewById(R.id.btn2);
        mBtnClear.setOnClickListener(this);
        mTvConsole = (TextView) findViewById(R.id.consoleText);

        mEtDir.setText(downloadDir);
    }

    private void downloadButtonClick() {
        Log.v(TAG, "downloadButtonClick()");
        downloadDir = mEtDir.getText().toString().replace("\n", "");
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
        url = mEtUrl.getText().toString().replace("\n", "");
        if (url.isEmpty()) {
            url = DOWNLOAD_URL;
        }
        String offset = mEtOffset.getText().toString();
        if (offset.isEmpty()) {
            Toast.makeText(getApplicationContext(), "请输入offset的值！", Toast.LENGTH_LONG).show();
            return;
        }
        mTvConsole.setText("");
        httpMultiDownloadGroup(url, offset);
    }

    private void clearButtonClick() {
        Log.v(TAG, "clearButtonClick()");
        boolean isClear = clearDownloadGroupTasks();
        if (isClear) {
            mTvConsole.append("Download Group Tasks Clear!" + "\n");
        } else {
            mTvConsole.append("Download Group Tasks is none!" + "\n");
        }
    }

    private void httpSingleDownload(String downloadUrl, String downloadName) {
        Log.v(TAG, "httpSingleDownload()");
        String path = EXTRA_STORAGE + "/dmm/" + downloadDir + "/ts/" + downloadName;
        Log.d(TAG, path);
        Aria.download(this)
                .load(downloadUrl)
                .setFilePath(path)
                .create();
    }

    private void httpMultiDownload(String url, String offset) {
        String firstFileName = generateFileName(url);
        String firstIndex = getTsIndex(firstFileName);
        String curFileName = "";
        String curUrl = "";
        for (int i = 0; i <= Integer.valueOf(offset); i++) {
            if (i == 0) {
                curFileName = firstFileName;
                curUrl = url;
            } else {
                int index = Integer.valueOf(firstIndex) + i;
                curFileName = firstFileName.replace(firstIndex + ".ts", String.valueOf(index) + ".ts");
                curUrl = url.replace(firstFileName, curFileName);
            }
            httpSingleDownload(curUrl, curFileName);
        }
    }

    private void httpMultiDownloadGroup(String url, String offset) {
        String firstFileName = generateFileName(url);
        String firstIndex = getTsIndex(firstFileName);
        if (firstIndex.isEmpty()) {
            httpSingleDownload(url, firstFileName);
            return;
        }
        List<String> urls = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        String curUrl = "";
        String curFileName = "";
        for (int i = 0; i <= Integer.valueOf(offset); i++) {
            int index = Integer.valueOf(firstIndex) + i;
            curFileName = firstFileName.replace(firstIndex + ".ts", String.valueOf(index) + ".ts");
            curUrl = url.replace(firstFileName, curFileName);
            Log.d(TAG, "index " + index + " url: " + curUrl);
            urls.add(curUrl);
            names.add(curFileName);
        }
        String groupDirPath = EXTRA_STORAGE + "/dmm/" + downloadDir + "/ts";
        File tsDir = new File(groupDirPath);
        if (!tsDir.exists()) {
            tsDir.mkdir();
        }
        long taskId = Aria.download(this)
                .loadGroup(urls)
                .setDirPath(groupDirPath)
                .setSubFileName(names)
                .unknownSize()
                .create();
        Log.d(TAG, "current task id: " + taskId);
    }

    private String generateFileName(String url) {
        String[] strings = url.split("/");
        return strings[strings.length - 1];
    }

    private String getTsIndex(String fileName) {
        try {
            String[] strs = fileName.split("\\.");
            String name = strs[0];
            String[] s = name.split("_");
            String index = s[2];
            return index;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return "";
        }
    }

    private void showDebugLog(TextView textView, String log) {
        String msg = log + "\n";
        textView.append(msg);
    }

    private void setSpannableString(TextView tv, String content, String colorString) {
        SpannableStringBuilder builder = CommonUtil.setSpannableString(content, "#4D8ADE");
        tv.append(builder);
    }

    private boolean clearDownloadGroupTasks() {
        Log.v(TAG, "clearDownloadGroupTasks()");
        boolean flag = true;
        List<DownloadGroupEntity> groupEntities = Aria.download(this).getGroupTaskList();
        if (groupEntities != null && groupEntities.size() > 0) {
            for (DownloadGroupEntity entity : groupEntities) {
                long taskId = entity.getId();
                Log.v(TAG, "DownloadGroupEntity taskId: " + taskId);
                // 取消组合任务
                Aria.download(this).loadGroup(taskId).cancel();
            }
        } else {
            Log.v(TAG, "no group tasks!");
            flag = false;
        }

        return flag;
    }
}
