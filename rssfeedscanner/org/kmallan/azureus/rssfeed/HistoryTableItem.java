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

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;

import java.text.DateFormat;
import java.util.Date;

public class HistoryTableItem extends TableItem {

  private Table parent;
  private Config config;
  private HistoryBean data;

  public HistoryTableItem(Table parent, Config config, int index) {
    super(parent, SWT.NULL, index);
    this.parent = parent;
    this.config = config;
  }

  public void checkSubclass() {
    return;
  }

  public HistoryBean getBean() {
    return data;
  }

  public void setBean(int index) {
    this.data = config.getHistory(index);

    update();
  }

  /*
  public void setBean(HistoryBean data) {
    if(this.data == null) config.addHistory(data);
    else config.setHistory(config.getHistoryIndex(this.data), data);
    this.data = data;

    update();
  }
  */

  public void update() {
    setText(0, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(data.getID())));
    setText(1, data.getFileData());
    setText(2, data.getLocation());

    String output = "";
    if(data.getFiltID() != 0) {
      output = "Filter Matched";
      if(data.getFiltName() != null) output += ": '" + data.getFiltName() + "'";
      if(data.getSeasonStart() >= 0) {
        output += " Type: TVShow - Ep " + Integer.toString(data.getSeasonStart()) + "x" + Integer.toString(data.getEpisodeStart());
        if(data.getSeasonEnd() > data.getSeasonStart())
          output += "-" + Integer.toString(data.getSeasonEnd()) + "x" + Integer.toString(data.getEpisodeEnd());
        else if(data.getEpisodeEnd() > data.getEpisodeStart())
          output += "-" + Integer.toString(data.getEpisodeEnd());
      }
    } else {
      output = "Manual Download";
    }
    setText(3, output);
  }

  public void remove() {
    config.removeHistory(data);
    parent.remove(parent.indexOf(this));
  }


}
