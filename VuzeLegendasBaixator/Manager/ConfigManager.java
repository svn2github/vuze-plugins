package Manager;

import Interface.IConfigManager;
import Interface.IDownloadHandler;
import Main.Core;
import Model.DownloadHandlerVO;
import Model.SubTitleLanguage;
import Model.SystemInformation;
import Utils.FileUtils;
import Utils.TorrentUtils;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PasswordParameter;
import org.gudy.azureus2.plugins.ui.config.StringListParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

import java.lang.reflect.Method;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: Brunol
 * Date: 26/03/2010
 * Time: 14:44:23
 * To change this template use File | Settings | File Templates.
 */
public class ConfigManager implements IConfigManager {
// ------------------------------ FIELDS ------------------------------

    public static final String BaseName = "VuzeLegendasBaixator";
    private static final String _BaseConfigName = BaseName + ".config";
    private static final String _PluginActive = "Active";
    private static final String _CategoryList = "CategoryList";
    private static final String _CategoryAll = "CategoryAll";
    private static final String _CheckedFiles = "CheckedFiles";
    private static final String _ExcludeFilesRegex = "ExcludeFilesRegex";
    private static final String _IntervalSearch = "IntervalSearch";
    private static final int _IntervalSearchDefault = 1440; // 24 horas em minutos
    private static final String _UseLanguageOnSubtitle = "UseLanguageOnSubtitle";
    private static final String _LanguageOnSubtitle = "LanguageOnSubtitle";

    private PluginConfig pconfig;
    private PluginInterface _pluginInterface;

// -------------------------- STATIC METHODS --------------------------

