package wei.yuan.video_decrypt.util;

import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;

/**
 * 公共类
 *
 * @author yugimaster 2020/4/17
 */
public class CommonUtil {

    private final static String TAG = "CommonUtil";

    /**
     * 截取byte数组   不改变原数组
     * @param b 原数组
     * @param off 偏差值（索引）
     * @param length 长度
     * @return 截取后的数组
     */
    public static byte[] getSubBytes(byte[] b, int off, int length) {
        byte[] b1 = new byte[length];
        System.arraycopy(b, off, b1, 0, length);
        return b1;
    }

    /**
     * 合并byte[]数组 （不改变原数组）
     * @param b1
     * @param b2
     * @return 合并后的数组
     */
    public static byte[] getMergedBytes(byte[] b1, byte[] b2) {
        byte[] b = new byte[b1.length + b2.length];
        System.arraycopy(b1, 0, b, 0, b1.length);
        System.arraycopy(b2, 0, b, b1.length, b2.length);
        return b;
    }

    /**
     * 根据字节数生成元素均为0的byte数组
     * @param length
     * @return
     */
    public static byte[] getZeroBytes(int length) {
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = 0;
        }
        return b;
    }

    /**
     * 写入二进制文件
     * @param bytes
     * @param file
     */
    public static boolean writeBinaryFile(byte[] bytes, File file, TextView textView) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes, 0, bytes.length);
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            textView.append("\n" + e.toString());
        }

        return false;
    }

    /**
     * 追加写入二进制文件
     * @param bytes
     * @param file
     */
    public static boolean writeBinaryFileAppend(byte[] bytes, File file, TextView textView) {
        try {
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(bytes, 0, bytes.length);
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            textView.append("\n" + e.toString());
        }

        return false;
    }

    /**
     * 读取二进制文件
     * @param file
     * @return
     */
    public static byte[] readBinaryFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            long len = file.length();
            byte[] data = toByteArray(fis, len);
            fis.close();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取文件大小
     * @param sizeInByte
     * @return String
     */
    public static String getFileSize(long sizeInByte) {
        if (sizeInByte < 1024) {
            return String.format("%s", sizeInByte);
        } else if (sizeInByte < 1024 * 1024) {
            return String.format(Locale.CANADA, "%.2fKB", sizeInByte / 1024.);
        } else if (sizeInByte < 1024 * 1024 * 1024) {
            return String.format(Locale.CANADA, ".2fMB", sizeInByte / 1024. / 1024);
        } else {
            return String.format(Locale.CANADA, "%.2fGB", sizeInByte / 1024. / 1024 / 1024);
        }
    }

    private static byte[] toByteArray(FileInputStream fis, long len) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[(int)len];
            int n = 0;
            while ((n = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }
}
