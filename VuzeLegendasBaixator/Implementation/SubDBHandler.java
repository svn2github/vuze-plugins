package Implementation;

import Exception.DownloadHandlerException;
import Hash.SubDBHasher;
import Interface.IDownloadHandler;
import Interface.ILogManager;
import Model.SystemInformation;
import Model.*;
import Utils.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.selectMax;

/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 20/05/2010
 * Time: 10:18:39
 * To change this template use File | Settings | File Templates.
 */
public class SubDBHandler implements IDownloadHandler {
// ------------------------------ FIELDS ------------------------------

    private String _UserAgent = null;
    private static final String _BaseUrl = "http://api.thesubdb.com/";
    private DefaultHttpClient httpclient = null;
    private DownloadHandlerVO _handlerVO = null;

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface IDownloadHandler ---------------------

    public void setSystemInformation(SystemInformation systemInformation) {
        _UserAgent = String.format("SubDB/1.0 (%s/%s; %s)",
                systemInformation.getSystemName(), systemInformation.getVersionNumber(), URL);
    }

    public void setLogManager(ILogManager logManager) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDescription() {
        return "SubDB";
    }

    public String getSiteUrl() {
        return "http://blog.thesubdb.com";
    }

    public Class getHandlerVOType() {
        return DownloadHandlerVO.class;
    }

    public LogonType getLogonType() {
        return LogonType.None;
    }

    public SubTitleLanguage[] getSupportedLanguages() {
        List<SubTitleLanguage> langs = new ArrayList<SubTitleLanguage>();
        langs.add(SubTitleLanguage.afr);
        langs.add(SubTitleLanguage.alb);
        langs.add(SubTitleLanguage.amh);
        langs.add(SubTitleLanguage.ara);
        langs.add(SubTitleLanguage.arm);
        langs.add(SubTitleLanguage.aze);
        langs.add(SubTitleLanguage.baq);
        langs.add(SubTitleLanguage.bel);
        langs.add(SubTitleLanguage.ben);
        langs.add(SubTitleLanguage.bih);
        langs.add(SubTitleLanguage.bre);
        langs.add(SubTitleLanguage.bul);
        langs.add(SubTitleLanguage.bur);
        langs.add(SubTitleLanguage.cat);
        langs.add(SubTitleLanguage.chr);
        langs.add(SubTitleLanguage.chi);
        langs.add(SubTitleLanguage.cos);
        langs.add(SubTitleLanguage.hrv);
        langs.add(SubTitleLanguage.cze);
        langs.add(SubTitleLanguage.dan);
        langs.add(SubTitleLanguage.div);
        langs.add(SubTitleLanguage.dut);
        langs.add(SubTitleLanguage.eng);
        langs.add(SubTitleLanguage.epo);
        langs.add(SubTitleLanguage.est);
        langs.add(SubTitleLanguage.fao);
        langs.add(SubTitleLanguage.tgl);
        langs.add(SubTitleLanguage.fin);
        langs.add(SubTitleLanguage.fre);
        langs.add(SubTitleLanguage.fry);
        langs.add(SubTitleLanguage.glg);
        langs.add(SubTitleLanguage.geo);
        langs.add(SubTitleLanguage.ger);
        langs.add(SubTitleLanguage.ell);
        langs.add(SubTitleLanguage.guj);
        langs.add(SubTitleLanguage.hat);
        langs.add(SubTitleLanguage.heb);
        langs.add(SubTitleLanguage.hin);
        langs.add(SubTitleLanguage.ice);
        langs.add(SubTitleLanguage.ind);
        langs.add(SubTitleLanguage.gle);
        langs.add(SubTitleLanguage.ita);
        langs.add(SubTitleLanguage.jpn);
        langs.add(SubTitleLanguage.jav);
        langs.add(SubTitleLanguage.kan);
        langs.add(SubTitleLanguage.kaz);
        langs.add(SubTitleLanguage.khm);
        langs.add(SubTitleLanguage.kor);
        langs.add(SubTitleLanguage.kur);
        langs.add(SubTitleLanguage.kir);
        langs.add(SubTitleLanguage.lao);
        langs.add(SubTitleLanguage.hun);
        langs.add(SubTitleLanguage.lat);
        langs.add(SubTitleLanguage.lav);
        langs.add(SubTitleLanguage.lit);
        langs.add(SubTitleLanguage.ltz);
        langs.add(SubTitleLanguage.lit);
        langs.add(SubTitleLanguage.mac);
        langs.add(SubTitleLanguage.may);
        langs.add(SubTitleLanguage.mal);
        langs.add(SubTitleLanguage.mlt);
        langs.add(SubTitleLanguage.mao);
        langs.add(SubTitleLanguage.mar);
        langs.add(SubTitleLanguage.mon);
        langs.add(SubTitleLanguage.nep);
        langs.add(SubTitleLanguage.nor);
        langs.add(SubTitleLanguage.oci);
        langs.add(SubTitleLanguage.ori);
        langs.add(SubTitleLanguage.pus);
        langs.add(SubTitleLanguage.per);
        langs.add(SubTitleLanguage.por);
        langs.add(SubTitleLanguage.por_BR);
        langs.add(SubTitleLanguage.pan);
        langs.add(SubTitleLanguage.que);
        langs.add(SubTitleLanguage.rum);
        langs.add(SubTitleLanguage.rus);
        langs.add(SubTitleLanguage.san);
        langs.add(SubTitleLanguage.gla);
        langs.add(SubTitleLanguage.scc);
        langs.add(SubTitleLanguage.snd);
        langs.add(SubTitleLanguage.sin);
        langs.add(SubTitleLanguage.slo);
        langs.add(SubTitleLanguage.slv);
        langs.add(SubTitleLanguage.spa);
        langs.add(SubTitleLanguage.sun);
        langs.add(SubTitleLanguage.swa);
        langs.add(SubTitleLanguage.swe);
        langs.add(SubTitleLanguage.syc);
        langs.add(SubTitleLanguage.tgk);
        langs.add(SubTitleLanguage.tam);
        langs.add(SubTitleLanguage.tat);
        langs.add(SubTitleLanguage.tha);
        langs.add(SubTitleLanguage.tib);
        langs.add(SubTitleLanguage.ton);
        langs.add(SubTitleLanguage.tur);
        langs.add(SubTitleLanguage.ukr);
        langs.add(SubTitleLanguage.urd);
        langs.add(SubTitleLanguage.uzb);
        langs.add(SubTitleLanguage.uig);
        langs.add(SubTitleLanguage.vie);
        langs.add(SubTitleLanguage.wel);
        langs.add(SubTitleLanguage.yid);
        return langs.toArray(new SubTitleLanguage[0]);
    }

