/*
 * Path.java
 *
 * Created on April 26, 2014, 3:31 PM
 */
package i18nAZ;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Path class
 * 
 * @author Repris d'injustice
 */
public class Path
{

    public static String getPath(URL url)
    {
        if (url == null)
        {
            return "";
        }
        return Path.format(url.getFile());
    }

    private static String format(String path)
    {
        if (path == null)
        {
            return "";
        }
        path = path.replace("!\\", "!/");

        String[] part = path.split("!/");

        path = part[0];
        path = path.replace('/', '\\');
        path = path.replace('|', ':');
       
        if (path.startsWith("jar:") == true)
        {
            path = path.substring(4);
        } 
        if (path.startsWith("file:") == true)
        {
            path = path.substring(5);
        }
        if (path.startsWith("\\") == true)
        {
            path = path.substring(1);
        }
        try
        {
            path = URLDecoder.decode(path, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
        }
        for (int i = 1; i < part.length; i++)
        {
            if(part[i].equals("") == true)
            {
                continue;
            }
            part[i] = part[i].replace('\\', '/');
            path += "!/" + part[i];
        }
        return path;
    }

    public static String getPath(File file)
    {
        return Path.getPath(Path.getUrl(file));
    }

    public static File getFile(URL url)
    {
        String path = Path.getPath(url);
        if (path == null || path == "")
        {
            return null;
        }
        return new File(path);
    }

    public static String getFilename(URL url)
    {
        return Path.getFilename(Path.getPath(url));
    }

    public static String getFilename(File file)
    {
        return Path.getFilename(Path.getPath(file));
    }

    public static String getFilename(String path)
    {
        if (path == null)
        {
            return "";
        }
        path = Path.format(path);

        String fileName = path;
        int lastIndex = fileName.lastIndexOf("!/");
        if (lastIndex != -1)
        {
            fileName = fileName.substring(lastIndex + 2);
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        else
        {
            fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
        }
        return fileName;
    }

    public static String getFilenameWithoutExtension(URL url)
    {
        return Path.getFilenameWithoutExtension(Path.getPath(url));
    }

    public static String getFilenameWithoutExtension(File file)
    {
        return Path.getFilenameWithoutExtension(Path.getPath(file));
    }

    public static String getFilenameWithoutExtension(String path)
    {
        if (path == null)
        {
            return "";
        }
        String fileNameWithoutExtension = Path.getFilename(path);
        int lastIndex = fileNameWithoutExtension.lastIndexOf('.');
        if (lastIndex != -1)
        {
            fileNameWithoutExtension = fileNameWithoutExtension.substring(0, lastIndex);
        }
        return fileNameWithoutExtension;
    }

    public static String getExtension(URL url)
    {
        return Path.getExtension(Path.getPath(url));
    }

    public static String getExtension(File file)
    {
        return Path.getExtension(Path.getPath(file));
    }

    public static String getExtension(String path)
    {
        if (path == null)
        {
            return "";
        }
        String extension = "";
        String fileNameWithoutExtension = Path.getFilename(path);
        int lastIndex = fileNameWithoutExtension.lastIndexOf('.');
        if (lastIndex != -1)
        {
            extension = fileNameWithoutExtension.substring(lastIndex);
        }
        return extension;
    }

    public static URL getUrl(String path)
    {
        path = Path.format(path);
        String[] part = path.split("!/");
        if (part.length == 1)
        {
            path = "file:" + path;
        }
        else
        {
            path = "jar:file:" + path;
        }
        try
        {
            return new URL(path);
        }
        catch (MalformedURLException e)
        {
            return null;
        }
    }

    public static URL getUrl(File file)
    {
        return Path.getUrl(file.toString());
    }
    
    public static boolean exists(URL url)
    {
        if (url == null)
        {
            return false;
        }

        try
        {
            InputStream inputStream = Path.openInputStream(url);
            inputStream.close();
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public static File clone(File file)
    {
        return file.getAbsoluteFile();
    }

    public static URL clone(URL url)
    {
        String path = Path.getPath(url);
        if (path == null || path == "")
        {
            return null;
        }
        return Path.getUrl(path);
    }

    public static boolean copyFile(URL source, URL dest)
    {
        return Path.copyFile(Path.getPath(source), Path.getPath(dest), false);
    }

    public static boolean copyFile(URL source, File dest)
    {
        return Path.copyFile(Path.getPath(source), Path.getPath(dest), false);
    }

    public static boolean copyFile(URL source, String dest)
    {
        return Path.copyFile(Path.getPath(source), dest, false);
    }

    public static boolean copyFile(File source, File dest)
    {
        return Path.copyFile(Path.getPath(source), Path.getPath(dest), false);
    }

    public static boolean copyFile(String source, String dest)
    {
        return Path.copyFile(source, dest, false);
    }

    public static boolean copyFile(URL source, File dest, boolean closeInputStream)
    {
        return Path.copyFile(Path.getPath(source), Path.getPath(dest), closeInputStream);
    }

    public static boolean copyFile(URL source, URL dest, boolean closeInputStream)
    {
        return Path.copyFile(Path.getPath(source), Path.getPath(dest), closeInputStream);
    }

    public static boolean copyFile(File source, File dest, boolean closeInputStream)
    {
        return Path.copyFile(Path.getPath(source), Path.getPath(dest), closeInputStream);
    }

    public static boolean copyFile(String source, String dest, boolean closeInputStream)
    {
        try
        {
            Path.copyFileWithException(source, dest, closeInputStream);
        }
        catch (IOException e)
        {
            return false;
        }
        return true;
    }

    public static void copyFileWithException(URL source, URL dest) throws IOException
    {
        Path.copyFileWithException(Path.getPath(source), Path.getPath(dest), false);
    }

    public static void copyFileWithException(File source, File dest) throws IOException
    {
        Path.copyFileWithException(Path.getPath(source), Path.getPath(dest), false);
    }

    public static void copyFileWithException(String source, String dest) throws IOException
    {
        Path.copyFileWithException(source, dest, false);
    }

    public static void copyFileWithException(URL source, URL dest, boolean closeInputStream) throws IOException
    {
        Path.copyFileWithException(Path.getPath(source), Path.getPath(dest), closeInputStream);
    }

    public static void copyFileWithException(File source, File dest, boolean closeInputStream) throws IOException
    {
        Path.copyFileWithException(Path.getPath(source), Path.getPath(dest), closeInputStream);
    }

    public static void copyFileWithException(String source, String dest, boolean closeInputStream) throws IOException
    {
        InputStream inputStream = Path.openInputStream(source);
        OutputStream outputStream = new FileOutputStream(dest);
        Path.copyFileWithException(inputStream, outputStream, closeInputStream);
    }

    private static void copyFileWithException(InputStream inputStream, OutputStream outputStream, boolean closeInputStream) throws IOException
    {
        try
        {
            if (!(inputStream instanceof BufferedInputStream))
            {
                inputStream = new BufferedInputStream(inputStream, 128 * 1024);
            }
            byte[] buffer = new byte[128 * 1024];
            while (true)
            {
                int len = inputStream.read(buffer);
                if (len == -1)
                {
                    break;
                }
                outputStream.write(buffer, 0, len);
            }
        }
        finally
        {
            try
            {
                if (closeInputStream)
                {
                    inputStream.close();
                }
            }
            catch (IOException e)
            {

            }
            outputStream.close();
        }
    }
    public static boolean moveDirectory(File sourceDir, File destDir)
    {
        try
        {
            copyDirectoryWithException(sourceDir, destDir);
        }
        catch (IOException e)
        {
            return false;
        }
        deleteFile(sourceDir);
        return true;
    }
    public static void moveDirectoryWithException(File sourceDir, File destDir) throws IOException
    {
        copyDirectoryWithException(sourceDir, destDir);
        deleteFile(sourceDir);
    }
    public static boolean copyDirectory(File sourceDir, File destDir)
    {
        try
        {
            copyDirectoryWithException(sourceDir, destDir);
        }
        catch (IOException e)
        {
            return false;
        }
        return true;
    }
    public static void copyDirectoryWithException(File sourceDir, File destDir) throws IOException
    {
        if (destDir.exists() == false)
        {
            destDir.mkdirs();
        }

        File[] children = sourceDir.listFiles();
        for (File source : children)
        {
            if (source.isDirectory() == true)
            {
                copyDirectoryWithException(source, new File(destDir, source.getName()));
            }
            else
            {
                String filename = Path.getFilename(source);
                 copyFileWithException(source, new File(destDir, filename), true);
            }
        }
    }
    public static boolean deleteFile(File resource)
    {
        if (resource.isDirectory() == true)
        {
            File[] childFiles = resource.listFiles();
            for (File child : childFiles)
            {
                deleteFile(child);
            }
        }
        return resource.delete();
    }

    public static boolean exists(File file)
    {
        if (file == null)
        {
            return false;
        }
        try
        {
            InputStream inputStream = Path.openInputStream(file);
            inputStream.close();
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public static boolean exists(String path)
    {
        if (path == null || path == "")
        {
            return false;
        }
        try
        {
            InputStream inputStream = Path.openInputStream(path);
            inputStream.close();
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public static File getDirectory(String path)
    {
        return getDirectory(path, null);
    }

    public static File getDirectory(File path)
    {
        return getDirectory(Path.getPath(path), null);
    }

    public static File getDirectory(String path, String child)
    {
        if (path == null || path == "")
        {
            return null;
        }
        path = Path.format(path);
        String[] parts = path.split("!/");

        if (child != null)
        {
            if (parts.length == 1)
            {
                child = child.replace('/', '\\');
                child = child.replace('|', ':');
                if (child.startsWith("\\") == true)
                {
                    child = path.substring(1);
                }
                try
                {
                    child = URLDecoder.decode(child, "UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                }
                path += (child == "") ? "" : "\\" + child;
            }
            else
            {
                child = child.replace('\\', '/');
                if (child.startsWith("/") == true)
                {
                    child = path.substring(1);
                }
                path += (child == "") ? "" : "/" + child;
            }
        }
        if (path.endsWith("\\") == true || path.endsWith("/") == true)
        {
            path += "unexistFile.file";
        }
        else if (parts.length == 1)
        {
            path += "\\unexistFile.file";
        }
        else
        {
            path += "/unexistFile.file";
        }
        return new File(path).getParentFile();
    }

    public static File getDirectory(File path, String child)
    {
        return getDirectory(Path.getPath(path), child);
    }

    public static File getDirectory(File path, File child)
    {
        return getDirectory(Path.getPath(path), Path.getPath(child));
    }
    public static InputStream openInputStream(URL url) throws IOException
    {
        return Path.openInputStream(Path.getPath(url), null, false);
    }

    public static InputStream openInputStream(File file) throws IOException
    {
        return Path.openInputStream(Path.getPath(file), null, false);
    }
    /**
     * Returns an input stream for reading the contents of the specified zip file entry.
     * 
     * <p>
     * Closing this ZIP file will, in turn, close all input streams that have been returned by invocations of this method.
     * 
     * @param entry
     *            the path ex: "C:\\folder\\file.txt" or "C:\\folder\\file.jar!/folder/" or "C:\\folder\\file.jar!/file.exe"
     * @return the input stream for reading the contents of the specified path.
     * @throws ZipException
     *             if a ZIP format error has occurred
     * @throws IOException
     *             if an I/O error has occurred
     */
    public static InputStream openInputStream(String path) throws IOException
    {
        return Path.openInputStream(path, null, false);
    }
    /**
     * Returns an input stream for reading the contents of the specified zip file entry.
     * 
     * <p>
     * Closing this ZIP file will, in turn, close all input streams that have been returned by invocations of this method.
     * 
     * @param entry
     *            the path ex: "C:\\folder\\file.txt" or "C:\\folder\\file.jar!/folder/" or "C:\\folder\\file.jar!/file.exe"
     * @return the input stream for reading the contents of the specified path.
     * @throws ZipException
     *             if a ZIP format error has occurred
     * @throws IOException
     *             if an I/O error has occurred
     */
    public static InputStream openInputStream(String path, String [] basePaths, boolean getLast) throws IOException
    {
        if (path == null || path == "")
        {
            return null;
        }
        path = Path.format(path);
        String[] parts = path.split("!/");
        if(basePaths != null && basePaths.length > 1)
        {
            basePaths[0] = path;
            basePaths[1] = path;
        }
        if (parts.length == 1)
        {
            if (path.toLowerCase(Locale.US).endsWith(".zip") || path.toLowerCase(Locale.US).endsWith(".jar"))
            {
                if(basePaths != null && basePaths.length > 1)
                {
                    basePaths[1] = path + "!/";
                }
                return new JarInputStream(new FileInputStream(path));
            }
            else
            {
                return new FileInputStream(path);
            }
        }
        else
        {
            if(basePaths != null && basePaths.length > 1)
            {
                basePaths[0] = parts[0];
            }
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(parts[0]));
            List<JarInputStream> inputStreams = new ArrayList<JarInputStream>();
            inputStreams.add(jarInputStream);
            JarEntry jarEntry = null;
            for (int i = 1; i < parts.length; i++)
            {
                if(getLast == true && i == parts.length - 1)
                {
                    if(parts[i].endsWith("/"))
                    {
                        if(basePaths != null && basePaths.length > 1)
                        {
                            basePaths[1] = path;
                        }
                        return jarInputStream;
                    }
                }
                while ((jarEntry = jarInputStream.getNextJarEntry()) != null)
                {
                    if (jarEntry.getName().equalsIgnoreCase(parts[i]))
                    {
                        String jarEntryName = jarEntry.getName().toLowerCase(Locale.US);
                        if (jarEntryName.endsWith(".jar") || jarEntryName.endsWith(".zip"))
                        {
                            if(basePaths != null && basePaths.length > 1)
                            {
                                basePaths[1] = path + "!/";
                            }
                            jarInputStream = new JarInputStream(jarInputStream);
                        }
                        inputStreams.add(jarInputStream);
                        break;
                    }
                }
                if (inputStreams.size() == i)
                {
                    throw new FileNotFoundException();
                }
                if(basePaths != null && basePaths.length > 1)
                {
                    basePaths[0] += "!/" + parts[i];
                }
            }
            return jarInputStream;
        }
    }
}
