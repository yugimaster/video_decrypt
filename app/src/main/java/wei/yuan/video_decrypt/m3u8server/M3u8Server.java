package wei.yuan.video_decrypt.m3u8server;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import wei.yuan.video_decrypt.m3u8server.NanoHTTPD.Response.Status;

public class M3u8Server extends NanoHTTPD {

    private static final String TAG = "M3U8Server";

    // M3u8Server 服务对象
    private static NanoHTTPD mM3u8Server;
    // 端口号
    public static final int PORT = 8081;

    /**
     * 构造方法
     */
    public M3u8Server() {
        // 端口号
        super(PORT);
        Log.v(TAG, "---M3u8Server---");
    }

    /**
     * 启动服务
     */
    public static void execute() {
        Log.v(TAG, "---execute---");
        try {
            // 创建服务
            mM3u8Server = M3u8Server.class.newInstance();
            // 超时时间
            mM3u8Server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException ioe) {
            Log.e(TAG, "启动服务失败：\n" + ioe);
        } catch (Exception e) {
            Log.e(TAG, "启动服务失败：\n" + e);
            System.exit(-1);
        }

        Log.i(TAG, "服务启动成功\n");

        try {
            System.in.read();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 重写 serve 方法，获取本地sdcard视频文件
     *
     * @param session The HTTP session
     * @return
     */
    @Override
    public Response serve(IHTTPSession session) {
        Log.v(TAG, "---serve---");
        String url = String.valueOf(session.getUri());
        Log.v(TAG, "请求URL：" + url);
        File file = new File(url);
        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return newFixedLengthResponse(Status.NOT_FOUND, "text/html",
                        "文件不存在：" + url);
            }
            long length = file.length();
            // ts文件
            String mimeType = "video/mpeg";
            if (url.contains(".m3u8")); {
                // m3u8文件
                mimeType = "video/x-mpegURL";
            }
            return newChunkedResponse(Status.OK, mimeType, fis);
        } else {
            return newFixedLengthResponse(Status.NOT_FOUND, "text/html",
                    "文件不存在：" + url);
        }
    }
}
