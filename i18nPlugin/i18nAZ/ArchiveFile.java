package i18nAZ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

public class ArchiveFile
{
    String path = null;
    String basePath = null;
    List<JarInputStream> jarInputStreams = new ArrayList<JarInputStream>();

    public ArchiveFile(File file) throws IOException
    {
        this(Path.getPath(file));
    }

    public ArchiveFile(URL url) throws IOException
    {
        this(Path.getPath(url));
       
    }

    public ArchiveFile(String path) throws IOException
    {        
        
        String[] basePaths = new String[2];
        InputStream inputStream = Path.openInputStream(path, basePaths, true);
        if(inputStream == null)
        {
            throw new FileNotFoundException();
        }
        if(inputStream instanceof JarInputStream == false)
        {
            inputStream.close();
            throw new FileNotFoundException();
        }      
        this.basePath = basePaths[0];
        this.path = basePaths[1];  
        this.jarInputStreams.add((JarInputStream) inputStream);
    }

    public void close() throws IOException
    {
        synchronized (ArchiveFile.this)
        {
            while (this.jarInputStreams.size() > 0)
            {
                try
                {
                    this.jarInputStreams.get(0).close();
                }
                catch (IOException e)
                {
                }
                this.jarInputStreams.remove(0);
            }
        }
    }

    public Enumeration<JarEntry> entries()
    {
        synchronized (ArchiveFile.this)
        {
            final JarInputStream jarInputStream = this.jarInputStreams.get(0);
            try
            {
                this.jarInputStreams.add(0, (JarInputStream) Path.openInputStream(this.path, null, true));
            }
            catch (IOException e)
            {
                return null;
            }

            return new Enumeration<JarEntry>()
            {
                boolean next = false;
                JarEntry currentJarEntry = null;
                JarEntry nextJarEntry = null;
                HashSet<String> dirs = new HashSet<String>();

                public JarEntry getNextJarEntry()
                {
                    while(true)
                    {
                        if (this.currentJarEntry == null)
                        {
                            try
                            {
                                this.currentJarEntry = jarInputStream.getNextJarEntry();
                            }
                            catch (IOException e)
                            {
                            }
                        }
                        if (this.currentJarEntry == null)
                        {
                            return null;
                        }
                        String path =  ArchiveFile.this.path;
                        String pathFile = ArchiveFile.this.basePath + "!/" + this.currentJarEntry.getName();         
                        if(pathFile.length() <= path.length()|| pathFile.startsWith(path) == false)
                        {
                            this.currentJarEntry = null;
                            continue;
                        }
                        pathFile = pathFile.substring(path.length());
                        String[] block = this.currentJarEntry.getName().split("/");
                        for (int i = 0; i < block.length - 1; i++)
                        {
                            String pathDir = ArchiveFile.this.basePath + "!/" + block[0] + "/";
                            for (int j = 0; j < i; j++)
                            {
                                pathDir += block[0] + "/";
                            }
                            if(pathDir.length() <= path.length() || pathDir.startsWith(path) == false)
                            {
                                continue;
                            }
                            pathDir = pathDir.substring(path.length());
                            if (this.dirs.contains(pathDir) == true)
                            {
                                continue;
                            }
                            this.dirs.add(pathDir);
                            return new JarEntry(  pathDir);
                        }
                        JarEntry result = this.currentJarEntry;
                        Util.setValue(result, Util.getField(JarEntry.class, "name"), pathFile);
                        this.currentJarEntry = null;
                        return result;
                    }
                }

                
                public boolean hasMoreElements()
                {
                    synchronized (ArchiveFile.this)
                    {
                        if (this.next == false)
                        {
                            this.nextJarEntry = this.getNextJarEntry();                      
                            this.next = true;
                        }
                        return this.nextJarEntry != null;
                    }
                }

                
                public JarEntry nextElement() throws NoSuchElementException
                {
                    synchronized (ArchiveFile.this)
                    {

                        JarEntry jarEntry = null;
                        if (this.next == false)
                        {                            
                            jarEntry = this.getNextJarEntry();                          
                        }
                        else
                        {
                            this.next = false;
                            jarEntry = this.nextJarEntry;
                        }
                        if (this.hasMoreElements() == false)
                        {
                            try
                            {
                                jarInputStream.close();
                            }
                            catch (IOException e)
                            {
                            }
                        }
                        return jarEntry;
                    }
                }
            };
        }
    }

