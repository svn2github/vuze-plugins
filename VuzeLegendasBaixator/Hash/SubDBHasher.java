package Hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 20/05/2010
 * Time: 10:50:01
 * To change this template use File | Settings | File Templates.
 */
public class SubDBHasher {
    /**
     * Size of the chunks that will be hashed in bytes (64 KB)
     */
    private static final int HASH_CHUNK_SIZE = 64 * 1024;

    public static String computeHash(File file) {
        byte[] head = new byte[HASH_CHUNK_SIZE];
        byte[] tail = new byte[HASH_CHUNK_SIZE];

        try {
            RandomAccessFile randomFile = new RandomAccessFile(file, "r");
            randomFile.read(head);
            randomFile.seek(randomFile.length() - HASH_CHUNK_SIZE);
            randomFile.read(tail);

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(head);
            md5.update(tail);
            return stringHexa(md5.digest());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static String stringHexa(byte[] bytes) {
       StringBuilder s = new StringBuilder();
       for (int i = 0; i < bytes.length; i++) {
           int parteAlta = ((bytes[i] >> 4) & 0xf) << 4;
           int parteBaixa = bytes[i] & 0xf;
           if (parteAlta == 0) s.append('0');
           s.append(Integer.toHexString(parteAlta | parteBaixa));
       }
       return s.toString();
    }
}
