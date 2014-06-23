/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Implementation;

import Exception.DownloadHandlerException;
import Interface.IDownloadHandler;
import Interface.ILogManager;
import Model.*;
import Utils.FileUtils;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.util.List;

/**
 *
 * @author Brunol
 */
public class Addic7edHandler implements IDownloadHandler {
    private String _UserAgent = null;
    private static final String _BaseUrl = "http://api.thesubdb.com/";
    private DefaultHttpClient httpclient = null;
    private DownloadHandlerVO _handlerVO = null;

    private String getCodeLanguage() {
        switch (_handlerVO.getLanguage()) {
            /*
            case pt_BR: return "pt-br";
            case en_US: return "en";
            case it_IT: return "it";
            case ro_RO: return "ro";
            case es_ES: return "es";
            case fr_FR: return "fr";
            case el_GR: return "el";
            case ar_AE: return "ar";
            case de_DE: return "de";
            case hr_HR: return "hr";
            case id_ID: return "id";
            case he_IL: return "he";
            case ru_RU: return "ru";
            case tr_TR: return "tr";
            case sl_SI: return "se";
            case cs_CZ: return "cs";
            case nl_NL: return "nl";
            case hu_HU: return "hu";
            */
            default: return null;
        }
    }

    public void setSystemInformation(SystemInformation systemInformation) {

    }

    public void setLogManager(ILogManager logManager) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDescription() {
        return "Addic7ed";
    }

    public String getSiteUrl() {
        return "http://www.addic7ed.com";
    }

    public Class getHandlerVOType() {
        return DownloadHandlerVO.class;
    }

    public LogonType getLogonType() {
        return LogonType.None;
    }

    public SubTitleLanguage[] getSupportedLanguages() {
        SubTitleLanguage[] langs = new SubTitleLanguage[3];
        /*langs[0] = SubTitleLanguage.pt_BR;
        langs[1] = SubTitleLanguage.en_US;
        langs[2] = SubTitleLanguage.it_IT;
        langs[3] = SubTitleLanguage.ro_RO;
        langs[4] = SubTitleLanguage.es_ES;
        langs[5] = SubTitleLanguage.fr_FR;
        langs[6] = SubTitleLanguage.el_GR;
        langs[7] = SubTitleLanguage.ar_AE;
        langs[8] = SubTitleLanguage.de_DE;
        langs[9] = SubTitleLanguage.hr_HR;
        langs[10] = SubTitleLanguage.id_ID;
        langs[11] = SubTitleLanguage.he_IL;
        langs[12] = SubTitleLanguage.ru_RU;
        langs[13] = SubTitleLanguage.tr_TR;
        langs[14] = SubTitleLanguage.sl_SI;
        langs[15] = SubTitleLanguage.cs_CZ;
        langs[16] = SubTitleLanguage.nl_NL;
        langs[17] = SubTitleLanguage.hu_HU;
        */
        return langs;
    }

    public FileUtils.SubTitleExtensions[] getSupportedSubTitleExtensions() {
        return new FileUtils.SubTitleExtensions[0];
    }

    public void doLogin(DownloadHandlerVO handlerVO) throws DownloadHandlerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<SubTitleVO> getSubTitleList(VideoFileVO movieFile) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SubTitleVO chooseOneSubTitle(VideoFileVO movieFile, List<SubTitleVO> subList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public InputStream getSubTitleFile(SubTitleVO subTitleVO) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void doLogout() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
