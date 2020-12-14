package wei.yuan.video_decrypt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.arialyy.annotations.DownloadGroup;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.task.DownloadGroupTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import wei.yuan.video_decrypt.activity.BaseActivity;
import wei.yuan.video_decrypt.util.CommonUtil;

public class M3U8AnalysisActivity extends BaseActivity {

    private final static String TAG = "M3U8AnalysisActivity";
    private final static String SDCARD_DIR = Environment.getExternalStorageDirectory().getPath();
    private static final String DMM_DIR = SDCARD_DIR + File.separator + "dmm";

    /**
     * 线程池
     */
    private static final Executor threadExecutor = new ThreadPoolExecutor(2, 4, 30,
            TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(10));

    private Context mContext;

    private File mTargetFile;

    private TextView mTvConsole;

    private String m3u8Dir;
    private List<String> mTsUrls;
    private List<String> mTsNames;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCreate()");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_analysis);

        mContext = getApplicationContext();

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("Info");
        String directory = bundle.getString("directory");
        m3u8Dir = DMM_DIR + File.separator + directory;
        mTsUrls = new ArrayList<String>();
        mTsNames = new ArrayList<String>();

        mTvConsole = (TextView) findViewById(R.id.consoleText);

        analysisM3U8();
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

    private Runnable analysisM3u8FileRunnable = new Runnable() {
        @Override
        public void run() {
            boolean flag = m3u8Loader();
            if (flag) {
                Log.v(TAG, "m3u8 load success");
            } else {
                Log.v(TAG, "m3u8 load failed");
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dismissLoadingDialog();
                    if (flag) {
                        openTsDownloadDialog();
                    }
                }
            });
        }
    };

    private void analysisM3U8() {
        Log.v(TAG, "analysisM3U8()");
        showLoadingDialog();
        File file = new File(m3u8Dir + File.separator + "local.m3u8");
        if (!file.exists()) {
            Log.d(TAG, file.getPath() + " doesn't exist");
            dismissLoadingDialog();
            showToastMsg(mContext, file.getPath() + " doesn't exist!");
        } else {
            mTargetFile = file;
            threadExecutor.execute(analysisM3u8FileRunnable);
        }
    }

    private boolean m3u8Loader() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(mTargetFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            String name;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#EXT")) {
                    Log.v(TAG, "do nothing");
                } else {
                    i++;
                    Log.d(TAG, "ts url: " + line);
                    name = generateFileName(line);
                    mTsUrls.add(line);
                    mTsNames.add(name);
                }
            }
            Log.d(TAG, "ts total count: " + i);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private void openTsDownloadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.Theme_AppCompat_Light_Dialog);
        builder.setTitle("当前解析 " + mTsUrls.size() + " ts下载址址");
        final String[] modes = {"Download", "Cancel"};
        builder.setItems(modes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String currentMode = modes[i];
                showToastMsg(mContext, "Mode: " + currentMode);
                if (i == 0) {
                    tsListDownload();
                }
            }
        });
        builder.show();
    }

    private void tsListDownload() {
        String tsPath = m3u8Dir + File.separator + "ts";
        File tsDir = new File(tsPath);
        if (!tsDir.exists()) {
            tsDir.mkdir();
        }
        long taskId = Aria.download(this)
                .loadGroup(mTsUrls)
                .setDirPath(tsPath)
                .setSubFileName(mTsNames)
                .unknownSize()
                .create();
        Log.d(TAG, "current task id: " + taskId);
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

    private String generateFileName(String url) {
        String[] strings = url.split("/");
        return strings[strings.length - 1];
    }

    private void setSpannableString(TextView tv, String content, String colorString) {
        SpannableStringBuilder builder = CommonUtil.setSpannableString(content, "#4D8ADE");
        tv.append(builder);
    }
}
