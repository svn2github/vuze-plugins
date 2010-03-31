/*
 * Created on 28 févr. 2005
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.plugins.chat.ui;

import java.util.Calendar;
import java.util.GregorianCalendar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.plugins.download.Download;
import com.aelitis.azureus.plugins.chat.ChatPlugin;

public class ChannelPanel {
  
  ChatPlugin  plugin;
  ChatPanel   panel;
  Download    download;
  
  Display     display;
  Composite   composite;
  StyledText  messages;
  Text        input;
  Button      send;
  
  long        lastTimeSent = 0;

  
	private static Color timeColor;
	private static Color nickColor;
	private static Color textColor;
	private static Color emoteColor;
	private static Color whiteColor;
  
  public ChannelPanel(ChatPlugin plugin,ChatPanel panel,Download download,Composite parent) {
    this.plugin   = plugin;
    this.panel = panel;
    this.download = download;
    
    display = parent.getDisplay();

    if (timeColor == null) {
    	// no need to destroy.. keep the colors through lifetime of AZ
	    timeColor = new Color(display,50,50,128);
	    nickColor = new Color(display,50,50,128);
	    textColor = new Color(display,0,0,0);
	    emoteColor = new Color(display,128,50,50);
	    whiteColor= new Color(display,255,255,255);
    }
    
    
    composite = new Composite(parent,SWT.NONE);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    composite.setLayout(layout);
    
    GridData data;
    
    messages = new StyledText(composite,SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
    //Not very nice lookigin but prevents the scroll bug from appearing
    messages.setWordWrap(false);
    data = new GridData(GridData.FILL_BOTH);
    data.horizontalSpan = 2;
    messages.setLayoutData(data);
    
    input = new Text(composite,SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    input.setLayoutData(data);
    
    send = new Button(composite,SWT.PUSH);
    send.setText("Send");
    data = new GridData();
    data.widthHint = 80;
    send.setLayoutData(data);
    
    send.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event event) {
        String text = input.getText();
        if(text != null && text.length() > 0) {
          interpretCommand(text);
        }        
      }
    });
    
    input.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent event) {
        if (event.keyCode == 13) {
          String text = input.getText();
          if(text != null && text.length() > 0) {
            interpretCommand(text);
          }
          
        }
      }
    });
  }
  
  public Control getControl() {
    return composite;
  }
  
  
  //Assumes we're called from the UI Thread
  public void messageReceived(final String nick,final String message) {
  	if (composite.isDisposed())
  		return;
  	
    //messages.append(nick + " > " + message + "\n");
    ScrollBar sb = messages.getVerticalBar();

    //System.out.println(sb.getSelection()+ "/" + (sb.getMaximum() - sb.getThumb()));
    boolean autoScroll = sb.getSelection() == (sb.getMaximum() - sb.getThumb());
    int nbLines = messages.getLineCount();
    if (nbLines > 4096 + 256)
      messages.replaceTextRange(0, messages.getOffsetAtLine(256), ""); //$NON-NLS-1$
    Calendar now = GregorianCalendar.getInstance();        
    String timeStamp =
      "[".concat(String.valueOf(now.get(Calendar.HOUR_OF_DAY))).concat(":").concat(format(now.get(Calendar.MINUTE))).concat(":").concat(format(now.get(Calendar.SECOND))).concat("]  "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$        
    
    int start = messages.getCharCount();
    int length = timeStamp.length();
    StyleRange range = new StyleRange(start,length,timeColor,whiteColor);
    messages.append(timeStamp + " ");
    messages.setStyleRange(range);    
    
    String outputNick = nick;
    String outputMessage = message;
    Color  outputColor;
    Color  nickColorToUse;
    start = messages.getCharCount();
    length = nick.length();
    if(! message.startsWith("/me ")) {
      length +=2;
      outputNick = "<" + nick + ">";
      outputColor = textColor;
      nickColorToUse = nickColor;
    } else {
      outputMessage = message.substring(3);
      outputColor = emoteColor;
      nickColorToUse = outputColor;
    }
    range = new StyleRange(start,length,nickColorToUse,whiteColor);
    messages.append(outputNick);
    messages.setStyleRange(range);    
    
    start = messages.getCharCount();
    length = outputMessage.length();
    range = new StyleRange(start,length,outputColor,whiteColor);        
    
    messages.append(outputMessage + "\n"); //$NON-NLS-1$
    messages.setStyleRange(range); 

    
    
    if (autoScroll)  messages.setSelection(messages.getText().length());
  }
  
  private String format(int n) {
    if(n < 10) return "0".concat(String.valueOf(n)); //$NON-NLS-1$
    return String.valueOf(n); //$NON-NLS-1$
  }
  
  
  private void interpretCommand(String text) {    
    if(text.startsWith("/")) {
      int spaceIndex = text.indexOf(" ");
      if(spaceIndex == -1) spaceIndex = text.length();
      
      if(spaceIndex > 1) {
        String cmd = text.substring(1,spaceIndex).trim().toLowerCase();
        String param = text.substring(spaceIndex).trim();
        
        if(cmd.equals("nick") || cmd.equals("name") || cmd.equals("n")) {
          input.setText("");
          if(param.length() > 0) {
            //ChannelPanel.this.plugin.sendMessage(ChannelPanel.this.download,"/me is now known as " + param);
            messageReceived("System", "/me : nick set to " + param); 
            ChannelPanel.this.plugin.setNick(param);            
          } else {
            messageReceived("System", "/me : usage : /nick newNick");
          }
        }
        
        else if(cmd.equals("h") || cmd.equals("help")) {
          input.setText("");
          printHelp();
        }
        
        else if(cmd.equals("ignore")) {
          input.setText("");
          if(param.length() > 0) {
            messageReceived("System", "/me : now ignoring " + param); 
            ChannelPanel.this.plugin.addIgnore(param);
          } else {
            messageReceived("System", "/me : usage : /ignore nick");
          }          
        }
        
        else if(cmd.equals("me")) {
          sendMessage(text);
        }
        
        else {
          messageReceived("System", "/me : invalid command : " + cmd);
          input.setText("");
        }
      }
    } else sendMessage(text);
  }
  
  private void sendMessage(String text) {
    long currentTime = plugin.getPluginInterface().getUtilities().getCurrentSystemTime();
    //At least five secs between messages
    if(currentTime > lastTimeSent + 5000) {
      lastTimeSent = currentTime;
      ChannelPanel.this.plugin.sendMessage(ChannelPanel.this.download,text);
      input.setText("");  
    } else {
      messageReceived("System", "/me : Flood Control, please wait before sending another message.");
    }
    
  }
  
  private void printHelp() {
    messageReceived("System", "/me : Allowed commands are :");
    messageReceived("   ", "/me /help , /h : Shows this message.");
    messageReceived("   ", "/me /nick , /name , /n : changes your nick");
    messageReceived("   ", "/me /ignore : ignores a peer by name (case sensitive)");
    messageReceived("   ", "/me /me : sends an emote");
  }
}
