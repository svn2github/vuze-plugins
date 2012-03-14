/*
 * RSSFeed - Azureus2 Plugin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package org.kmallan.azureus.rssfeed;

import org.gudy.azureus2.plugins.download.Download;

import java.io.Serializable;
import java.util.regex.*;

public class FilterBean implements Serializable {

  static final long serialVersionUID = -979691945084080240L;

  private String name, storeDir, expression, category, type, mode;
  private int state, priority, rateUpload, rateDownload, startSeason, startEpisode, endSeason, endEpisode;
  private long filtId, urlId = 0;
  private boolean isRegex, matchTitle, matchLink, moveTop, customRate, renameFile, renameIncEpisode, disableAfter, cleanFile, enabled;
  private boolean smartHistory = true;

  private String exprLower;
  private Pattern exprPat;

  public static Pattern epsnnenn_snnenn = Pattern.compile("(.*?)" + "s([0-9]+)e([0-9]+)[\\-\\+]s([0-9]+)e([0-9]+)" + ".*?");
  public static Pattern epsnnenn_enn = Pattern.compile("(.*?)" + "s([0-9]+)e([0-9]+)[\\-\\+]e([0-9]+)" + ".*?");
  public static Pattern epsnnenn_nn = Pattern.compile("(.*?)" + "s([0-9]+)e([0-9]+)[\\-\\+]([0-9]+)" + ".*?");
  public static Pattern epsnnenn = Pattern.compile("(.*?)" + "s([0-9]+)e([0-9]+)" + ".*?");
  public static Pattern epnnxnn_nnxnn = Pattern.compile("(.*?)" + "([0-9]+)x([0-9]+)[\\-\\+]([0-9]+)x([0-9]+)" + ".*?");
  public static Pattern epnnxnn_nn = Pattern.compile("(.*?)" + "([0-9]+)x([0-9]+)[\\-\\+]([0-9]+)" + ".*?");
  public static Pattern epnnxnn = Pattern.compile("(.*?)" + "([0-9]+)x([0-9]+)" + ".*?");
  public static Pattern epnnnn_nnnn = Pattern.compile("(.*?)" + "([0-9]+)([0-9]{2})[\\-\\+]([0-9]+)([0-9]{2})" + ".*?");
  public static Pattern epnnnn_nn = Pattern.compile("(.*?)" + "([0-9]+)([0-9]{2})[\\-\\+]([0-9]{2})" + ".*?");
  public static Pattern epnnnn = Pattern.compile("(.*?)" + "([0-9]+)([0-9]{2})" + ".*?");

  public FilterBean() {
    filtId = System.currentTimeMillis();

    exprLower = "";
    exprPat = Pattern.compile(".*" + exprLower + ".*");
  }

  public long getID() {
    return filtId;
  }

  public void setID(long filtId) {
    this.filtId = filtId;
  }

  public String getName() {
    if(name == null) name = "";
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStoreDir() {
    if(storeDir == null) storeDir = "";
    return storeDir;
  }

  public void setStoreDir(String storeDir) {
    this.storeDir = storeDir;
  }

  public String getExpression() {
    if(expression == null) expression = "";
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
    exprLower = expression.toLowerCase();
    try {
      exprPat = Pattern.compile(".*" + exprLower + ".*");
    } catch (PatternSyntaxException e) {
      exprPat = null;
    }
  }

  public boolean getIsRegex() {
    return isRegex;
  }

  public void setIsRegex(boolean isRegex) {
    this.isRegex = isRegex;
  }

  public boolean getMatchTitle() {
    return matchTitle;
  }

  public void setMatchTitle(boolean matchTitle) {
    this.matchTitle = matchTitle;
  }

  public boolean getMatchLink() {
    return matchLink;
  }

  public void setMatchLink(boolean matchLink) {
    this.matchLink = matchLink;
  }

  public boolean getMoveTop() {
    return moveTop;
  }

  public void setMoveTop(boolean moveTop) {
    this.moveTop = moveTop;
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }

  public int getPriority() {
    if(priority != Download.PR_HIGH_PRIORITY && priority != Download.PR_LOW_PRIORITY)
      priority = Download.PR_HIGH_PRIORITY;
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public boolean getRateUseCustom() {
    return customRate;
  }

  public void setRateUseCustom(boolean customRate) {
    this.customRate = customRate;
  }

  public int getRateUpload() {
    return rateUpload;
  }

  public void setRateUpload(int rateUpload) {
    this.rateUpload = rateUpload;
  }

  public int getRateDownload() {
    return rateDownload;
  }

  public void setRateDownload(int rateDownload) {
    this.rateDownload = rateDownload;
  }

  public String getCategory() {
    if(category == null) category = "";
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public long getFeed() {
    return urlId;
  }

  public void setFeed(long urlId) {
    this.urlId = urlId;
  }

  public String getType() {
    if(type == null) type = "";
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getStartSeason() {
    return startSeason;
  }

  public void setStartSeason(int startSeason) {
    this.startSeason = startSeason;
  }

  public int getStartEpisode() {
    return startEpisode;
  }

  public void setStartEpisode(int startEpisode) {
    this.startEpisode = startEpisode;
  }

  public int getEndSeason() {
    return endSeason;
  }

  public void setEndSeason(int endSeason) {
    this.endSeason = endSeason;
  }

  public int getEndEpisode() {
    return endEpisode;
  }

  public void setEndEpisode(int endEpisode) {
    this.endEpisode = endEpisode;
  }

  public boolean getRenameFile() {
    return renameFile;
  }

  public void setRenameFile(boolean renameFile) {
    this.renameFile = renameFile;
  }

  public boolean getRenameIncEpisode() {
    return renameIncEpisode;
  }

  public void setRenameIncEpisode(boolean renameIncEpisode) {
    this.renameIncEpisode = renameIncEpisode;
  }

  public boolean getDisableAfter() {
    return disableAfter;
  }

  public void setDisableAfter(boolean disableAfter) {
    this.disableAfter = disableAfter;
  }

  public boolean getCleanFile() {
    return cleanFile;
  }

  public void setCleanFile(boolean cleanFile) {
    this.cleanFile = cleanFile;
  }

  public String getMode() {
    if(mode == null) mode = "";
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean matches(long urlId, String title, String link) {
    if(!getEnabled()) return false;
    if(getFeed() != 0 && urlId != getFeed()) return false;

    boolean matched = false;
    if((getMatchTitle()) && (match(title))) matched = true;
    else if((getMatchLink()) && (match(link))) matched = true;
    if(!matched) return false;

    if(getType().equalsIgnoreCase("TVShow") && getStartSeason() + getEndSeason() >= 0) {
      Episode e = getSeason(title);
      if(e == null) e = getSeason(link);
      if(e == null) return false;

      if(getStartSeason() >= 0 && getEndSeason() > 0) {
        if(!e.inRange(getStartSeason(), getStartEpisode(), getEndSeason(), getEndEpisode())) return false;
      } else if(getStartSeason() >= 0) {
        if(!e.isFrom(getStartSeason(), getStartEpisode())) return false;
      } else {
        if(!e.isUpto(getEndSeason(), getEndEpisode())) return false;
      }
    }
    return true;
  }

  public static Episode getSeason(String str) {
    str = str.toLowerCase();
    Pattern lmp = Pattern.compile("(ht|f)tp:.*/(.*?)");
    Matcher lmm = lmp.matcher(str);
    if(lmm.matches()) str = lmm.group(2); // strip if url

    String showTitle = "";
    int seasonStart, seasonEnd, episodeStart, episodeEnd;
    Episode e = null;

    Matcher m = epsnnenn_snnenn.matcher(str);
    if(!m.matches()) m = epsnnenn_enn.matcher(str);
    if(!m.matches()) m = epsnnenn_nn.matcher(str);
    if(!m.matches()) m = epsnnenn.matcher(str);
    if(!m.matches()) m = epnnxnn_nnxnn.matcher(str);
    if(!m.matches()) m = epnnxnn_nn.matcher(str);
    if(!m.matches()) m = epnnxnn.matcher(str);
    if(!m.matches()) m = epnnnn_nnnn.matcher(str);
    if(!m.matches()) m = epnnnn_nn.matcher(str);
    if(!m.matches()) m = epnnnn.matcher(str);
    if(!m.matches()) return null;

    showTitle = stringClean(m.group(1));

    switch(m.groupCount()) {
      case 3:
        seasonStart = Integer.parseInt(m.group(2));
        episodeStart = Integer.parseInt(m.group(3));
        e = new Episode(showTitle, seasonStart, episodeStart);
        break;
      case 4:
        seasonStart = Integer.parseInt(m.group(2));
        episodeStart = Integer.parseInt(m.group(3));
        seasonEnd = Integer.parseInt(m.group(2));
        episodeEnd = Integer.parseInt(m.group(4));
        e = new Episode(showTitle, seasonStart, episodeStart, seasonEnd, episodeEnd);
        break;
      case 5:
        seasonStart = Integer.parseInt(m.group(2));
        episodeStart = Integer.parseInt(m.group(3));
        seasonEnd = Integer.parseInt(m.group(4));
        episodeEnd = Integer.parseInt(m.group(5));
        e = new Episode(showTitle, seasonStart, episodeStart, seasonEnd, episodeEnd);
        break;
    }

    return e;
  }

  private static String stringClean(String str) {
    str = str.replaceAll("[ \\._\\-]+", " ");
    str = str.replaceAll("\\[.*\\]", "");
    str = str.trim();
    if(!str.equals(str.toLowerCase())) return str;

    String[] strp = str.split("[^\\w\\d]+");
    for(int iLoop = 0; iLoop < strp.length; iLoop++) {
      if(strp[iLoop].length() == 0) continue;
      String c = String.valueOf(strp[iLoop].charAt(0));
      String nstrp = strp[iLoop].replaceFirst(c, c.toUpperCase());
      str = str.replaceAll(strp[iLoop], nstrp);
    }
    return str;
  }

  private boolean match(String matchee) {
    if(getIsRegex()){
      if ( exprPat == null ){
    	  return( false );	// invalid expression, always fail
      }
      Matcher m = exprPat.matcher(matchee.toLowerCase());
      return m.matches();
    } else {
      if(matchee.toLowerCase().indexOf(exprLower) >= 0) return true;
    }
    return false;
  }

  public boolean getUseSmartHistory() {
    if("TVShow".equalsIgnoreCase(type)) return smartHistory;
    else return true;
  }

  public void setUseSmartHistory(boolean smartHistory) {
    this.smartHistory = smartHistory;
  }
}
