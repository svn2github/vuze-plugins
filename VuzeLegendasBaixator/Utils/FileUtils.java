package Utils;

import Model.SubTitleLanguage;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 12/03/2010
 * Time: 15:05:49
 * To change this template use File | Settings | File Templates.
 */
public class FileUtils {
    public enum MovieExtensions { AJP, ASF, ASX, AVCHD, AVI, BIK, BIX, BOX, CAM, DAT, DIVX, DMF, DV, VO, LC, FLI, FLIC, FLV, FLX, GVI,
        GVP, H264, M1V, M2P, M2TS, M2V, M4E, M4V, MJP, MJPEG, MJPG, MKV, MOOV, MOV, MOVHD, MOVIE, MOVX, MP4, MPE, MPEG, MPG, MPV, MPV2,
        MXF, NSV, NUT, OGG, OGM, OMF, PS, QT, RAM, RM, RMVB, SWF, TS, VFW, VID, VIDEO, VIV, VIVO, VOB, VRO, WM, WMV, WMX, WRAP, WVX, WX,
        X264, XVID }

    public enum SubTitleExtensions { SRT, SUB, SMI, TXT, SSA, ASS, MPL }

    public static boolean isMovieFile(String fileName) {
        try {
            MovieExtensions ext = MovieExtensions.valueOf(getExtension(fileName).toUpperCase());
            return (ext != null);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isSubTitleFile(String fileName) {
        try {
            SubTitleExtensions ext = SubTitleExtensions.valueOf(getExtension(fileName).toUpperCase());
            return (ext != null);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean hasSubTitleFile(String pathDir, final String fileName, final SubTitleLanguage subTitleLanguage) {
        final String[] sufixList = SubTitleLanguage.getCodeSufix(subTitleLanguage);
        File dirFiles = new File(pathDir);
        final File[] files = dirFiles.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    boolean found = !fileName.equals(name) && (isSubTitleFile(name)) &&
                            getFileNameWithoutExtension(fileName).equalsIgnoreCase(getFileNameWithoutExtension(name));
                    if ((!found) && (subTitleLanguage != null)) {
                        for (String sufix : sufixList) {
                            String fileNameSubtitle = getFileNameWithoutExtension(fileName) + "." + sufix;
                            found = fileNameSubtitle.equalsIgnoreCase(getFileNameWithoutExtension(name));
                            if (found)
                                break;
                        }
                    }
                    return found;
                }
        });
        return (files != null) && (files.length > 0);
    }

    public static String changeExtension(String originalName, String newExtension) {        
        int lastDot = originalName.lastIndexOf(".");
        if (lastDot != -1) {
            return originalName.substring(0, lastDot) + "." + newExtension;
        } else {
            return originalName + ". " + newExtension;
        }
    }

    public static String getPathWithoutFileName(String fullPathFile) {
        int sep = fullPathFile.lastIndexOf(File.separator);
        if (sep < 0)
            return fullPathFile;
        else
            return fullPathFile.substring(0, sep);
    }

    public static String getFileNameWithoutExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot != -1) {
            return fileName.substring(0, lastDot);
        } else {
            return fileName;
        }
    }

    public static String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot != -1) {
            return fileName.substring(lastDot+1);
        } else {
            return fileName;
        }
    }

    public static String InputToString(InputStream inStream, String enconding) {
        try {
            final char[] buffer = new char[0x10000];
            StringBuilder out = new StringBuilder();
            Reader in = new InputStreamReader(inStream, enconding);
            int read;
            do {
                read = in.read(buffer, 0, buffer.length);
                if (read > 0) {
                    out.append(buffer, 0, read);
                }
            } while (read >= 0);
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static InputStream StringToInput(String value) {
        return new ByteArrayInputStream(value.getBytes());
    }

    public static byte[] InputToByte(InputStream inStream) {
        try {
            final byte[] buffer = new byte[0x10000];
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(0);
            int read;
            do {
                read = inStream.read(buffer, 0, buffer.length);
                if (read >= 0) {
                    byteStream.write(buffer, 0, read);
                }
            } while (read >= 0);
            return byteStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error converting InputStream to Bytes: " + e.getMessage(), e);
        }
    }

    public static Map<String, InputStream> DecompressRar(InputStream inStream) {
        try {
            final Map<String, InputStream> mapFiles = new HashMap<String, InputStream>();

            ISevenZipInArchive inArchive = SevenZip.openInArchive(null, new sevenZipByteInputStream(InputToByte(inStream)));
            ISimpleInArchive simpleRar = inArchive.getSimpleInterface();
            for (final ISimpleInArchiveItem item : simpleRar.getArchiveItems()) {
                item.extractSlow(new ISequentialOutStream() {
                        public int write(byte[] data) throws SevenZipException {
                            mapFiles.put(item.getPath(), new ByteArrayInputStream(data));
                            return data.length; // Return amount of proceed data
                        }
                    });
            }
            return mapFiles;
        } catch (SevenZipException e) {
            throw new RuntimeException("Error decompressing RAR: " + e.getMessage(), e);
        }
    }

    public static byte[] decodeBase64(String contentBase64) {
        return Base64.decodeBase64(contentBase64.getBytes());
    }

    public static InputStream inflateFromGZip(byte[] contentFile) {
        try {
            ByteArrayInputStream byteInput = new ByteArrayInputStream(contentFile);
            return new GZIPInputStream(byteInput);
        } catch (IOException e) {
            throw new RuntimeException("Error on inflate Stream: " + e.getMessage(), e);
        }
    }
}
