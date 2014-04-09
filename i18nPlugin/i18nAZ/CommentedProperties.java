/*
 * CommentedProperties.java
 *
 * Created on March 21, 2014, 4:27 PM
 */
package i18nAZ;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * CommentedProperties class
 *   
 * @author Repris d'injustice
 */
class CommentedProperties extends Properties
{
    private static final long serialVersionUID = 1L;
    private boolean Loaded = false;
    private boolean IsThrowable = false;
    private Map<String, String[]> Comments = new HashMap<String, String[]>();
    @Override
    public synchronized Object clone() 
    {
        CommentedProperties commentedProperties = new CommentedProperties();
        commentedProperties.Loaded = true;
        for (Iterator<String> iterator = this.stringPropertyNames().iterator(); iterator.hasNext();)
        {
            String key = iterator.next();
            commentedProperties.put(key, this.get(key));            
        }
        for (Iterator<String> iterator = this.Comments.keySet().iterator(); iterator.hasNext();)
        {
            String key = iterator.next();
            commentedProperties.putComment(key, this.getComment(key));            
        }        
        return commentedProperties;
    }
    @Override
    public synchronized void clear() 
    {
        super.clear();
        this.Comments.clear();
        this.Loaded = false;
    }
    public synchronized void load(URL url) throws IOException
    {
        InputStream inputStream = null;
        try
        {
            inputStream = url.openStream();
        }
        catch (IOException e)
        {
        }
        if(inputStream != null)
        {
            this.load(inputStream);
            inputStream.close();            
        }
    }
    @Override
    public synchronized void load(Reader reader) throws IOException
    {
        this.load0(new LineReader(reader));
    }
    @Override
    public synchronized void load(InputStream inStream) throws IOException
    {
        this.load0(new LineReader(inStream));
    }
    private void load0(LineReader lr) throws IOException
    {
        super.clear();
        char[] convtBuf = new char[1024];
        int limit;
        int keyLen;
        int valueStart;
        char c;
        boolean hasSep;
        boolean precedingBackslash;

        ArrayList<String> CommentList = new ArrayList<String>();
        while ((limit = lr.readLine()) >= 0)
        {
            c = 0;
            keyLen = 0;
            valueStart = limit;
            hasSep = false;

            precedingBackslash = false;
            while (keyLen < limit)
            {
                c = lr.lineBuf[keyLen];
                // need check if escaped.
                if ((c == '=' || c == ':') && !precedingBackslash)
                {
                    valueStart = keyLen + 1;
                    hasSep = true;
                    break;
                }
                else if ((c == ' ' || c == '\t' || c == '\f') && !precedingBackslash)
                {
                    valueStart = keyLen + 1;
                    break;
                }
                if (c == '\\')
                {
                    precedingBackslash = !precedingBackslash;
                }
                else
                {
                    precedingBackslash = false;
                }
                keyLen++;
            }
            while (valueStart < limit)
            {
                c = lr.lineBuf[valueStart];
                if (c != ' ' && c != '\t' && c != '\f')
                {
                    if (!hasSep && (c == '=' || c == ':'))
                    {
                        hasSep = true;
                    }
                    else
                    {
                        break;
                    }
                }
                valueStart++;
            }
            if (lr.isCommentLine == true)
            {
                CommentList.add(this.loadConvert(lr.lineBuf, 0, limit, convtBuf));
            }
            else
            {
                String key = this.loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
                String value = this.loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
                this.put(key, value);
                if (CommentList.size() > 0)
                {
                    if (this.Comments.containsKey(key) == true)
                    {
                        String[] FoundedComments = this.Comments.get(key);
                        for (int i = 0; i < FoundedComments.length; i++)
                        {
                            CommentList.add(i, FoundedComments[i]);
                        }
                    }
                    this.Comments.put(key, CommentList.toArray(new String[CommentList.size()]));
                    CommentList.clear();
                }
            }
        }
        this.Loaded = true;
    }
    class LineReader
    {
        public LineReader(InputStream inStream)
        {
            this.inStream = inStream;
            this.inByteBuf = new byte[8192];
        }

        public LineReader(Reader reader)
        {
            this.reader = reader;
            this.inCharBuf = new char[8192];
        }

        byte[] inByteBuf;
        char[] inCharBuf;
        char[] lineBuf = new char[1024];
        int inLimit = 0;
        int inOff = 0;
        InputStream inStream;
        Reader reader;
        boolean isCommentLine = false;

