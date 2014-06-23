/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Interface;

import Model.DownloadHandlerVO;
import Model.SubTitleLanguage;

import java.util.Map;

/**
 *
 * @author Bruno
 */
public interface IConfigManager {
    String getExcludeFilesRegex();
    int getIntervalSearch();
    boolean getUseLanguageOnSubtitle();
    SubTitleLanguage getLanguageOnSubtitle();
    Map<IDownloadHandler, DownloadHandlerVO> getDownloadHandlers();
}