    public static void initializeConfigPage(final PluginInterface pluginInterface) {
        final BasicPluginConfigModel cfg = pluginInterface.getUIManager().createBasicPluginConfigModel(_BaseConfigName);
        LocaleUtilities localeUtil = pluginInterface.getUtilities().getLocaleUtilities();

        cfg.addBooleanParameter2(_PluginActive, _BaseConfigName + "." + _PluginActive, false);
        cfg.addIntParameter2(_IntervalSearch, _BaseConfigName + "." + _IntervalSearch, _IntervalSearchDefault);
        cfg.addStringParameter2(_ExcludeFilesRegex, _BaseConfigName + "." + _ExcludeFilesRegex, "");

        BooleanParameter useLanguageOnSubTitle =
                cfg.addBooleanParameter2(_UseLanguageOnSubtitle, _BaseConfigName + "." + _UseLanguageOnSubtitle, false);
        String[] subTitleValues = SubTitleLanguage.getValues();
        String[] subTitleLabels = SubTitleLanguage.getDescriptions();
        StringListParameter languageOnSubtitle =
                cfg.addStringListParameter2(_LanguageOnSubtitle, _BaseConfigName + "." + _LanguageOnSubtitle, subTitleValues, subTitleLabels, "");
        useLanguageOnSubTitle.addEnabledOnSelection(languageOnSubtitle);

        // Configuração dos Handlers
        final List<IDownloadHandler> handlersList = SubTitleManager.getExistingHandlers();
        for (IDownloadHandler handler : handlersList) {
            try {
                DownloadHandlerVO handlerVO = (DownloadHandlerVO) handler.getHandlerVOType().newInstance();
                Method[] metodos = handlerVO.getClass().getMethods();

                int paramCount = 2;
                for (Method metodo : metodos)
                    if (metodo.getName().startsWith("set"))
                        paramCount++;

                String nomeHandle = handler.getClass().getSimpleName();
                addLocalisedMessage(localeUtil, nomeHandle, handler.getDescription());
                addLocalisedMessage(localeUtil, handler.getSiteUrl(), handler.getSiteUrl());

                Parameter[] parametros = new Parameter[paramCount];
                parametros[0] = cfg.addHyperlinkParameter2(handler.getSiteUrl(), handler.getSiteUrl());
                parametros[1] = cfg.addBooleanParameter2(nomeHandle, _BaseConfigName + ".Active", false);

                paramCount = 2;
                for (Method metodo : metodos) {
                    if (metodo.getName().startsWith("set")) {
                        Class<?> typeParam = metodo.getParameterTypes()[0];
                        String nomeProp = metodo.getName().substring(3);
                        String nomePropPlugin = handler.getClass().getSimpleName() + "." + nomeProp;
                        String resourceMessage = _BaseConfigName + "." + handlerVO.getClass().getSimpleName() + "." + nomeProp;

                        if ((handler.getLogonType() == IDownloadHandler.LogonType.None) &
                                ((metodo.getName().equals("setUserName")) || (metodo.getName().equals("setPassword"))))
                            continue;

                        if (typeParam == SubTitleLanguage.class) {
                            // Na lista de idiomas coloco só os que o Handler suporta
                            SubTitleLanguage[] enumValores = handler.getSupportedLanguages();
                            String[] valores = SubTitleLanguage.getValues(enumValores);
                            String[] labels = SubTitleLanguage.getDescriptions(enumValores);
                            parametros[paramCount] = cfg.addStringListParameter2(nomePropPlugin, resourceMessage, valores, labels, "");
                        } else if (typeParam == FileUtils.SubTitleExtensions.class) {
                            // Na lista de extensões coloca só as que o Handler suporta
                            FileUtils.SubTitleExtensions[] enumValores = handler.getSupportedSubTitleExtensions();
                            if (enumValores.length > 0) {
                                String[] valores = new String[enumValores.length];
                                for (int j = 0; j < enumValores.length; j++)
                                    valores[j] = enumValores[j].toString();
                                parametros[paramCount] = cfg.addStringListParameter2(nomePropPlugin, resourceMessage, valores, "");
                            }
                        } else if (typeParam == int.class) {
                            parametros[paramCount] = cfg.addIntParameter2(nomePropPlugin, resourceMessage, 0);
                        } else if (typeParam == String.class) {
                            if (nomeProp.equalsIgnoreCase("Password"))
                                parametros[paramCount] = cfg.addPasswordParameter2(nomePropPlugin, resourceMessage, PasswordParameter.ET_PLAIN, new byte[0]);
                            else
                                parametros[paramCount] = cfg.addStringParameter2(nomePropPlugin, resourceMessage, "");
                        } else if (typeParam.isEnum()) {
                            Object[] enumValores = typeParam.getEnumConstants();
                            String[] valores = new String[enumValores.length];
                            for (int j = 0; j < enumValores.length; j++)
                                valores[j] = enumValores[j].toString();
                            parametros[paramCount] = cfg.addStringListParameter2(nomePropPlugin, resourceMessage, valores, "");
                        }
                        ((BooleanParameter) parametros[1]).addEnabledOnSelection(parametros[paramCount]);
                        paramCount++;
                    }
                }
                cfg.createGroup(nomeHandle, parametros);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        // Configuração das Categorias
        String[] existingCategories = getExistingCategories(pluginInterface);
        Parameter[] parametros = new Parameter[existingCategories.length];
        BooleanParameter paramCatAll = cfg.addBooleanParameter2(_CategoryAll, _BaseConfigName + "." + _CategoryAll, true);
        int paramCount = 0;
        for (String category : existingCategories) {
            String nomeCfgCategory = _CategoryList + "." + category;
            String nomeResMessage = _BaseConfigName + "." + nomeCfgCategory;
            addLocalisedMessage(localeUtil, nomeResMessage, category);
            parametros[paramCount] = cfg.addBooleanParameter2(nomeCfgCategory, nomeResMessage, false);
            paramCatAll.addDisabledOnSelection(parametros[paramCount]);
            paramCount++;
        }
        cfg.createGroup(_BaseConfigName + "." + _CategoryList, parametros);
    }

    public static void addLocalisedMessage(LocaleUtilities localeUtil, String name, String value) {
        Properties propsMsg = new Properties();
        propsMsg.put(name, value);
        localeUtil.integrateLocalisedMessageBundle(propsMsg);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public ConfigManager(PluginInterface pluginInterface) {
        pconfig = pluginInterface.getPluginconfig();
        _pluginInterface = pluginInterface;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface IConfigManager ---------------------

    public String getExcludeFilesRegex() {
        return pconfig.getPluginStringParameter(_ExcludeFilesRegex);
    }

    public int getIntervalSearch() {
        // Converte para milisegundos
        return pconfig.getPluginIntParameter(_IntervalSearch, _IntervalSearchDefault) * 60 * 1000;
    }

    public boolean getUseLanguageOnSubtitle() {
        return pconfig.getPluginBooleanParameter(_UseLanguageOnSubtitle, false);
    }

    public SubTitleLanguage getLanguageOnSubtitle() {
        String value = pconfig.getPluginStringParameter(_LanguageOnSubtitle, "");
        if ((value != null) && (!value.isEmpty()))
            return SubTitleLanguage.valueOf(value);
        return null;
    }

    public Map<IDownloadHandler, DownloadHandlerVO> getDownloadHandlers() {
        List<IDownloadHandler> handlersList = SubTitleManager.getExistingHandlers();
        Map<IDownloadHandler, DownloadHandlerVO> handlers = new LinkedHashMap<IDownloadHandler, DownloadHandlerVO>();

        for (IDownloadHandler handler : handlersList) {
            // Se estiver marcado para ser usado busco as informações do VO
            if (pconfig.getPluginBooleanParameter(handler.getClass().getSimpleName(), false)) {

                SystemInformation systemInformation = new SystemInformation();
                systemInformation.setSystemName(Core.SYSTEM_NAME);
                systemInformation.setVersionNumber(Core.VERSION_NUMBER);
                handler.setSystemInformation(systemInformation);
                try {
                    DownloadHandlerVO handlerVO = (DownloadHandlerVO) handler.getHandlerVOType().newInstance();
                    Method[] metodos = handlerVO.getClass().getMethods();
                    for (Method metodo : metodos) {
                        if (metodo.getName().startsWith("set")) {
                            Class<?> typeParam = metodo.getParameterTypes()[0];
                            String nomeProp = metodo.getName().substring(3);
                            String nomePropPlugin = handler.getClass().getSimpleName() + "." + nomeProp;
                            if (typeParam == int.class) {
                                metodo.invoke(handlerVO, pconfig.getPluginIntParameter(nomePropPlugin, 0));
                            } else if (typeParam == String.class) {
                                metodo.invoke(handlerVO, pconfig.getPluginStringParameter(nomePropPlugin, ""));
                            } else if (typeParam.isEnum()) {
                                String valor = pconfig.getPluginStringParameter(nomePropPlugin, "");
                                Object[] enumValores = typeParam.getEnumConstants();
                                for (Object objEnum : enumValores)
                                    if (objEnum.toString().equalsIgnoreCase(valor)) {
                                        metodo.invoke(handlerVO, objEnum);
                                        break;
                                    }
                            }
                        }
                    }
                    handlers.put(handler, handlerVO);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
        return handlers;
    }

// -------------------------- OTHER METHODS --------------------------

    public void addCheckedFile(String checkedFile) {
        String[] checkedFiles = getCheckedFiles();
        String[] newCheckedFiles = new String[checkedFiles.length + 1];
        System.arraycopy(checkedFiles, 0, newCheckedFiles, 0, checkedFiles.length);
        newCheckedFiles[newCheckedFiles.length - 1] = checkedFile;
        setCheckedFiles(newCheckedFiles);
    }

    public String[] getCheckedFiles() {
        return pconfig.getPluginStringListParameter(_CheckedFiles);
    }

    public void setCheckedFiles(String[] checkedFiles) {
        pconfig.setPluginStringListParameter(_CheckedFiles, checkedFiles);
    }

    public String[] getCategoryList() {
        if (getCategoryAll())
            return null;

        String[] existingCategories = getExistingCategories(_pluginInterface);
        String categorias = "";
        for (String category : existingCategories) {
            String nomeCfgCategory = _CategoryList + "." + category;
            if (pconfig.getPluginBooleanParameter(nomeCfgCategory, false))
                categorias += category + ";";
        }
        if (categorias.indexOf(";") > 0)
            return categorias.substring(0, categorias.length() - 1).split(";");
        else
            return new String[0];
    }

    public boolean getCategoryAll() {
        return pconfig.getPluginBooleanParameter(_CategoryAll, true);
    }

    public static String[] getExistingCategories(PluginInterface pluginInterface) {
        List<String> categories = new ArrayList<String>();
        Download[] torrents = pluginInterface.getDownloadManager().getDownloads();
        TorrentAttribute ta = TorrentUtils.getCategoryAttr(pluginInterface);
        for (Download torrent : torrents) {
            String category = torrent.getAttribute(ta);
            if (category == null)
                continue;
            if (!TorrentUtils.hasMovieFile(torrent))
                continue;
            if ((!category.isEmpty()) && (categories.indexOf(category) == -1))
                categories.add(category);
        }
        return categories.toArray(new String[0]);
    }

    public boolean getPluginActive() {
        return pconfig.getPluginBooleanParameter(_PluginActive, false);
    }
}
