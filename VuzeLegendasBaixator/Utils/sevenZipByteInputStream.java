package Utils;

import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.SevenZipException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 13/05/2010
 * Time: 14:54:01
 * To change this template use File | Settings | File Templates.
 */
public class sevenZipByteInputStream implements IInStream {
    byte[] _bytes;
    long _position = 0;

    public sevenZipByteInputStream(byte[] bytes) {
        _bytes = bytes;
    }

    public long seek(long l, int i) throws SevenZipException {
        switch (i) {
            case IInStream.SEEK_SET:
                _position = l;
                break;
            case IInStream.SEEK_CUR:
                _position = _position + l;
                break;
            case IInStream.SEEK_END:
                _position = _bytes.length - l;
                break;
        }
        if (_position < 0)
            _position = 0;
        if (_position > _bytes.length)
            _position = _bytes.length;
        return _position;
    }

    public int read(byte[] bytes) throws SevenZipException {
        int bytesLeft = _bytes.length - (int)_position;
        int bytesToRead = bytes.length;
        if (bytesToRead > bytesLeft)
            bytesToRead = bytesLeft;

        System.arraycopy(_bytes, (int)_position, bytes, 0, bytesToRead);
        _position += bytesToRead;
        
        return bytesToRead;
    }
}
