package Implementation;

import Exception.DownloadHandlerException;
import Interface.IDownloadHandler;
import Interface.ILogManager;
import Model.SystemInformation;
import Model.*;
import Utils.FileUtils;
import Hash.OpenSubtitlesHasher;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.hamcrest.Matchers;

import java.io.*;
import java.net.URL;
import java.util.*;

import static ch.lambdaj.Lambda.*;
import static ch.lambdaj.Lambda.on;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 12/05/2010
 * Time: 11:08:36
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("unchecked")
public class OpenSubtitlesOrgHandler implements IDownloadHandler {
// ------------------------------ FIELDS ------------------------------

    private String _UserAgent = null;
    private static final String _UrlXmlRpc = "http://api.opensubtitles.org/xml-rpc";
    private XmlRpcClient xmlRpcClient;
    private DownloadHandlerVO _handlerVO;
    private String _tokenConnection;

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface IDownloadHandler ---------------------

    public void setSystemInformation(SystemInformation systemInformation) {
        _UserAgent = String.format("%s v%s",
                systemInformation.getSystemName(), systemInformation.getVersionNumber());
    }

    public void setLogManager(ILogManager logManager) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDescription() {
        return "OpenSubTitles";
    }

    public String getSiteUrl() {
        return "http://www.opensubtitles.org";
    }

    public Class getHandlerVOType() {
        return DownloadHandlerVO.class;
    }

    public LogonType getLogonType() {
        return LogonType.Optional;
    }

    public SubTitleLanguage[] getSupportedLanguages() {
        List<SubTitleLanguage> langs = new ArrayList<SubTitleLanguage>();
        langs.add(SubTitleLanguage.alb);
        langs.add(SubTitleLanguage.ara);
        langs.add(SubTitleLanguage.bul);
        langs.add(SubTitleLanguage.cat);
        langs.add(SubTitleLanguage.chi);
        langs.add(SubTitleLanguage.cze);
        langs.add(SubTitleLanguage.dan);
        langs.add(SubTitleLanguage.dut);
        langs.add(SubTitleLanguage.eng);
        langs.add(SubTitleLanguage.est);
        langs.add(SubTitleLanguage.fin);
        langs.add(SubTitleLanguage.fre);
        langs.add(SubTitleLanguage.geo);
        langs.add(SubTitleLanguage.ger);
        langs.add(SubTitleLanguage.glg);
        langs.add(SubTitleLanguage.ell);
        langs.add(SubTitleLanguage.heb);
        langs.add(SubTitleLanguage.hrv);
        langs.add(SubTitleLanguage.hun);
        langs.add(SubTitleLanguage.ice);
        langs.add(SubTitleLanguage.ind);
        langs.add(SubTitleLanguage.ita);
        langs.add(SubTitleLanguage.jpn);
        langs.add(SubTitleLanguage.kor);
        langs.add(SubTitleLanguage.mac);
        langs.add(SubTitleLanguage.may);
        langs.add(SubTitleLanguage.nor);
        langs.add(SubTitleLanguage.oci);
        langs.add(SubTitleLanguage.per);
        langs.add(SubTitleLanguage.pol);
        langs.add(SubTitleLanguage.por);
        langs.add(SubTitleLanguage.por_BR);
        langs.add(SubTitleLanguage.rus);
        langs.add(SubTitleLanguage.scc);
        langs.add(SubTitleLanguage.sin);
        langs.add(SubTitleLanguage.slo);
        langs.add(SubTitleLanguage.slv);
        langs.add(SubTitleLanguage.spa);
        langs.add(SubTitleLanguage.swe);
        langs.add(SubTitleLanguage.tgl);
        langs.add(SubTitleLanguage.tha);
        langs.add(SubTitleLanguage.tur);
        langs.add(SubTitleLanguage.ukr);
        langs.add(SubTitleLanguage.vie);
        langs.add(SubTitleLanguage.rum);
        return langs.toArray(new SubTitleLanguage[0]);
    }

    public FileUtils.SubTitleExtensions[] getSupportedSubTitleExtensions() {
        return FileUtils.SubTitleExtensions.values();
    }

