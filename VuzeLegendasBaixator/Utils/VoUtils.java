/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Utils;

import Model.SubTitleLanguage;
import Model.VideoFileVO;
import java.io.File;
import java.util.regex.Pattern;

/**
 *
 * @author Brunol
 */
public class VoUtils {
    
    public static VideoFileVO fileToMovieVO(File diskFile, SubTitleLanguage subTitleLanguage) {
        String regexTvShow = "(.*)\\.[sS](\\d{2})[eExX](\\d{2,4}).*(xvid|x264|h.264).(.[^\\.]*).*";
        Pattern patternTvShow = Pattern.compile(regexTvShow, Pattern.CASE_INSENSITIVE);

        VideoFileVO videoFileVO = new VideoFileVO();
        videoFileVO.setFileName(diskFile.getName());
        videoFileVO.setPathDir(FileUtils.getPathWithoutFileName(diskFile.getPath()));
        videoFileVO.setHasSubTitle(FileUtils.hasSubTitleFile(videoFileVO.getPathDir(), videoFileVO.getFileName(), subTitleLanguage));
        videoFileVO.setSize(diskFile.length());
        videoFileVO.setFile(diskFile);
        videoFileVO.setIsTvShow(patternTvShow.matcher(videoFileVO.getFileName()).find());
        return videoFileVO;
    }

}
