package wei.yuan.video_decrypt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import wei.yuan.video_decrypt.activity.BaseActivity;
import wei.yuan.video_decrypt.util.CommonUtil;
import wei.yuan.video_decrypt.view.DownloadProgressBar;

public class M3U8AnalysisActivity extends BaseActivity implements View.OnClickListener {

    private final static String TAG = "M3U8AnalysisActivity";
    private final static String SDCARD_DIR = Environment.getExternalStorageDirectory().getPath();
    private static final String DMM_DIR = SDCARD_DIR + File.separator + "dmm";

    /**
     * 线程池
     */
    private static final Executor threadExecutor = new ThreadPoolExecutor(2, 4, 30,
            TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(10));

    private Context mContext;

    private Timer mTimerDownload;
    private TimerTask mTimerTaskWait;

    private File mTargetFile;

    private TextView mTvConsole;
    private TextView mTvCount;
    private TextView mTvSpeed;
    private TextView mTvZero;
    private TextView mTvInfo;
    private Button mBtnAnalysis;
    private Button mBtnCombine;
    private Button mBtnCancel;
    private Button mBtnDeduplicate;

    private RelativeLayout mProgressLayout;
    private DownloadProgressBar mProgressBar;

    private String m3u8Dir;
    private String mSpeed = "1";
    private List<String> mTsUrls;
    private List<String> mTsNames;
    private List<String> mDownloadUrls;
    private List<String> mDownloadNames;

    private long mTaskId = -1;

    private int tsTotalCount = 0;
    private int tsZeroCount = 0;
    private int tsDownloadedCount = 0;
    // 当前下载任务中已下载ts个数
    private int tsCurrentCount = 0;
    // 当前下载任务中总ts个数
    private int tsDownloadTotal = 0;
    // 当前下载任务中ts大小为0个数
    private int tsDownloadZero = 0;

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
        mDownloadUrls = new ArrayList<String>();
        mDownloadNames = new ArrayList<String>();

        initView();

        // 注册aria
        Aria.download(this).register();
        clearDownloadGroupTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.v(TAG, "onStart()");
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

