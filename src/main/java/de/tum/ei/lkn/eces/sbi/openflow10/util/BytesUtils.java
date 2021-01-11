package de.tum.ei.lkn.eces.sbi.openflow10.util;

/**
 * Some helpers for byte arrays.
 *
 * @author Amaury Van Bemten
 */
public class BytesUtils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static long bytesToLong(byte[] array) {
        return bytesToLong(array, 0, array.length);
    }

    public static long bytesToLong(byte[] array, int start, int end) {
        if(array == null || array.length == 0 || start >= end || start > array.length || end > array.length)
            return 0;

        long result = 0;
        for(int i = start; i < end; i++) {
            result <<= 8;
            result |= (array[i] & 0xFF);
        }
        return result;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