        int readLine() throws IOException
        {
            this.isCommentLine = false;

            int len = 0;
            char c = 0;

            boolean skipWhiteSpace = true;
            boolean isNewLine = true;
            boolean appendedLineBegin = false;
            boolean precedingBackslash = false;
            boolean skipLF = false;

            while (true)
            {
                if (this.inOff >= this.inLimit)
                {
                    this.inLimit = (this.inStream == null) ? this.reader.read(this.inCharBuf) : this.inStream.read(this.inByteBuf);
                    this.inOff = 0;
                    if (this.inLimit <= 0)
                    {
                        if (len == 0)
                        {
                            return -1;
                        }
                        return len;
                    }
                }
                if (this.inStream != null)
                {
                    // The line below is equivalent to calling a
                    // ISO8859-1 decoder.
                    c = (char) (0xff & this.inByteBuf[this.inOff++]);
                }
                else
                {
                    c = this.inCharBuf[this.inOff++];
                }
                if (skipLF)
                {
                    skipLF = false;
                    if (c == '\n')
                    {
                        continue;
                    }
                }
                if (skipWhiteSpace)
                {
                    if (c == ' ' || c == '\t' || c == '\f')
                    {
                        continue;
                    }
                    if (!appendedLineBegin && (c == '\r' || c == '\n'))
                    {
                        continue;
                    }
                    skipWhiteSpace = false;
                    appendedLineBegin = false;
                }
                if (isNewLine)
                {
                    isNewLine = false;
                    if (c == '#' || c == '!')
                    {
                        skipWhiteSpace = false;
                        appendedLineBegin = false;
                        this.isCommentLine = true;
                        continue;
                    }
                }

                if (c != '\n' && c != '\r')
                {
                    this.lineBuf[len++] = c;
                    if (len == this.lineBuf.length)
                    {
                        int newLength = this.lineBuf.length * 2;
                        if (newLength < 0)
                        {
                            newLength = Integer.MAX_VALUE;
                        }
                        char[] buf = new char[newLength];
                        System.arraycopy(this.lineBuf, 0, buf, 0, this.lineBuf.length);
                        this.lineBuf = buf;
                    }
                    // flip the preceding backslash flag
                    if (c == '\\')
                    {
                        precedingBackslash = !precedingBackslash;
                    }
                    else
                    {
                        precedingBackslash = false;
                    }
                }
                else
                {
                    // reached EOL
                    if (len == 0 && this.isCommentLine == false)
                    {
                        isNewLine = true;
                        skipWhiteSpace = true;
                        len = 0;
                        continue;
                    }
                    if (this.inOff >= this.inLimit)
                    {
                        this.inLimit = (this.inStream == null) ? this.reader.read(this.inCharBuf) : this.inStream.read(this.inByteBuf);
                        this.inOff = 0;
                        if (this.inLimit <= 0)
                        {
                            return len;
                        }
                    }
                    if (precedingBackslash)
                    {
                        len -= 1;
                        // skip the leading whitespace characters in following
                        // line
                        skipWhiteSpace = true;
                        appendedLineBegin = true;
                        precedingBackslash = false;
                        if (c == '\r')
                        {
                            skipLF = true;
                        }
                    }
                    else
                    {
                        return len;
                    }
                }
            }
        }
    }

    private String loadConvert(char[] in, int off, int len, char[] convtBuf)
    {
        if (convtBuf.length < len)
        {
            int newLen = len * 2;
            if (newLen < 0)
            {
                newLen = Integer.MAX_VALUE;
            }
            convtBuf = new char[newLen];
        }
        char aChar;
        char[] out = convtBuf;
        int outLen = 0;
        int end = off + len;

        while (off < end)
        {
            aChar = in[off++];
            if (aChar == '\\' && off < end)
            {
                aChar = in[off++];
                if (aChar == 'u' && off < end)
                {
                    // Read the xxxx
                    int value = 0;
                    boolean isMalformed = false;
                    String Unicode = "";
                    for (int i = 0; i < 4 && off < end; i++)
                    {
                        aChar = in[off++];
                        Unicode += Character.toString(aChar);
                        switch (aChar)
                        {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                isMalformed = true;
                        }
                    }
                    if (isMalformed == false)
                    {
                        out[outLen++] = (char) value;
                    }
                    else if (this.IsThrowable == true)
                    {
                        throw new IllegalArgumentException("Malformed \\uxxxx encoding : \\u" + Unicode);
                    }
                }
                else
                {
                    if (aChar == 't')
                    {
                        aChar = '\t';
                    }
                    else if (aChar == 'r')
                    {
                        aChar = '\r';
                    }
                    else if (aChar == 'n')
                    {
                        aChar = '\n';
                    }
                    else if (aChar == 'f')
                    {
                        aChar = '\f';
                    }
                    out[outLen++] = aChar;
                }
            }
            else
            {
                out[outLen++] = aChar;
            }
        }
        return new String(out, 0, outLen);
    }