        clearArrayLists();
        resetDownloadValues();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_analysis:
                Log.v(TAG, "start analysis m3u8");
                analysisM3U8();
                break;
            case R.id.btn_combine:
                Log.v(TAG, "start combine ts");
                combineTsFiles();
                break;
            case R.id.btn_cancel:
                Log.v(TAG, "cancel current download group task");
                cancelDownloadGroupTask(mTaskId);
                break;
            case R.id.btn_deduplicate:
                Log.v(TAG, "delete duplicated files");
                gotoDeduplication();
                break;
            default:
                break;
        }
    }

    @DownloadGroup.onPre void onPre(DownloadGroupTask task) {
        Log.v(TAG, "DownloadGroup prepare");
        openDownloadProgressBar();
    }

    @DownloadGroup.onTaskStart void taskStart(DownloadGroupTask task) {
        Log.v(TAG, "DownloadGroup start");
    }

    @DownloadGroup.onTaskResume void taskResume(DownloadGroupTask task) {
        Log.v(TAG, "DownloadGroup resume");
    }

    @DownloadGroup.onTaskRunning void running(DownloadGroupTask task) {
        Log.v(TAG, "DownloadGroup running");
        mSpeed = String.valueOf(task.getSpeed());
    }

    @DownloadGroup.onWait void onWait(DownloadGroupTask task){
        Log.v(TAG, "DownloadGroup wait");
    }

    @DownloadGroup.onTaskStop void taskStop(DownloadGroupTask task) {
        Log.v(TAG, "DownloadGroup stop");
    }

    @DownloadGroup.onTaskCancel void taskCancel(DownloadGroupTask task) {
        Log.v(TAG, "DownloadGroup cancel");
        cancelDownloadGroupTask(mTaskId);
        setSpannableString(mTvConsole, "DownloadGroup cancel" + "\n", "#4D8ADE");
        mTaskId = -1;
    }

    @DownloadGroup.onTaskFail void taskFail(DownloadGroupTask task) {
        String msg = "DownloadGroup fail";
        Log.v(TAG, msg);
        cancelDownloadGroupTask(mTaskId);
        setSpannableString(mTvConsole, msg + "\n", "#FF0000");
        mTaskId = -1;
    }

    @DownloadGroup.onTaskComplete void taskComplete(DownloadGroupTask task) {
        String msg = "DownloadGroup complete";
        Log.v(TAG, msg);
        setSpannableString(mTvConsole, msg + "\n", "#4D8ADE");
        mTaskId = -1;
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
        tsCurrentCount += 1;
        updateDownloadProgressBar();
        Log.d(TAG, msg);
        mTvConsole.append(msg + "\n");
        String srcPath = m3u8Dir + File.separator + "ts" + File.separator + subEntity.getFileName();
        File zeroDir = new File(m3u8Dir + File.separator + "0");
        if (!zeroDir.exists()) {
            zeroDir.mkdirs();
        }
        String tarPath = zeroDir.getPath() + File.separator + subEntity.getFileName();
        moveFileToZeroDir(srcPath, tarPath);
    }

    @DownloadGroup.onSubTaskFail void onSubTaskFail(DownloadGroupTask groupTask, DownloadEntity subEntity) {
        String msg = "[" + subEntity.getFileName() + "] fail";
        Log.d(TAG, msg);
    }

    private Runnable analysisM3u8FileRunnable = new Runnable() {
        @Override
        public void run() {
            clearArrayLists();
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

    private Runnable combineTsFilesRunnable = new Runnable() {
        @Override
        public void run() {
            boolean flag = combineTsToOutput(mTargetFile);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (flag) {
                        setSpannableString(mTvConsole, "combine ts success\n", "#4D8ADE");
                    } else {
                        setSpannableString(mTvConsole, "combine ts failed\n", "#FF0000");
                    }
                    dismissLoadingDialog();
                }
            });
        }
    };

    private void initView() {
        mTvConsole = (TextView) findViewById(R.id.consoleText);
        mBtnAnalysis = (Button) findViewById(R.id.btn_analysis);
        mBtnCombine = (Button) findViewById(R.id.btn_combine);
        mBtnCancel = (Button) findViewById(R.id.btn_cancel);
        mBtnDeduplicate = (Button) findViewById(R.id.btn_deduplicate);
        mProgressLayout = (RelativeLayout) findViewById(R.id.rl_progress);
        mProgressBar = (DownloadProgressBar) findViewById(R.id.download_progress_bar);
        mTvCount = (TextView) findViewById(R.id.tv_count);
        mTvSpeed = (TextView) findViewById(R.id.tv_speed);
        mTvZero = (TextView) findViewById(R.id.tv_zero);
        mTvInfo = (TextView) findViewById(R.id.tv_info);
        mBtnAnalysis.setOnClickListener(this);
        mBtnCombine.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
        mBtnDeduplicate.setOnClickListener(this);
        mProgressLayout.setVisibility(View.GONE);
    }

    private void analysisM3U8() {
        Log.v(TAG, "analysisM3U8()");
        showLoadingDialog();
        File file = new File(m3u8Dir + File.separator + "local.m3u8");
        if (!file.exists()) {
            Log.d(TAG, file.getPath() + " doesn't exist");
            dismissLoadingDialog();
            showToastMsg(mContext, file.getPath() + " doesn't exist!");
        } else {
            tsTotalCount = 0;
            tsDownloadedCount = 0;
            tsZeroCount = 0;
            mTargetFile = file;
            threadExecutor.execute(analysisM3u8FileRunnable);
        }
    }

    private void combineTsFiles() {
        Log.v(TAG, "combineTsFiles()");
        showLoadingDialog();
        File tsDir = new File(m3u8Dir + File.separator + "ts");
        if (!tsDir.exists() || !tsDir.isDirectory()) {
            dismissLoadingDialog();
            showToastMsg(mContext, tsDir.getPath() + " doesn't exist!");
        } else {
            mTargetFile = tsDir;
            threadExecutor.execute(combineTsFilesRunnable);
        }
    }

    private boolean combineTsToOutput(File target) {
        File outputDir = new File(m3u8Dir + File.separator + "output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        List<File> files = Arrays.asList(target.listFiles());
        if (files.size() == 0) {
            return false;
        }
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                if (o1.isFile() && o2.isDirectory())
                    return 1;
                if (o1.getName().length() < o2.getName().length())
                    return -1;
                if (o1.getName().length() > o2.getName().length())
                    return 1;
                return o1.getName().compareTo(o2.getName());
            }
        });
        int total = files.size();
        File output = new File(outputDir + File.separator + "all.wmv");
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            byte[] tsData = CommonUtil.readBinaryFile(file);
            if (tsData == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDebugLog(mTvConsole, file.getName() + " is null");
                    }
                });
                total -= 1;
                continue;
            }
            boolean isSuccess = CommonUtil.writeBinaryFileAppend(tsData, output, mTvConsole);
            if (isSuccess) {
                float percent = Float.valueOf(String.valueOf(i + 1)) * 100 / Float.valueOf(String.valueOf(files.size()));
                String msg = String.format("Combinig %s + into data......\ntotal: %d\npercent: %.1f",
                        file.getName(), total, percent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDebugLog(mTvConsole, msg);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDebugLog(mTvConsole, "Combine " + file.getName() + " failed");
                    }
                });
                total -= 1;
            }
        }
        Log.v(TAG, "Combine finished");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SpannableStringBuilder builder = CommonUtil.setSpannableString("Combine finished\n", "#4D8ADE");
                mTvConsole.append(builder);
            }
        });
        return true;
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
                    int d = isTsFileDownloaded(name);
                    if (d == 1) {
                        tsDownloadedCount += 1;
                    } else if (d == 2) {
                        tsZeroCount += 1;
                    }
                }
            }
            tsTotalCount = i;
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
        showAnalysisMsg();
        builder.setTitle("当前解析 " + tsTotalCount + " ts下载址址");
        final String[] modes = {getString(R.string.download_now), getString(R.string.cancel)};
        builder.setItems(modes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String currentMode = modes[i];
                showToastMsg(mContext, "Mode: " + currentMode);
                if (i == 0) {
                    mTvConsole.setText("");
                    resetDownloadValues();
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
        for (int i = 0; i < mTsNames.size(); i++) {
            String url = mTsUrls.get(i);
            String name = mTsNames.get(i);
            File tsFile = new File(m3u8Dir + File.separator + "ts" + File.separator + name);
            if (!tsFile.exists()) {
                mDownloadUrls.add(url);
                mDownloadNames.add(name);
            } else if (tsFile.length() == 0) {
                tsFile.delete();
                mDownloadUrls.add(url);
                mDownloadNames.add(name);
            }
        }
        Log.v(TAG, "download urls size: " + mDownloadUrls.size());
        Log.v(TAG, "download names size: " + mDownloadNames.size());
        tsDownloadTotal = mDownloadUrls.size();
        long taskId = Aria.download(this)
                .loadGroup(mDownloadUrls)
                .setDirPath(tsPath)
                .setSubFileName(mDownloadNames)
                .unknownSize()
                .ignoreTaskOccupy()
                .create();
        Log.d(TAG, "current task id: " + taskId);
        mTaskId = taskId;
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

    private void cancelDownloadGroupTask(long taskId) {
        Log.v(TAG, "DownloadGroup taskId: " + taskId);
        if (taskId == -1) {
            setSpannableString(mTvConsole, "invalid task id!" + "\n", "#FF0000");
            return;
        }
        Aria.download(this).loadGroup(taskId).cancel();
    }

    private String generateFileName(String url) {
        String[] strings = url.split("/");
        return strings[strings.length - 1];
    }

    private void setSpannableString(TextView tv, String content, String colorString) {
        SpannableStringBuilder builder = CommonUtil.setSpannableString(content, colorString);
        tv.append(builder);
    }

    private void showAnalysisMsg() {
        int remain = tsTotalCount - tsDownloadedCount - tsZeroCount;
        mTvConsole.append("====================\n总ts文件 ");
        setSpannableString(mTvConsole, String.valueOf(tsTotalCount), "#4D8ADE");
        mTvConsole.append(" 个\n已下载 ");
        setSpannableString(mTvConsole, String.valueOf(tsDownloadedCount), "#00FF00");
        mTvConsole.append(" 个\n大小为0的 ");
        setSpannableString(mTvConsole, String.valueOf(tsZeroCount), "#FF0000");
        mTvConsole.append(" 个\n未下载 ");
        setSpannableString(mTvConsole, String.valueOf(remain), "#BEBEBE");
        mTvConsole.append(" 个\n====================\n");
    }

    /**
     * 判断文件是否下载
     * @param fileName
     * @return int
     * 0 未下载
     * 1 已下载
     * 2 文件大小为0
     */
    private int isTsFileDownloaded(String fileName) {
        File tsFile = new File(m3u8Dir + File.separator + "ts" + File.separator + fileName);
        File zeroFile = new File(m3u8Dir + File.separator + "0" + File.separator + fileName);
        if (tsFile.exists()) {
            if (tsFile.length() == 0) {
                return 2;
            } else {
                return 1;
            }
        } else if (zeroFile.exists()) {
            return 2;
        }

        return 0;
    }

    private void showDebugLog(TextView textView, String log) {
        Log.d(TAG, log);
        String msg = log + "\n";
        textView.append(msg);
    }

    private void clearArrayLists() {
        if (mTsUrls != null && mTsUrls.size() > 0) {
            mTsUrls.clear();
        }
        if (mTsNames != null && mTsNames.size() > 0) {
            mTsNames.clear();
        }
        if (mDownloadUrls != null && mDownloadUrls.size() > 0) {
            mDownloadUrls.clear();
        }
        if (mDownloadNames != null && mDownloadNames.size() > 0) {
            mDownloadNames.clear();
        }
    }

    private void resetDownloadValues() {
        tsTotalCount = 0;
        tsZeroCount = 0;
        tsDownloadedCount = 0;
        tsCurrentCount = 0;
        tsDownloadZero = 0;
        tsDownloadTotal = 0;
    }

    private void moveFileToZeroDir(String srcPath, String targetPath) {
        File source = new File(srcPath);
        File target = new File(targetPath);
        threadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (source.exists()) {
                    if (source.length() == 0) {
                        Log.d(TAG, source.getPath() + " copy to the zero dir...");
                        boolean flag = CommonUtil.fileCopy(source, target);
                        if (flag) {
                            Log.d(TAG, "copy success");
                            source.delete();
                            // 记录下载的ts为0的个数
                            tsDownloadZero += 1;
                        } else {
                            Log.d(TAG, "copy failed");
                        }
                    } else {
                        Log.d(TAG, source.getPath() + " not need to copy to the zero dir!");
                    }
                } else {
                    Log.d(TAG, source.getPath() + " doesn't exist");
                }
            }
        });
    }

    private void gotoDeduplication() {
        threadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                deduplication();
            }
        });
    }

    /**
     * ts文件去重
     */
    private void deduplication() {
        File zeroDir = new File(m3u8Dir + File.separator + "0");
        if (!zeroDir.exists() || !zeroDir.isDirectory()) {
            Log.v(TAG, zeroDir.getPath() + " not exist!");
            return;
        }
        File[] zeroFiles = zeroDir.listFiles();
        if (zeroFiles == null || zeroFiles.length == 0) {
            Log.v(TAG, zeroDir.getPath() + " is empty!");
            return;
        }
        File tsDir = new File(m3u8Dir + File.separator + "ts");
        if (!tsDir.exists() || !tsDir.isDirectory()) {
            Log.v(TAG, tsDir.getPath() + " not exist!");
            return;
        }
        File[] tsFiles = tsDir.listFiles();
        if (tsFiles == null || tsFiles.length == 0) {
            Log.v(TAG, tsDir.getPath() + " is empty!");
            return;
        }
        int count = 0;
        List<String> zeroFileNames = new ArrayList<String>();
        for (File zeroFile : zeroFiles) {
            zeroFileNames.add(zeroFile.getName());
        }
        for (File tsFile : tsFiles) {
            String name = tsFile.getName();
            if (zeroFileNames.contains(name)) {
                File file = new File(zeroDir, name);
                file.delete();
                count += 1;
            }
        }

        final int total = count;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                openDeduplicationResultDialog(total);
            }
        });
    }

    private void openDeduplicationResultDialog(int count) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                R.style.Theme_AppCompat_Light_Dialog);
        builder.setTitle("去重 " + count + " 个文件");
        final String[] modes = {getString(R.string.ok)};
        builder.setItems(modes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.show();
    }

    private void openDownloadProgressBar() {
        Log.v(TAG, "openDownloadProgressBar()");
        mProgressLayout.setVisibility(View.VISIBLE);
        mTvInfo.setText("已下载/0大小/总个数");
        updateDownloadProgressBar();
    }

    private void updateDownloadProgressBar() {
        // 更新下载进度
        String s1 = String.format("%d/<font color=\"#FF0000\">%d</font>/%d",
                tsCurrentCount, tsDownloadZero, tsDownloadTotal);
        String s2 = CommonUtil.sizeConvertFormat(Long.parseLong(mSpeed)) + "/s";
        mTvCount.setText(Html.fromHtml(s1));
        mTvSpeed.setText(s2);
        mProgressBar.setTotalValue(tsDownloadTotal);
        mProgressBar.setCurrentValue(tsCurrentCount);
    }

    /**
     * 统计大小为0的ts个数
     */
    private void addZeroTsCount() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String s = "0 TS File: " + tsDownloadZero;
                mTvZero.setText(s);
            }
        });
    }

    private void startDownloadWaitTimer() {
        mTimerDownload = new Timer();
        mTimerTaskWait = new TimerTask() {
            @Override
            public void run() {

            }
        };
    }
}
