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

import java.io.Serializable;
import java.util.regex.*;

public class HistoryBean implements Serializable, Comparable {

  static final long serialVersionUID = -4112775156287555070L;

  private String fileData, location, filtType, filtName;
  private long histId, filtId;
  private String showTitle;
  private int seasonStart, seasonEnd, episodeStart, episodeEnd;
  private boolean proper;

  public HistoryBean() {
    histId = System.currentTimeMillis();
  }

  public long getID() {
    return histId;
  }

  public void setID(long histId) {
    this.histId = histId;
  }

  public long getFiltID() {
    return filtId;
  }

  public void setFiltID(long filtId) {
    this.filtId = filtId;
  }

  public String getFileData() {
    if(fileData == null) fileData = "";
    return fileData;
  }

  public void setFileData(String fileData) {
    this.fileData = fileData;
  }

  public String getLocation() {
    if(location == null) location = "";
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public boolean setSeason(String str) {
    Pattern lmp = Pattern.compile("(ht|f)tp:.*/(.*?)");
    Matcher lmm = lmp.matcher(str);
    if (lmm.matches()) {
      str = lmm.group(2);
    }

    Matcher m = FilterBean.epsnnenn_snnenn.matcher(str);
    if(!m.matches()) m = FilterBean.epsnnenn_enn.matcher(str);
    if(!m.matches()) m = FilterBean.epsnnenn_nn.matcher(str);
    if(!m.matches()) m = FilterBean.epsnnenn.matcher(str);
    if(!m.matches()) m = FilterBean.epnnxnn_nnxnn.matcher(str);
    if(!m.matches()) m = FilterBean.epnnxnn_nn.matcher(str);
    if(!m.matches()) m = FilterBean.epnnxnn.matcher(str);
    if(!m.matches()) m = FilterBean.epnnnn_nnnn.matcher(str);
    if(!m.matches()) m = FilterBean.epnnnn_nn.matcher(str);
    if(!m.matches()) m = FilterBean.epnnnn.matcher(str);
    if(!m.matches()) return false;


    setShowTitle(FilterBean.stringClean(m.group(1)));
    final int end = m.end(m.groupCount());
    setProper(FilterBean.properPattern.matcher(str.substring(end)).find());

    switch (m.groupCount()) {
      case 3:
        setSeasonStart(Integer.parseInt(m.group(2)));
        setEpisodeStart(Integer.parseInt(m.group(3)));
        setSeasonEnd(Integer.parseInt(m.group(2)));
        setEpisodeEnd(Integer.parseInt(m.group(3)));
        break;
      case 4:
        setSeasonStart(Integer.parseInt(m.group(2)));
        setEpisodeStart(Integer.parseInt(m.group(3)));
        setSeasonEnd(Integer.parseInt(m.group(2)));
        setEpisodeEnd(Integer.parseInt(m.group(4)));
        break;
      case 5:
        setSeasonStart(Integer.parseInt(m.group(2)));
        setEpisodeStart(Integer.parseInt(m.group(3)));
        setSeasonEnd(Integer.parseInt(m.group(4)));
        setEpisodeEnd(Integer.parseInt(m.group(5)));
        break;
      default:
        return false;
    }
    return true;
  }

  public String getShowTitle() {
    return showTitle;
  }

  public void setShowTitle(String showTitle) {
    this.showTitle = showTitle;
  }

  public boolean isProper() {
    return proper;
  }

  public void setProper(boolean proper) {
    this.proper = proper;
  }

  public int getSeasonStart() {
    return seasonStart;
  }

  public void setSeasonStart(int seasonStart) {
    this.seasonStart = seasonStart;
  }

  public int getSeasonEnd() {
    return seasonEnd;
  }

  public void setSeasonEnd(int seasonEnd) {
    this.seasonEnd = seasonEnd;
  }

  public int getEpisodeStart() {
    return episodeStart;
  }

  public void setEpisodeStart(int episodeStart) {
    this.episodeStart = episodeStart;
  }

  public int getEpisodeEnd() {
    return episodeEnd;
  }

  public void setEpisodeEnd(int episodeEnd) {
    this.episodeEnd = episodeEnd;
  }

  public String toString() {
    return location;
  }

  public int compareTo(Object o) {
    return -(new Long(histId).compareTo(new Long(((HistoryBean)o).histId)));
  }

  public void setFilter(FilterBean filter) {
    if(filter != null) {
      this.filtId = filter.getID();
      this.filtName = filter.getName();
      this.filtType = filter.getType();
    }
  }

  public String getFiltName() {
    return filtName;
  }

  public String getFiltType() {
    return filtType;
  }
}