    private String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode)
    {
        int len = theString.length();
        int bufLen = len * 2;
        if (bufLen < 0)
        {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuffer outBuffer = new StringBuffer(bufLen);

        for (int x = 0; x < len; x++)
        {
            char aChar = theString.charAt(x);
            // Handle common case first, selecting largest block that
            // avoids the specials below
            if ((aChar > 61) && (aChar < 127))
            {
                if (aChar == '\\')
                {
                    outBuffer.append('\\');
                    outBuffer.append('\\');
                    continue;
                }
                outBuffer.append(aChar);
                continue;
            }
            switch (aChar)
            {
                case ' ':
                    if (x == 0 || escapeSpace)
                    {
                        outBuffer.append('\\');
                    }
                    outBuffer.append(' ');
                    break;
                case '\t':
                    outBuffer.append('\\');
                    outBuffer.append('t');
                    break;
                case '\n':
                    outBuffer.append('\\');
                    outBuffer.append('n');
                    break;
                case '\r':
                    outBuffer.append('\\');
                    outBuffer.append('r');
                    break;
                case '\f':
                    outBuffer.append('\\');
                    outBuffer.append('f');
                    break;
                case '=': // Fall through
                case ':': // Fall through
                case '#': // Fall through
                case '!':
                    outBuffer.append('\\');
                    outBuffer.append(aChar);
                    break;
                default:
                    if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode)
                    {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(Util.toHex((aChar >> 12) & 0xF));
                        outBuffer.append(Util.toHex((aChar >> 8) & 0xF));
                        outBuffer.append(Util.toHex((aChar >> 4) & 0xF));
                        outBuffer.append(Util.toHex(aChar & 0xF));
                    }
                    else
                    {
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }

    private static void writeComments(BufferedWriter bw, String comments) throws IOException
    {
        bw.write("#");
        int len = comments.length();
        int current = 0;
        int last = 0;
        char[] uu = new char[6];
        uu[0] = '\\';
        uu[1] = 'u';
        while (current < len)
        {
            char c = comments.charAt(current);
            if (c > '\u00ff' || c == '\n' || c == '\r')
            {
                if (last != current)
                {
                    bw.write(comments.substring(last, current));
                }
                if (c > '\u00ff')
                {
                    uu[2] = Util.toHex((c >> 12) & 0xf);
                    uu[3] = Util.toHex((c >> 8) & 0xf);
                    uu[4] = Util.toHex((c >> 4) & 0xf);
                    uu[5] = Util.toHex(c & 0xf);
                    bw.write(new String(uu));
                }
                else
                {
                    bw.newLine();
                    if (c == '\r' && current != len - 1 && comments.charAt(current + 1) == '\n')
                    {
                        current++;
                    }
                    if (current == len - 1 || (comments.charAt(current + 1) != '#' && comments.charAt(current + 1) != '!'))
                    {
                        bw.write("#");
                    }
                }
                last = current + 1;
            }
            current++;
        }
        if (last != current)
        {
            bw.write(comments.substring(last, current));
        }
        bw.newLine();
    }

    public void put(String Key, String Value, String Comment)
    {
        this.put(Key, Value);
        this.putComment(Key, Comment);
    }

    public void putComment(String Key, String Comment)
    {
        ArrayList<String> CommentList = new ArrayList<String>();
        CommentList.add(Comment);
        if (this.Comments.containsKey(Key) == true)
        {
            String[] FoundedComments = this.Comments.get(Key);
            for (int i = 0; i < FoundedComments.length; i++)
            {
                CommentList.add(i, FoundedComments[i]);
            }
        }
        this.Comments.put(Key, CommentList.toArray(new String[CommentList.size()]));
    }
    public void putComment(String Key, String[] Comments)
    {
        for (int i = 0; i < Comments.length; i++)
        {
            this.putComment(Key, Comments[i]);
        }        
    }
    public String[] getComment(String Key)
    {
        String[] Comment = null;
        if (this.Comments.containsKey(Key) == true)
        {
            Comment = this.Comments.get(Key);
        }
        return Comment;
    }

    @Override
    public void store(Writer writer, String comments) throws IOException
    {
        this.store0((writer instanceof BufferedWriter) ? (BufferedWriter) writer : new BufferedWriter(writer), comments, false);
    }
    public void store(File file ) throws IOException
    {
        file.getParentFile().mkdirs();
        FileOutputStream output = new FileOutputStream(file);       
        if (output != null)
        {
            this.store(output, null);
            output.close();
        }
    }
    @Override
    public void store(OutputStream out, String comments) throws IOException
    {
        this.store0(new BufferedWriter(new OutputStreamWriter(out, "8859_1")), comments, true);
    }

    private void store0(BufferedWriter bw, String comments, boolean escUnicode) throws IOException
    {
        synchronized (this)
        {
            for (Enumeration<Object> e = this.keys(); e.hasMoreElements();)
            {
                String key = (String) e.nextElement();
                String val = (String) this.get(key);
                key = this.saveConvert(key, true, escUnicode);
                val = this.saveConvert(val, false, escUnicode);
                if (this.Comments.containsKey(key) == true)
                {
                    String[] FoundedComments = this.Comments.get(key);
                    for (int i = 0; i < FoundedComments.length; i++)
                    {
                        writeComments(bw, FoundedComments[i]);
                    }
                }
                bw.write(key + "=" + val);
                bw.newLine();
            }
        }
        bw.flush();
    }


    public boolean IsLoaded()
    {
        return this.Loaded;
    }
}