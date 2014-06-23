package Implementation;

import Interface.IDownloadHandler;
import Interface.ILogManager;
import Model.*;
import Utils.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.hamcrest.Matchers;
import Exception.DownloadHandlerException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ch.lambdaj.Lambda.*;

public class LegendasTVHandler implements IDownloadHandler {
// ------------------------------ FIELDS ------------------------------

    private DefaultHttpClient httpclient = null;
    private DownloadHandlerVO _handlerVO = null;
    private boolean isLogged = false;
    private final String _pageEncoding = "iso-8859-1";
    private ILogManager logManager;

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface IDownloadHandler ---------------------

    public void setSystemInformation(SystemInformation systemInformation) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setLogManager(ILogManager logManager) {
        this.logManager = logManager;
    }

    public String getDescription() {
        return "Legendas.TV";
    }

    public String getSiteUrl() {
        return "http://www.legendas.tv";
    }

    public Class getHandlerVOType() {
        return DownloadHandlerVO.class;
    }

    public LogonType getLogonType() {
        return LogonType.Required;
    }

    public SubTitleLanguage[] getSupportedLanguages() {
        List<SubTitleLanguage> langs = new ArrayList<SubTitleLanguage>();
        langs.add(SubTitleLanguage.eng);
        langs.add(SubTitleLanguage.spa);
        langs.add(SubTitleLanguage.por_BR);
        langs.add(SubTitleLanguage.por);
        return langs.toArray(new SubTitleLanguage[0]);
    }

    public FileUtils.SubTitleExtensions[] getSupportedSubTitleExtensions() {
        return FileUtils.SubTitleExtensions.values();
    }

    public void doLogin(DownloadHandlerVO handlerVO) throws DownloadHandlerException {
        if ((handlerVO.getUserName() == null) || (handlerVO.getPassword() == null))
            throw new DownloadHandlerException();

        _handlerVO = handlerVO;

        httpclient = new DefaultHttpClient();

        try {
            HttpPost httpost = new HttpPost(getURLForAction("login_verificar.php"));

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("txtLogin", _handlerVO.getUserName()));
            nvps.add(new BasicNameValuePair("txtSenha", _handlerVO.getPassword()));
            nvps.add(new BasicNameValuePair("chkLogin", "1"));

            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.ISO_8859_1));

            HttpResponse response = httpclient.execute(httpost);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Status Response Error: " + response.getStatusLine());
            }

            HttpEntity ent = response.getEntity();
            InputStream entStream = ent.getContent();

            String respHTML = FileUtils.InputToString(entStream, _pageEncoding);
            if (respHTML.indexOf("Bem vindo(a), voce está sendo redirecionado(a)!") < 0) {
                throw new DownloadHandlerException();
            }
            isLogged = true;
            // Consome o resto da resposta para não entupir o pipeline do httpclient
            ent.consumeContent();
        } catch (IOException e) {
            throw new RuntimeException("Login Error:" + e.getMessage(), e);
        }
    }

    public InputStream getSubTitleFile(SubTitleVO subTitleVO) {
        try {
            String urlPost = getURLForAction(String.format("info.php?d=%s&c=1", subTitleVO.getID()));
            HttpPost httpost = new HttpPost(urlPost);

            HttpResponse response = httpclient.execute(httpost);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Status Response Error: " + response.getStatusLine());
            }

            HttpEntity ent = response.getEntity();
            InputStream entStream = ent.getContent();

            InputStream subTitleStream = null;

            Map<String, InputStream> rarFiles = FileUtils.DecompressRar(entStream);
            if (rarFiles.size() == 0)
                throw new RuntimeException("Rar File is empty");

            String fileRarNameChoosen = null;

            logManager.debug("files rar: " + rarFiles.keySet().toString());

            // Primeiro vejo se tem uma legenda com o nome do arquivo
            for (String fileRarName : rarFiles.keySet()) {
                String fileNameSub = FileUtils.getFileNameWithoutExtension(fileRarName);
                String fileNameMovie = FileUtils.getFileNameWithoutExtension(subTitleVO.getFileName());
                if (fileNameSub.equalsIgnoreCase(fileNameMovie)) {
                    fileRarNameChoosen = fileRarName;
                    if (FileUtils.getExtension(fileRarName).equalsIgnoreCase(_handlerVO.getPreferedExtSubTitle().toString()))
                        break;
                }
            }
            // Se não achei faço o contrário, vejo se o nome do arquivo começa com o nome da legenda
            // as vezes o nome da legenda omite alguma parte do final do nome do arquivo
            if (fileRarNameChoosen == null) {
                for (String fileRarName : rarFiles.keySet()) {
                    String fileNameSub = FileUtils.getFileNameWithoutExtension(fileRarName);
                    String fileNameMovie = FileUtils.getFileNameWithoutExtension(subTitleVO.getFileName());
                    if (fileNameMovie.toLowerCase().startsWith(fileNameSub.toLowerCase())) {
                        fileRarNameChoosen = fileRarName;
                        if (FileUtils.getExtension(fileRarName).equalsIgnoreCase(_handlerVO.getPreferedExtSubTitle().toString()))
                            break;
                    }
                }
            }

            subTitleVO.setFileName(fileRarNameChoosen);
            subTitleStream = rarFiles.get(fileRarNameChoosen);
            ent.consumeContent();

            if (subTitleStream == null)
                logManager.debug("No files from: + urlPost");

            return subTitleStream;
        } catch (Exception e) {
            throw new RuntimeException("Download Error:" + e.getMessage(), e);
        }
    }

    public void doLogout() {
        try {
            if (isLogged) {
                HttpPost httpost = new HttpPost(getURLForAction("logoff.php"));
                HttpResponse response = httpclient.execute(httpost);

                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("Status Response Error: " + response.getStatusLine());
                }
                HttpEntity ent = response.getEntity();
                // Consome o resto da resposta para n�o entupir o pipeline do httpclient
                ent.consumeContent();
            }
        } catch (IOException e) {
            throw new RuntimeException("Login Error:" + e.getMessage(), e);
        }
        httpclient.getConnectionManager().shutdown();
    }