    public void doLogin(DownloadHandlerVO handlerVO) throws DownloadHandlerException {
        try {
            /* Proxy Configuration for XmlRpcConfig
            Properties systemSettings = System.getProperties();
            systemSettings.put("http.proxyHost","server");
            systemSettings.put("http.proxyPort", "port");
            */
            XmlRpcClientConfigImpl xmlRpcConfig = new XmlRpcClientConfigImpl();
            xmlRpcConfig.setServerURL(new URL(_UrlXmlRpc));
            xmlRpcConfig.setEnabledForExtensions(true);
            xmlRpcConfig.setUserAgent(_UserAgent);
            xmlRpcClient = new XmlRpcClient();
            xmlRpcClient.setConfig(xmlRpcConfig);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        _handlerVO = handlerVO;

        Vector<String> params = new Vector<String>();
        params.add(_handlerVO.getUserName());
        params.add(_handlerVO.getPassword());
        params.add(getCodeLanguage());
        params.add(_UserAgent);

        Map resp = (Map)executeRpcMethod("LogIn", params);

        validarRespStatus((String)resp.get("status"), "LogIn");
        
        _tokenConnection = (String)resp.get("token");
    }

    public List<SubTitleVO> getSubTitleList(VideoFileVO movieFile) {
        List<SubTitleVO> subTitleList = new ArrayList<SubTitleVO>();

        HashMap<String, Object> movieMap = new HashMap<String, Object>();

        movieMap.put("sublanguageid", getCodeLanguage());
        movieMap.put("moviehash", OpenSubtitlesHasher.computeHash(movieFile.getFile()));
        movieMap.put("moviebytesize", movieFile.getSize());

        Vector params = new Vector();
        params.add(_tokenConnection);
        params.add(new Object[] { movieMap });

        Map resp = (Map)executeRpcMethod("SearchSubtitles", params);

        Object respData = resp.get("data");

        if ((respData.getClass() == Boolean.class) && (!(Boolean)respData))
            return subTitleList;

        Object[] subtitlesResp = (Object[])respData;

        for(Object item : subtitlesResp) {
            Map subtitleResp = (Map)item;
            SubTitleVO subTitleVO = new SubTitleVO();
            subTitleVO.setID((String)subtitleResp.get("IDSubtitleFile"));
            subTitleVO.setDescricao((String)subtitleResp.get("MovieName"));
            subTitleVO.setFileName((String)subtitleResp.get("SubFileName"));
            subTitleVO.setCds(Integer.parseInt((String)subtitleResp.get("SubSumCD")));
            subTitleVO.setDownloads(Integer.parseInt((String)subtitleResp.get("SubDownloadsCnt")));
            subTitleVO.setMovieSize(Long.parseLong((String)subtitleResp.get("MovieByteSize")));
            subTitleVO.setRelease((String)subtitleResp.get("MovieReleaseName"));
            subTitleList.add(subTitleVO);
        }
        return subTitleList;
    }

    public SubTitleVO chooseOneSubTitle(VideoFileVO movieFile, List<SubTitleVO> subList) {
        // Filtra pelo tamanho do arquivo, se não sobrar nada ignora, isso nem sempre é confiável
        List<SubTitleVO> possibleSubTitle1 = filter(having(on(SubTitleVO.class).getMovieSize(), Matchers.equalTo(movieFile.getSize())), subList);
        if (possibleSubTitle1.size() == 0)
            possibleSubTitle1 = subList;

        // Tento filtar pela extensão da legenda, podem haver vários tipos
        List<SubTitleVO> possibleSubTitle2 = filter(having(FileUtils.getExtension(on(SubTitleVO.class).getFileName()), Matchers.equalToIgnoringCase(_handlerVO.getPreferedExtSubTitle().toString())), possibleSubTitle1);
        if (possibleSubTitle2.size() == 0)
            possibleSubTitle2 = possibleSubTitle1;

        // Se houver mais de uma legenda para o filme, pega a que tem mais downloads no site
        return selectMax(possibleSubTitle2, on(SubTitleVO.class).getDownloads());
    }

    public InputStream getSubTitleFile(SubTitleVO subTitleVO) {
        Vector params = new Vector();
        params.add(_tokenConnection);
        params.add(new Object[] { subTitleVO.getID() });

        Map resp = (Map)executeRpcMethod("DownloadSubtitles", params);

        validarRespStatus((String)resp.get("status"), "DownloadSubtitles");

        Object[] subtitlesResp = (Object[])resp.get("data");

        if (subtitlesResp.length == 0)
            throw new RuntimeException("No subtitles returned from server");

        return FileUtils.inflateFromGZip(FileUtils.decodeBase64((String)((Map)subtitlesResp[0]).get("data")));
    }

    public void doLogout() {
        if ((xmlRpcClient == null) || (_tokenConnection == null))
            return;

        Vector<String> params = new Vector<String>();
        params.add(_tokenConnection);

        Map resp = (Map)executeRpcMethod("LogOut", params);

        validarRespStatus((String)resp.get("status"), "LogOut");
    }

// -------------------------- OTHER METHODS --------------------------

    private Object executeRpcMethod(String method, List params) {
        try {
            return xmlRpcClient.execute(method, params);
        } catch (XmlRpcException e) {
            throw new RuntimeException("Erro on calling method " + method, e);
        }
    }

    private String getCodeLanguage() {
        try {
            switch (_handlerVO.getLanguage()) {
                case por_BR:
                    return "pob";
                default:
                    return SubTitleLanguage.class.getField(_handlerVO.getLanguage().name()).getAnnotation(CodeISO639_2.class).value();
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void validarRespStatus(String status, String method) {
        if ((!status.equalsIgnoreCase("200 OK")) && (!status.equalsIgnoreCase("206 Partial content; message")))
            throw new RuntimeException("Error on " + method + ": " + status);
    }
}
