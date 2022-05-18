package wei.yuan.video_decrypt;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.arialyy.aria.core.Aria;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import wei.yuan.video_decrypt.activity.BaseActivity;
import wei.yuan.video_decrypt.exoplayer.DefaultViewActivity;
import wei.yuan.video_decrypt.exoplayer.SeniorCustomViewActivity;
import wei.yuan.video_decrypt.exoplayer.SimpleCustomViewActivity;
import wei.yuan.video_decrypt.m3u8server.M3u8Server;
import wei.yuan.video_decrypt.util.AESUtil;
import wei.yuan.video_decrypt.util.CommonUtil;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final String EXTERNAL_STORAGE = "/mnt/sdcard/dmm";

    /**
     * 线程池
     */
    private static final Executor threadExecutor = new ThreadPoolExecutor(2, 4, 30,
            TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(10));

    private EditText mEt;
    private ScrollView mScrollView;
    private TextView mTvConsole;
    private Button mBtnM3U8;
    private Button mBtnBrowserDownload;
    private Button mBtnDownload;
    private Button mBtnCombine;
    private Button mBtnDecrypt;
    private Button mBtnPlay;
    private Button mBtnVideoPlayer;
    private Button mBtnAnalysis;
    private Button mBtnExoDefault;
    private Button mBtnExoSimple;
    private Button mBtnExoSenior;

    private BroadcastReceiver mOtgReceiver;
    private File mSrcDir;

    private List<String> extraStorageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        mEt = (EditText) findViewById(R.id.et_path);
        mScrollView = (ScrollView) findViewById(R.id.scroller);
        mTvConsole = (TextView) findViewById(R.id.consoleText);
        mBtnM3U8 = (Button) findViewById(R.id.btn1);
        mBtnM3U8.setOnClickListener(this);
        mBtnBrowserDownload = (Button) findViewById(R.id.btn11);
        mBtnBrowserDownload.setOnClickListener(this);
        mBtnDownload = (Button) findViewById(R.id.btn2);
        mBtnDownload.setOnClickListener(this);
        mBtnCombine = (Button) findViewById(R.id.btn3);
        mBtnCombine.setOnClickListener(this);
        mBtnDecrypt = (Button) findViewById(R.id.btn4);
        mBtnDecrypt.setOnClickListener(this);
        mBtnPlay = (Button) findViewById(R.id.btn5);
        mBtnPlay.setOnClickListener(this);
        mBtnVideoPlayer = (Button) findViewById(R.id.btn6);
        mBtnVideoPlayer.setOnClickListener(this);
        mBtnAnalysis = (Button) findViewById(R.id.btn7);
        mBtnAnalysis.setOnClickListener(this);
        mBtnExoDefault = (Button) findViewById(R.id.btn8);
        mBtnExoDefault.setOnClickListener(this);
        mBtnExoSimple = (Button) findViewById(R.id.btn9);
        mBtnExoSimple.setOnClickListener(this);
        mBtnExoSenior = (Button) findViewById(R.id.btn10);
        mBtnExoSenior.setOnClickListener(this);

        extraStorageList = CommonUtil.getSdcardPaths();
        Log.d(TAG, "extra storage length: " + extraStorageList.size());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOtgReceiver != null) {
            this.unregisterReceiver(mOtgReceiver);
            mOtgReceiver = null;
        }
        M3u8Server.close();
    }

    @Override
    public void onClick(View v) {
        String path = mEt.getText().toString().replace("\n", "");
        if (path.isEmpty()) {
            Toast.makeText(getApplicationContext(), "请输入文件目录！", Toast.LENGTH_LONG).show();
            return;
        }
        switch (v.getId()) {
            case R.id.btn1:
                Log.v(TAG, "start local m3u8 activity");
                startActivity(LocalM3u8Activity.class.getName(), path);
                break;
            case R.id.btn2:
                Log.v(TAG, "start download activity");
                startActivity(DownloadActivity.class.getName(), path);
                break;
            case R.id.btn3:
                mTvConsole.setText("");
                combineTstoFile();
                break;
            case R.id.btn4:
                mTvConsole.setText("");
                decryptTsFiles();
                break;
            case R.id.btn5:
                Log.v(TAG, "start m3u8 vod activity");
                startActivity(M3U8VodActivity.class.getName(), path);
                break;
            case R.id.btn6:
                Log.v(TAG, "use system video player");
                openVideoPlayer(path);
                break;
            case R.id.btn7:
                Log.v(TAG, "m3u8 analysis activity");
                startActivity(M3U8AnalysisActivity.class.getName(), path);
                break;
            case R.id.btn8:
                Log.v(TAG, "exo player default activity");
                startActivity(DefaultViewActivity.class.getName(), path);
                break;
            case R.id.btn9:
                Log.v(TAG, "exo player simple custom activity");
                startActivity(SimpleCustomViewActivity.class.getName(), path);
                break;
            case R.id.btn10:
                Log.v(TAG, "exo player senior custom activity");
                startActivity(SeniorCustomViewActivity.class.getName(), path);
                break;
            case R.id.btn11:
                Log.v(TAG, "open browser to download url");
                openBrowserDownload(this, path);
                break;
            default:
                break;
        }
    }

    private Runnable decryptTsFilesRunnable = new Runnable() {
        @Override
        public void run() {
            decryptTsToTemp(mSrcDir);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dismissLoadingDialog();
                }
            });
        }
    };

    private void startActivity(String className, String path) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Bundle bundle = new Bundle();
        bundle.putString("directory", path);
        intent.setClassName(getApplicationContext(), className);
        intent.putExtra("Info", bundle);
        startActivity(intent);
    }

    private void showDebugLog(TextView textView, String log) {
        Log.d(TAG, log);
        String msg = log + "\n";
        textView.append(msg);
    }

    private String[] getUsbList() {
        File usbFolder = new File("/storage/");
        if( !usbFolder.exists() || !usbFolder.isDirectory() ) {
            Log.v(TAG, "getUsbList(), usbFolder invalid!");
            return null;
        }

        File[] usbList = usbFolder.listFiles();
        if( usbList == null || usbList.length == 0 ) {
            Log.v(TAG, "usbList invalid!");
            return null;
        }

        List<String> fileList = new ArrayList<String>();
        for( File usb : usbList ) {
            if( !usb.isDirectory() ) {
                continue;
            }
            showDebugLog(mTvConsole, "" + usb);
            if( usb.getName().equals("self") || usb.getName().equals("emulated") ) {
                continue;
            }

            fileList.add(usb.getName());
        }
        if( fileList == null || fileList.isEmpty() ) {
            return null;
        }

        int index = 0;
        String[] usbDevices = new String[fileList.size()];
        for(String file: fileList) {
            usbDevices[index] = file;
            index++;
        }
        return usbDevices;
    }

    private void decryptSingleTs(File tsFile, String keyHex, String ivHex, String fileName, File srcDir) {
        showDebugLogOnUIThread("decryptSingleTs()");
        byte[] data = CommonUtil.readBinaryFile(tsFile);
        if (data == null) {
            showDebugLogOnUIThread(tsFile.getAbsolutePath() + " is null");
            return;
        }
        byte[] newData = AESUtil.decrypt(data, keyHex, ivHex);
        if (newData == null) {
            showDebugLogOnUIThread("ts file is invalid");
            return;
        }
        String name = fileName.replace(".ts", "");
        File tempDir = new File(srcDir, "temp");
        if (!tempDir.exists()) {
            showDebugLogOnUIThread("make temp directory");
            tempDir.mkdirs();
        }
        File tempFile = new File(tempDir, name + "_temp.ts");
        boolean isSaved = CommonUtil.writeBinaryFile(newData, tempFile, mTvConsole);
        if (isSaved) {
            showDebugLogOnUIThread("ts file decrypts success");
        } else {
            showDebugLogOnUIThread("ts file decrypts failed");
        }
    }

    private void combineTstoFile() {
        Log.v(TAG, "combineTstoFile()");
        File storage = new File(EXTERNAL_STORAGE);
        if (!storage.exists() || !storage.isDirectory()) {
            Log.v(TAG, "storage invalid!");
            return;
        }
        String path = mEt.getText().toString().replace("\n", "");
        if (path.isEmpty()) {
            Toast.makeText(getApplicationContext(), "请输入文件目录！", Toast.LENGTH_LONG).show();
            return;
        }
        File srcDir = new File(storage, path);
        File tempDir = new File(srcDir, "temp");
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            Toast.makeText(getApplicationContext(), "文件目录不存在！", Toast.LENGTH_LONG).show();
            return;
        }
        File outputDir = new File(srcDir, "output");
        if (!outputDir.exists()) {
            showDebugLog(mTvConsole, "make output directory");
            outputDir.mkdirs();
        }
        List<File> files = Arrays.asList(tempDir.listFiles());
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
        Log.d(TAG, "temp directory files: ");
        int total = files.size();
        String dir = mEt.getText().toString().replace("\n", "");
        File output = createOutputFile(outputDir, dir, files);
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            byte[] tsData = CommonUtil.readBinaryFile(file);
            if (tsData == null) {
                showDebugLog(mTvConsole, file.getName() + " is null");
                total -= 1;
                continue;
            }
            boolean isSuccess = CommonUtil.writeBinaryFileAppend(tsData, output, mTvConsole);
            if (isSuccess) {
                float percent = Float.valueOf(String.valueOf(i + 1)) * 100 / Float.valueOf(String.valueOf(files.size()));
                String msg = String.format("Combinig %s + into data......\ntotal: %d\npercent: %.1f",
                        file.getName(), total, percent);
                showDebugLog(mTvConsole, msg);
            } else {
                showDebugLog(mTvConsole, "Combine " + file.getName() + " failed");
                total -= 1;
            }
        }
        Log.v(TAG, "Combine finished");
        SpannableStringBuilder builder = CommonUtil.setSpannableString("Combine finished\n", "#4D8ADE");
        mTvConsole.append(builder);
    }

    private String getKeyHex(File keyFile) {
        try {
            FileInputStream fileInputStream = new FileInputStream(keyFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            char[] input = new char[fileInputStream.available()];
            inputStreamReader.read(input);
            inputStreamReader.close();
            fileInputStream.close();
            return new String(input);
        } catch (Exception e) {
            showDebugLogOnUIThread(e.toString());
            return "";
        }
    }

    private String getIvHex(String fileName) {
        Log.d(TAG, "file name: " + fileName);
        showDebugLogOnUIThread("file name: " + fileName);
        String tsIndex = getTsIndex(fileName);
        showDebugLogOnUIThread("ts index: " + tsIndex);
        int index = Integer.valueOf(tsIndex);
        return String.format("%032x", index);
    }

    private void decryptTsToTemp(File srcDir) {
        File keyFile = new File(srcDir, "key_o.key");
        if (keyFile.exists()) {
            showDebugLogOnUIThread(keyFile.getAbsolutePath() + " exists");
            try {
                String keyHexStr = getKeyHex(keyFile);
                showDebugLogOnUIThread("key hex str: " + keyHexStr);
                File tsDir = new File(srcDir, "ts");
                if (!tsDir.exists()) {
                    showDebugLogOnUIThread("ts directory doesn't exist!");
                    return;
                }
                File[] files = tsDir.listFiles();
                if (files.length > 0) {
                    for (int i = 0; i < files.length; i++) {
                        File f = files[i];
                        String name = f.getName();
                        String ivHexStr = getIvHex(name);
                        showDebugLogOnUIThread("iv hex str: " + ivHexStr);
                        decryptSingleTs(f, keyHexStr, ivHexStr, name, srcDir);
                    }
                } else {
                    showDebugLogOnUIThread("ts directory is empty!");
                }
            } catch (Exception e) {
                showDebugLogOnUIThread(e.toString());
            }
        } else {
            showDebugLogOnUIThread(keyFile.getAbsolutePath() + " doesn't exist");
        }

        debugLogAppendOnUIThread("Decrypt ts finished\n", "#4D8ADE");
    }

    private void showExternalStorageFiles(File srcDir) {
        File keyFile = new File(srcDir, "key_o.key");
        if (keyFile.exists()) {
            showDebugLog(mTvConsole, keyFile.getAbsolutePath() + " exists");
            try {
                String keyHexStr = getKeyHex(keyFile);
                showDebugLog(mTvConsole, "key hex str: " + keyHexStr);
                File tsDir = new File(srcDir, "ts");
                if (!tsDir.exists()) {
                    showDebugLog(mTvConsole, "ts directory doesn't exist!");
                    return;
                }
                File[] files = tsDir.listFiles();
                if (files.length > 0) {
                    for (int i = 0; i < files.length; i++) {
                        File f = files[i];
                        String name = f.getName();
                        String ivHexStr = getIvHex(name);
                        showDebugLog(mTvConsole, "iv hex str: " + ivHexStr);
                        decryptSingleTs(f, keyHexStr, ivHexStr, name, srcDir);
                    }
                } else {
                    showDebugLog(mTvConsole, "ts directory is empty!");
                }
            } catch (Exception e) {
                showDebugLog(mTvConsole, e.toString());
            }
        } else {
            showDebugLog(mTvConsole, keyFile.getAbsolutePath() + " doesn't exist");
        }

        SpannableStringBuilder builder = CommonUtil.setSpannableString("Decrypt ts finished\n", "#4D8ADE");
        mTvConsole.append(builder);
    }

    private String getTsIndex(String fileName) {
        try {
            String[] strs = fileName.split("\\.");
            String name = strs[0];
            int pos = name.indexOf("_");
            if (pos == -1) {
                return name;
            }
            String[] s = name.split("_");
            String index = s[s.length - 1];
            return index;
        } catch (Exception e) {
            showDebugLogOnUIThread("getTsIndex()" + "\n" + e.toString());
            return "";
        }
    }

    private void decryptTsFiles() {
        Log.v(TAG, "decryptTsFiles()");
        showLoadingDialog();
        File storage = new File(EXTERNAL_STORAGE);
        if (!storage.exists() || !storage.isDirectory()) {
            Log.v(TAG, "storage invalid!");
            dismissLoadingDialog();
            return;
        }
        String path = mEt.getText().toString().replace("\n", "");
        if (path.isEmpty()) {
            dismissLoadingDialog();
            Toast.makeText(getApplicationContext(), "请输入文件目录！", Toast.LENGTH_LONG).show();
            return;
        }
        File srcDir = new File(storage, path);
        mSrcDir = srcDir;
        mTvConsole.setText("");
        if (srcDir.exists() && srcDir.isDirectory()) {
            threadExecutor.execute(decryptTsFilesRunnable);
        } else {
            dismissLoadingDialog();
            showDebugLog(mTvConsole, srcDir.getAbsolutePath() + " doesn't exist");
        }
    }

    private File createOutputFile(File outDir, String dir, List<File> files) {
        int total = files.size();
        File firstFile = files.get(0);
        File lastFile = files.get(total - 1);

        String indexLeft = getTsIndex(firstFile.getName());
        String indexRight = getTsIndex(lastFile.getName());
        return new File(outDir, dir + "_" + indexLeft + "-" + indexRight + ".wmv");
    }

    private void openVideoPlayer(String path) {
        // 开启本地代理
        M3u8Server.execute();
        String dmmDir = Environment.getExternalStorageDirectory().getPath() + File.separator + "dmm";
        String localUrl = String.format("http://127.0.0.1:%d%s", M3u8Server.PORT, dmmDir
                + File.separator + path + File.separator + "local.m3u8");

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        String type = "video/*";
        Uri uri = Uri.parse(localUrl);
        intent.setDataAndType(uri, type);
        startActivity(intent);
    }

    private void showDebugLogOnUIThread(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showDebugLog(mTvConsole, msg);
            }
        });
    }

    private void debugLogAppendOnUIThread(String msg, String colorString) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SpannableStringBuilder builder = CommonUtil.setSpannableString(msg, colorString);
                mTvConsole.append(builder);
            }
        });
    }

    private void multiUrlDownloadWithBrowser(Context context, String firstUrl, int count) {
        String firstFileName = generateFileName(firstUrl);
        String firstTsIndex = getTsIndex(firstFileName);
        for (int i = 0; i < count; i++) {
            if (firstUrl.endsWith(".ts")) {
                int index = Integer.valueOf(firstTsIndex) + i;
                String curTsName = firstFileName.replace(firstTsIndex + ".ts",
                        String.valueOf(index) + ".ts");
                String curUrl = firstUrl.replace(firstFileName, curTsName);
                openBrowserDownload(context, curUrl);
            } else {
                openBrowserDownload(context, firstUrl);
            }
        }
    }

    private String generateFileName(String url) {
        String[] strings = url.split("/");
        return strings[strings.length - 1];
    }
}