// -------------------------- OTHER METHODS --------------------------

    public SubTitleVO chooseOneSubTitle(VideoFileVO movieFile, List<SubTitleVO> subList) {
        // A busca de legenda nesse site é muito incerta pois eles não possuem API, esse Handler
        // é uma gambiarra para conseguir as legendas, a busca mais confiável a do nome do arquivo
        // na Descrição da legenda
        List<SubTitleVO> possibleSubTitle = new ArrayList<SubTitleVO>();
        
        // Vou procurar na Descrição da Legenda pelo nome do arquivo sem a extensão
        // Na descrição normalmente tem "Para os releases: <nome do arquivo>"
        String movieName = FileUtils.getFileNameWithoutExtension(movieFile.getFileName());
        Pattern patternRelease = Pattern.compile(movieName, Pattern.CASE_INSENSITIVE);
        for (SubTitleVO subTitleVO : subList) {
            HttpPost httpost = new HttpPost(getURLForAction(String.format("info.php?d=%s", subTitleVO.getID())));
            try {
                HttpResponse response = httpclient.execute(httpost);

                if (response.getStatusLine().getStatusCode() != 200) {
                   throw new RuntimeException("Status Response Error: " + response.getStatusLine());
                }

                HttpEntity ent = response.getEntity();
                InputStream entStream = ent.getContent();
                String respHTML = FileUtils.InputToString(entStream, _pageEncoding);
                Matcher matcher = patternRelease.matcher(respHTML);
                ent.consumeContent();
                // Se achei na descrição então é essa, já caio fora
                if (matcher.find()) {
                    possibleSubTitle.add(subTitleVO);
                    break;
                }
            } catch (Exception e) {
                throw new RuntimeException("Download Error:" + e.getMessage(), e);
            }
        }
        // Se não retornou nada, coloco tudo de volta na lista, quer dizer que não deu certo
        if (possibleSubTitle.size() == 0)
            possibleSubTitle.addAll(subList);

        // Filtra pelo tamanho do arquivo se tem mais de um ainda, se não sobrar nada ignora (isso é pouco confiável nesse Handler)
        if (possibleSubTitle.size() > 1) {
            List<SubTitleVO> possibleSubTitle2 = filter(having(on(SubTitleVO.class).getMovieSize(), Matchers.equalTo(movieFile.getSize())), subList);
            if (possibleSubTitle2.size() > 0)
                possibleSubTitle = possibleSubTitle2;
        }

        // Se houver mais de uma legenda para o filme, pega a que tem mais downloads no site, deve ser a melhor...
        return selectMax(possibleSubTitle, on(SubTitleVO.class).getDownloads());
    }

    public List<SubTitleVO> getSubTitleList(VideoFileVO movieFile) {
        String regexInfSubTitle = "\\<span onmouseover.*?gpop\\('([^']+)','([^']+)','([^']+)','([^']+)','([^']+)','([\\d]+)MB','([^']+)',.*?abredown\\('(\\w+)'\\)";
        String regexInfTvShow = ".*\\.[sS]\\d{2}.?(-?[eExX]\\d{2})+";
        Pattern patternSubTitle = Pattern.compile(regexInfSubTitle, Pattern.CASE_INSENSITIVE);
        Pattern patternTvShow = Pattern.compile(regexInfTvShow, Pattern.CASE_INSENSITIVE);
        List<SubTitleVO> subTitleList = new ArrayList<SubTitleVO>();

        try {
            HttpPost httpost = new HttpPost(getURLForAction("index.php?opcao=buscarlegenda"));
            // Busca pelo nome direto só sem extensão
            String txtLegenda = FileUtils.getFileNameWithoutExtension(movieFile.getFileName());
            // Se for um seriado busco apenas pelo nome, temporada e episódio
            Matcher matcherTvShow = patternTvShow.matcher(movieFile.getFileName());
            if (matcherTvShow.find())
                txtLegenda = matcherTvShow.group();

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("txtLegenda", txtLegenda));
            nvps.add(new BasicNameValuePair("selTipo", "1"));
            nvps.add(new BasicNameValuePair("int_idioma", getCodeLanguage(_handlerVO.getLanguage())));

            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.ISO_8859_1));

            HttpResponse response = httpclient.execute(httpost);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Status Response Error: " + response.getStatusLine());
            }

            HttpEntity ent = response.getEntity();
            InputStream entStream = ent.getContent();
            String respHTML = FileUtils.InputToString(entStream, _pageEncoding);
            Matcher matcher = patternSubTitle.matcher(respHTML);

            while (matcher.find()) {
                SubTitleVO subTitleVO = new SubTitleVO();
                subTitleVO.setID(matcher.group(8));
                subTitleVO.setDescricao(matcher.group(1));
                subTitleVO.setCds(Integer.parseInt(matcher.group(4)));
                subTitleVO.setRelease(matcher.group(3));
                subTitleVO.setMovieSize(Integer.parseInt(matcher.group(6)) * 1024 * 1024); // MB -> Bytes
                subTitleVO.setFps(Integer.parseInt(matcher.group(5)));
                subTitleVO.setDownloads(Integer.parseInt(matcher.group(7)));
                // Ainda não tenho o nome do arquivo da legenda então coloco o nome do arquivo do filme pra usar depois
                subTitleVO.setFileName(movieFile.getFileName());
                subTitleList.add(subTitleVO);
            }
            ent.consumeContent();
            return subTitleList;
        } catch (Exception e) {
            throw new RuntimeException("getSubTitleList Error:" + e.getMessage(), e);
        }
    }

    private String getURLForAction(String action) {
        return "http://legendas.tv/" + action;
    }

    private String getCodeLanguage(SubTitleLanguage language) {
        switch (language) {
            case por_BR: return "1";
            case por: return "10";
            case eng: return "2";
            case spa: return "3";
            default: return null;
        }
    }
}
