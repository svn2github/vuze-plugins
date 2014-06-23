package Model;

import Utils.FileUtils;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 12/03/2010
 * Time: 09:16:12
 * To change this template use File | Settings | File Templates.
 */
public class DownloadHandlerVO {
    private String _userName;
    public String getUserName() {
        return _userName;
    }
    public void setUserName(String value) {
        _userName = value;
    }

    private String _password;
    public String getPassword() {
        return _password;
    }
    public void setPassword(String value) {
        _password = value;
    }

    private SubTitleLanguage _language;
    public SubTitleLanguage getLanguage() {
        return _language;
    }
    public void setLanguage(SubTitleLanguage value) {
        _language = value;
    }

    private FileUtils.SubTitleExtensions _preferedExtSubTitle;
    public FileUtils.SubTitleExtensions getPreferedExtSubTitle() {
        return _preferedExtSubTitle;
    }
    public void setPreferedExtSubTitle(FileUtils.SubTitleExtensions value) {
        _preferedExtSubTitle = value;
    }
}
