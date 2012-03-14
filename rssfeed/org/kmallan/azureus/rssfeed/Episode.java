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

public class Episode {

  public int seasonStart, seasonEnd, episodeStart, episodeEnd;
  public String showTitle = "";

  public Episode(int season, int episode) {
    setEpisode(season, episode, season, episode);
  }

  public Episode(String title, int season, int episode) {
    showTitle = title;
    setEpisode(season, episode, season, episode);
  }

  public Episode(int sStart, int eStart, int sEnd, int eEnd) {
    setEpisode(sStart, eStart, sEnd, eEnd);
  }

  public Episode(String title, int sStart, int eStart, int sEnd, int eEnd) {
    showTitle = title;
    setEpisode(sStart, eStart, sEnd, eEnd);
  }

  private void setEpisode(int sStart, int eStart, int sEnd, int eEnd) {
    seasonStart = sStart;
    episodeStart = eStart;
    seasonEnd = sEnd;
    episodeEnd = eEnd;
  }

  public boolean isFrom(int season, int episode) {
    if(seasonStart > season) return true;
    if((seasonStart == season) && (episodeStart >= episode)) return true;
    return false;
  }

  public boolean isUpto(int season, int episode) {
    if(seasonEnd < season) return true;
    if((seasonEnd == season) && (episodeEnd <= episode)) return true;
    return false;
  }

  public boolean inRange(int sStart, int eStart, int sEnd, int eEnd) {
    if((isFrom(sStart, eStart)) && (isUpto(sEnd, eEnd))) return true;
    return false;
  }

  public String toString() {
    return showTitle + " s" + seasonStart + "e" + episodeStart + " - s" + seasonEnd + "e" + episodeEnd;
  }
}

