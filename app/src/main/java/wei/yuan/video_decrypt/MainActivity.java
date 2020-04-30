package wei.yuan.video_decrypt;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import wei.yuan.video_decrypt.util.AESUtil;
import wei.yuan.video_decrypt.util.CommonUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String EXTERNAL_STORAGE = "/mnt/sdcard/dmm";

    private EditText mEt;
    private ScrollView mScrollView;
    private TextView mTvConsole;
    private Button mBtnM3U8;
    private Button mBtnDownload;

    private BroadcastReceiver mOtgReceiver;

    private String[] usbList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        mEt = (EditText) findViewById(R.id.et_path);
        mEt.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    Log.v(TAG, "手指弹起时执行确认功能");
                    File storage = new File(EXTERNAL_STORAGE);
                    if (!storage.exists() || !storage.isDirectory()) {
                        Log.v(TAG, "storage invalid!");
                        return true;
                    }
                    String path = mEt.getText().toString().replace("\n", "");
                    File srcDir = new File(storage, path);
                    if (srcDir.exists() && srcDir.isDirectory()) {
                        showExternalStorageFiles(srcDir);
                    } else {
                        showDebugLog(mTvConsole, srcDir.getAbsolutePath() + " doesn't exist");
                    }
                    return true;
                }

                return false;
            }
        });
        mScrollView = (ScrollView) findViewById(R.id.scroller);
        mTvConsole = (TextView) findViewById(R.id.consoleText);
        mBtnM3U8 = (Button) findViewById(R.id.btn1);
        mBtnM3U8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "start local m3u8 activity");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClassName(getApplicationContext(), LocalM3u8Activity.class.getName());
                startActivity(intent);
            }
        });
        mBtnDownload = (Button) findViewById(R.id.btn2);
        mBtnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "start m3u8 download activity");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClassName(getApplicationContext(), M3u8DownloadActivity.class.getName());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOtgReceiver != null) {
            this.unregisterReceiver(mOtgReceiver);
            mOtgReceiver = null;
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
}
