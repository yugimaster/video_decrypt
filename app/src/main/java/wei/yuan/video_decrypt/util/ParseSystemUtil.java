package wei.yuan.video_decrypt.util;

/**
 * 进制转换工具类
 *
 */
public class ParseSystemUtil {

    /**
     * 将二进制byte[]转换成16进制
     * @param buf
     * @return
     */
    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 将16进制转成二进制byte[]
     * @param hexStr
     * @return
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        char[] hex = hexStr.toCharArray();
        // 转result长度减半
        int length = hex.length / 2;
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            // 先将hex数据转成10进制数值
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            // 将第一个值的二进制值左平移4位  ex: 00001000 => 10000000 (8=>128)
            // 与第二个值的二进制值作联集    ex: 10000000 | 00001100 => 10001100 (137)
            int value = (high << 4) | low;
            // 与FFFFFFFF作补集
            if (value > 127) {
                value -= 256;
            }
            // 最后转回byte
            result[i] = (byte) value;
        }
        return result;
    }
}