    public ZipEntry getEntry(String name)
    {
        return this.getJarEntry(name);
    }

    public InputStream getInputStream(ZipEntry ze) throws IOException
    {
        JarInputStream jarInputStream = null;
        synchronized (ArchiveFile.this)
        {
            jarInputStream = this.jarInputStreams.get(0);
            try
            {
                this.jarInputStreams.add(0, (JarInputStream) Path.openInputStream(this.path, null, true));
            }
            catch (IOException e)
            {
                return null;
            }
        }
        ZipEntry zipEntry = null;
        while ((zipEntry = jarInputStream.getNextEntry()) != null)
        {
            if (zipEntry.getName().equalsIgnoreCase(ze.getName()) == true)
            {
                return jarInputStream;
            }
        }
        try
        {
            jarInputStream.close();
        }
        catch (IOException e)
        {
        }
        return null;

    }

    public JarEntry getJarEntry(String name)
    {
        Enumeration<JarEntry> entries = this.entries();
        while (entries.hasMoreElements())
        {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.getName().equalsIgnoreCase(name) == true)
            {
                return jarEntry;
            }
        }
        return null;
    }

    public String getName()
    {
        return this.path;
    }

    public URL[] findRessources(String resourceName, String extension)
    {
        return this.findResources(resourceName, null, null, null, extension);
    }

    public URL[] findContainsRessources(String contains, String extension)
    {
        return this.findResources(null, contains, null, null, extension);
    }

    public URL[] findStartsWithRessources(String startsWith, String extension)
    {
        return this.findResources(null, null, startsWith, null, extension);
    }

    public URL[] findEndsWithRessources(String endsWith, String extension)
    {
        return this.findResources(null, null, null, endsWith, extension);
    }

    private URL[] findResources(String equals, String contains, String startsWith, String endsWith, String extension)
    {
        equals = (equals == null) ? null : equals.toLowerCase(Locale.US);
        contains = (contains == null) ? null : contains.toLowerCase(Locale.US);
        startsWith = (startsWith == null) ? null : startsWith.toLowerCase(Locale.US);
        endsWith = (endsWith == null) ? null : endsWith.toLowerCase(Locale.US);
        extension = extension.toLowerCase(Locale.US);
        URL[] resourcesURLs = new URL[0];
        Enumeration<JarEntry> entries = this.entries();
        while (entries.hasMoreElements())
        {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.isDirectory() == false)
            {
                String jarEntryExtension = Path.getExtension(jarEntry.getName());
                if (jarEntryExtension.equalsIgnoreCase(".jar") == true || jarEntryExtension.equalsIgnoreCase(".zip") == true)
                {
                    ArchiveFile archiveFile = null;
                    try
                    {
                        archiveFile = new ArchiveFile(this.getName() + jarEntry.getName());
                    }
                    catch (IOException e)
                    {
                    }
                    if (archiveFile != null)
                    {
                        URL[] resourcesChildURLs = archiveFile.findResources(equals, contains, startsWith, endsWith, extension);
                        URL[] newTable = new URL[resourcesURLs.length + resourcesChildURLs.length];
                        System.arraycopy(resourcesURLs, 0, newTable, 0, resourcesURLs.length);
                        System.arraycopy(resourcesChildURLs, 0, newTable, resourcesURLs.length, resourcesChildURLs.length);
                        resourcesURLs = newTable;
                    }
                }
                else if (jarEntryExtension.equalsIgnoreCase(extension) == true)
                {
                    String jarEntryName = Path.getFilenameWithoutExtension(jarEntry.getName().toLowerCase(Locale.US));
                    if (((equals != null && jarEntryName.equals(equals)) || (contains != null && jarEntryName.contains(contains)) || (startsWith != null && jarEntryName.startsWith(startsWith)) || (endsWith != null && jarEntryName.endsWith(endsWith))))
                    {
                        URL url = Path.getUrl(this.getName() + jarEntry.getName());
                        URL[] newTable = new URL[resourcesURLs.length + 1];
                        System.arraycopy(resourcesURLs, 0, newTable, 0, resourcesURLs.length);
                        resourcesURLs = newTable;
                        resourcesURLs[resourcesURLs.length - 1] = url;
                    }

                }

            }
        }
        return resourcesURLs;
    }

    public int size()
    {
        int value = 0;
        Enumeration<JarEntry> entries = this.entries();
        while (entries.hasMoreElements())
        {
            entries.nextElement();
            value++;
        }
        return value;
    }

}