    public FileUtils.SubTitleExtensions[] getSupportedSubTitleExtensions() {
        return new FileUtils.SubTitleExtensions[0];
    }

    public void doLogin(DownloadHandlerVO handlerVO) throws DownloadHandlerException {
        httpclient = new DefaultHttpClient();
        // No login in this site
        _handlerVO = handlerVO;
    }

    public List<SubTitleVO> getSubTitleList(VideoFileVO movieFile) {
        String movieHash = SubDBHasher.computeHash(movieFile.getFile());
        HttpGet httpGet = new HttpGet(getURLForDownload(movieHash));
        httpGet.addHeader("User-Agent", _UserAgent);
        try {
            HttpResponse response = httpclient.execute(httpGet);

            List<SubTitleVO> subTitleList = new ArrayList<SubTitleVO>();

            if (response.getStatusLine().getStatusCode() == 200) {
                HttpEntity ent = response.getEntity();

                String contentDisp = response.getFirstHeader("Content-Disposition").getValue();
                contentDisp = contentDisp.replace(" attachment; filename=", "");

                InputStream entStream = ent.getContent();
                
                SubTitleVO subTitle = new SubTitleVO();
                subTitle.setID(movieHash);
                subTitle.setFileName(contentDisp);
                // Put subtitle here, this way I don't have do get it again
                subTitle.setDescricao(FileUtils.InputToString(entStream, "ISO-8859-1"));
                subTitleList.add(subTitle);

                ent.consumeContent();
            } else
            if (response.getStatusLine().getStatusCode() != 404) {
               throw new RuntimeException("Status Response Error: " + response.getStatusLine());
            }

            return subTitleList;            
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public SubTitleVO chooseOneSubTitle(VideoFileVO movieFile, List<SubTitleVO> subList) {
        // Se houver mais de uma legenda para o filme, pega a que tem mais downloads no site, deve ser a melhor...
        return selectMax(subList, on(SubTitleVO.class).getDownloads());
    }

    public InputStream getSubTitleFile(SubTitleVO subTitleVO) {
        return FileUtils.StringToInput(subTitleVO.getDescricao());
    }

    public void doLogout() {
        // No login in this site
    }

// -------------------------- OTHER METHODS --------------------------

    private String getURLForDownload(String hash) {
        return _BaseUrl + "?action=download&language=" + getCodeLanguage() + "&hash=" + hash;
    }

    private String getCodeLanguage() {
        try {
            switch (_handlerVO.getLanguage()) {
                case por_BR:
                    return "pt";
                case heb:
                    return "iw";
                case jav:
                    return "jw";
                case syc:
                    return "syr";
                default:
                    return SubTitleLanguage.class.getField(_handlerVO.getLanguage().name()).getAnnotation(CodeISO639_1.class).value();
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }
}
