package wei.yuan.video_decrypt.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {

    // 算法
    private static final String ALGORITHM = "AES";

    // 解密算法
    private static final String AES_TYPE = "AES/CBC/NoPadding";

    // 编码
    private static final String ENCODING = "UTF-8";

    public static byte[] decrypt(byte[] data, String key, String iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_TYPE);
            SecretKeySpec secretKeySpec = new SecretKeySpec(ParseSystemUtil.parseHexStr2Byte(key), ALGORITHM);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ParseSystemUtil.parseHexStr2Byte(iv));
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            int blockSize = cipher.getBlockSize();
            int tsLen = data.length;
            int padLen = blockSize - tsLen % blockSize;
            byte newData[] = data;
            if (padLen != blockSize) {
                byte[] dataFront = CommonUtil.getSubBytes(data, 0, tsLen - padLen);
                byte[] dataEnd = CommonUtil.getZeroBytes(padLen);
                newData = CommonUtil.getMergedBytes(dataFront, dataEnd);
            }
            byte[] decryptData = cipher.doFinal(newData);
            if (padLen != blockSize) {
                int decryptLen = decryptData.length;
                byte[] dataTemp = CommonUtil.getSubBytes(decryptData, 0, decryptLen - padLen);
                decryptData = dataTemp;
            }
            return decryptData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
