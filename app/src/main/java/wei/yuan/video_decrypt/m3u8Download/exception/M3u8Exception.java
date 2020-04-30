package wei.yuan.video_decrypt.m3u8Download.exception;

/**
 * @author yugimaster
 * @email yugibrother@126.com
 * @date 2020/04/28
 */

public class M3u8Exception extends RuntimeException {
    public M3u8Exception() {
        super();
    }

    public M3u8Exception(String message) {
        super(message);
    }

    public M3u8Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
