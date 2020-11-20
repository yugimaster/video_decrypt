package wei.yuan.video_decrypt;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import wei.yuan.video_decrypt.util.AESUtil;
import wei.yuan.video_decrypt.util.CommonUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final String EXTERNAL_STORAGE = "/mnt/sdcard/dmm";

    private EditText mEt;
    private ScrollView mScrollView;
    private TextView mTvConsole;
    private Button mBtnM3U8;
    private Button mBtnDownload;
    private Button mBtnCombine;
    private Button mBtnDecrypt;
    private Button mBtnPlay;

    private BroadcastReceiver mOtgReceiver;

    private String[] usbList = null;

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
        mBtnDownload = (Button) findViewById(R.id.btn2);
        mBtnDownload.setOnClickListener(this);
        mBtnCombine = (Button) findViewById(R.id.btn3);
        mBtnCombine.setOnClickListener(this);
        mBtnDecrypt = (Button) findViewById(R.id.btn4);
        mBtnDecrypt.setOnClickListener(this);
        mBtnPlay = (Button) findViewById(R.id.btn5);
        mBtnPlay.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOtgReceiver != null) {
            this.unregisterReceiver(mOtgReceiver);
            mOtgReceiver = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1:
                Log.v(TAG, "start local m3u8 activity");
                Intent intent1 = new Intent(Intent.ACTION_VIEW);
                Bundle bundle1 = new Bundle();
                String path1 = mEt.getText().toString().replace("\n", "");
                bundle1.putString("directory", path1);
                intent1.setClassName(getApplicationContext(), LocalM3u8Activity.class.getName());
                intent1.putExtra("Info", bundle1);
                startActivity(intent1);
                break;
            case R.id.btn2:
                Log.v(TAG, "start download activity");
                Bundle bundle = new Bundle();
                Intent intent2 = new Intent(Intent.ACTION_VIEW);
                String path = mEt.getText().toString().replace("\n", "");
                bundle.putString("directory", path);
                intent2.setClassName(getApplicationContext(), DownloadActivity.class.getName());
                intent2.putExtra("Info", bundle);
                startActivity(intent2);
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
                Intent intent5 = new Intent(Intent.ACTION_VIEW);
                Bundle bundle5 = new Bundle();
                String path5 = mEt.getText().toString().replace("\n", "");
                bundle5.putString("directory", path5);
                intent5.setClassName(getApplicationContext(), M3U8VodActivity.class.getName());
                intent5.putExtra("Info", bundle5);
                startActivity(intent5);
                break;
            default:
                break;
        }
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
        showDebugLog(mTvConsole, "decryptSingleTs()");
        byte[] data = CommonUtil.readBinaryFile(tsFile);
        if (data == null) {
            showDebugLog(mTvConsole, tsFile.getAbsolutePath() + " is null");
            return;
        }
        byte[] newData = AESUtil.decrypt(data, keyHex, ivHex);
        if (newData == null) {
            showDebugLog(mTvConsole, "ts file is invalid");
            return;
        }
        String name = fileName.replace(".ts", "");
        File tempDir = new File(srcDir, "temp");
        if (!tempDir.exists()) {
            showDebugLog(mTvConsole, "make temp directory");
            tempDir.mkdirs();
        }
        File tempFile = new File(tempDir, name + "_temp.ts");
        boolean isSaved = CommonUtil.writeBinaryFile(newData, tempFile, mTvConsole);
        if (isSaved) {
            showDebugLog(mTvConsole, "ts file decrypts success");
        } else {
            showDebugLog(mTvConsole, "ts file decrypts failed");
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
            showDebugLog(mTvConsole, e.toString());
            return "";
        }
    }

    private String getIvHex(String fileName) {
        Log.d(TAG, "file name: " + fileName);
        showDebugLog(mTvConsole, "file name: " + fileName);
        String tsIndex = getTsIndex(fileName);
        showDebugLog(mTvConsole, "ts index: " + tsIndex);
        int index = Integer.valueOf(tsIndex);
        return String.format("%032x", index);
    }

    private void showExternalStorageFiles(File srcDir) {
        File keyFile = new File(srcDir, "key.key");
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
            String[] s = name.split("_");
            String index = s[2];
            return index;
        } catch (Exception e) {
            showDebugLog(mTvConsole, "getTsIndex()" + "\n" + e.toString());
            return "";
        }
    }

    private void decryptTsFiles() {
        Log.v(TAG, "decryptTsFiles()");
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
        mTvConsole.setText("");
        if (srcDir.exists() && srcDir.isDirectory()) {
            showExternalStorageFiles(srcDir);
        } else {
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
}
