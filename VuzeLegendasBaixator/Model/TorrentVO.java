/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Model;

import java.util.List;

/**
 *
 * @author Bruno
 */
public class TorrentVO {
    private String _torrentName;
    public String getTorrentName() {
        return _torrentName;
    }
    public void setTorrentName(String value) {
        _torrentName = value;
    }

    private String _category;
    public String getCategory() {
        return _category;
    }
    public void setCategory(String value) {
        _category = value;
    }

    private List<VideoFileVO> _videoFileList;
    public List<VideoFileVO> getVideoFileList() {
        return _videoFileList;
    }
    public void setVideoFileList(List<VideoFileVO> value) {
        _videoFileList = value;
    }
}
