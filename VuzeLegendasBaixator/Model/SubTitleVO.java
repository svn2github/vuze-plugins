package Model;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 25/02/2010
 * Time: 18:11:31
 * To change this template use File | Settings | File Templates.
 */
public class SubTitleVO {
    private String _id;
    public String getID() {
        return _id;
    }
    public void setID(String value) {
        _id = value;
    }

    private String _descricao;
    public String getDescricao() {
        return _descricao;
    }
    public void setDescricao(String value) {
        _descricao = value;
    }

    private String _release;
    public String getRelease() {
        return _release;
    }
    public void setRelease(String value) {
        _release = value;
    }

    private int _cds;
    public int getCds() {
        return _cds;
    }
    public void setCds(int value) {
        _cds = value;
    }

    private long _movieSize;
    public long getMovieSize() {
        return _movieSize;
    }
    public void setMovieSize(long value) {
        _movieSize = value;
    }

    private int _fps;
    public int getFps() {
        return _fps;
    }
    public void setFps(int value) {
        _fps = value;
    }

    private int _downloads;
    public int getDownloads() {
        return _downloads;
    }
    public void setDownloads(int value) {
        _downloads = value;
    }

    private String _fileName;
    public String getFileName() {
        return _fileName;
    }
    public void setFileName(String value) {
        _fileName = value;
    }
}
