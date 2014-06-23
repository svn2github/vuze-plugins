/*
 * File    : AzBlogPlugin.java
 * Created : 12-Nov-2004
 * By      : Thomas Winningham
 * 
 * AzBlog - A MetaBlogger API plugin for Azureus
 *
 */

package net.sourceforge.writtorrent.AzBlog;

import java.io.*;
import java.util.*;
import java.net.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.ui.menus.*;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.tables.*;

import org.apache.xmlrpc.*;

public class
AzBlogPluginClass
	implements Plugin
{


	public static final String	CONFIG_AZBLOG_PUBLISH				= "MetaWeblog Publish (disabled=draft)";
	public static final boolean	CONFIG_AZBLOG_PUBLISH_DEFAULT			= true;

	public static final String	CONFIG_AZBLOG_AUTOPUBLISH			= "Auto-Publish all torrents (careful!)";
	public static final boolean	CONFIG_AZBLOG_AUTOPUBLISH_DEFAULT		= false;

	public static final String	CONFIG_AZBLOG_BLOGID				= "MetaWeblog Blog ID";
	public static final String	CONFIG_AZBLOG_BLOGID_DEFAULT			= "myblog";

	public static final String	CONFIG_AZBLOG_USERNAME				= "MetaWeblog Username";
	public static final String	CONFIG_AZBLOG_USERNAME_DEFAULT			= "Username";

	public static final String	CONFIG_AZBLOG_PASSWORD				= "MetaWeblog Password";
	public static final String	CONFIG_AZBLOG_PASSWORD_DEFAULT			= "";

	public static final String	CONFIG_AZBLOG_SERVER				= "MetaWeblog Server";
	public static final String	CONFIG_AZBLOG_SERVER_DEFAULT			= "http://myblog.com:80/RPC2";



	protected PluginInterface		plugin_interface;
	protected PluginConfig			plugin_config;
	protected DownloadManager		download_manager;
	protected Utilities			utilities;
	protected Formatters			formatters;
	protected Tracker			tracker;

	protected TorrentAttribute		torrent_categories;
	protected String			file_root;

	protected BooleanParameter 		blog_publish;
	protected StringParameter 		blog_id;	
	protected StringParameter 		blog_username;
	protected PasswordParameter 		blog_password;
	protected StringParameter 		blog_url;
	protected BooleanParameter		blog_autopublish;

	protected int				torrentSize;
	protected byte[]			torrentBytes;


//####################################################################################################################

public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		plugin_config		= plugin_interface.getPluginconfig();
		download_manager	= plugin_interface.getDownloadManager();
		utilities		= plugin_interface.getUtilities();
		formatters 		= utilities.getFormatters();
		tracker 		= plugin_interface.getTracker();



		plugin_interface.getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( "net.sourceforge.writtorrent.AzBlog.internat.Messages");

		UIManager	ui_manager = plugin_interface.getUIManager();

		BasicPluginConfigModel config_model = ui_manager.createBasicPluginConfigModel( "plugins", "azblog.blog");

		blog_publish 		= config_model.addBooleanParameter2( CONFIG_AZBLOG_PUBLISH, "azblog.blog.publish", CONFIG_AZBLOG_PUBLISH_DEFAULT );
		blog_url			= config_model.addStringParameter2( CONFIG_AZBLOG_SERVER, "azblog.blog.server", CONFIG_AZBLOG_SERVER_DEFAULT );
		blog_id			= config_model.addStringParameter2( CONFIG_AZBLOG_BLOGID, "azblog.blog.blogid", CONFIG_AZBLOG_BLOGID_DEFAULT );
		blog_username		= config_model.addStringParameter2( CONFIG_AZBLOG_USERNAME, "azblog.blog.username", CONFIG_AZBLOG_USERNAME_DEFAULT );
		blog_password		= config_model.addPasswordParameter2( CONFIG_AZBLOG_PASSWORD, "azblog.blog.password", 1, null );
		blog_autopublish	= config_model.addBooleanParameter2( CONFIG_AZBLOG_AUTOPUBLISH, "azblog.blog.autopublish", CONFIG_AZBLOG_AUTOPUBLISH_DEFAULT );


// Seeding Torrents menu item

		TableContextMenuItem menuOne = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "azblog.blog.blogthistorrent" );
		menuOne.addListener(
			new MenuItemListener()
			{
				public void
				selected(
					MenuItem			_menu,
					Object	_target )
				{
				post(_menu, _target);
				}
			});

// Downloading Torrents menu item

		TableContextMenuItem menuTwo = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "azblog.blog.blogthistorrent" );
		menuTwo.addListener(
			new MenuItemListener()
			{
				public void
				selected(
					MenuItem			_menu,
					Object	_target )
				{
				post(_menu, _target);
				}
			});



// Publish Torrents automatically... listen for new torrents.

		DownloadManager manage = plugin_interface.getDownloadManager();
		manage.addListener(
			new DownloadManagerListener()
			{
				public void 
				downloadAdded(Download _download)
				{
					if (blog_autopublish.getValue())
					autopost(_download);
				}
				public void 
				downloadRemoved(Download _download)
				{
				}
			});


}

//####################################################################################################################

public void autopost( Download downloadObject){
				if (downloadObject == null)
				return;
				String enclosureurl=create_enclosure(downloadObject);
				blog_post(downloadObject, enclosureurl, String.valueOf(torrentSize));
}

//####################################################################################################################

public void post(
					MenuItem			_menu,
					Object	_target )
				{
				Download downloadObject = (Download)((TableRow)_target).getDataSource();
				if (downloadObject == null)
				return;
				String enclosureurl=create_enclosure(downloadObject);
				blog_post(downloadObject, enclosureurl, String.valueOf(torrentSize));
}
//####################################################################################################################

public String create_enclosure(Download downloadObject)
				{
				String enclosureurl = null;
				byte[] torrentoutput = null;
				int availableLength = 0;

					try {

						InputStream in = null;
						in = new FileInputStream(downloadObject.getTorrentFileName());
						availableLength = in.available();
						byte[] totalBytes = new byte[availableLength];
						int bytedata = in.read(totalBytes);
						torrentoutput = totalBytes;

					}catch( Throwable  e ){
						
						e.printStackTrace();
					}

				torrentSize  = availableLength;

					
					Hashtable ht = new Hashtable();
					ht.put("name", downloadObject.getName() + ".torrent");
					ht.put("type", "application/x-bittorrent");
					ht.put("bits", torrentoutput);

					Vector v = new Vector();
					v.addElement(blog_id.getValue());
					v.addElement(blog_username.getValue());
					v.addElement(blog_password.getValue());
					v.addElement(ht);

				
					Hashtable returned = new Hashtable();
					
					try{
					XmlRpcClient rpc = new XmlRpcClient(blog_url.getValue());
					returned = (Hashtable)rpc.execute("metaWeblog.newMediaObject", v);
					enclosureurl = (String)returned.get("url");

					}catch( Throwable  e ){
						e.printStackTrace();
					}

					return enclosureurl;

}

//####################################################################################################################

public void blog_post(Download downloadObject, String enclosureurl, String enclosureLength){
	
					Hashtable ht = new Hashtable();
					ht.put("title",downloadObject.getName() + "");
					ht.put("description",downloadObject.getTorrent().getComment() + "");
						Hashtable enclose = new Hashtable();
						enclose.put("url",enclosureurl +"");
						enclose.put("length", enclosureLength +"");
						enclose.put("type", "application/x-bittorrent");
					ht.put("enclosure",enclose);
					ht.put("link", enclosureurl + "");

					Vector v = new Vector();
					v.addElement(blog_id.getValue());
					v.addElement(blog_username.getValue());
					v.addElement(blog_password.getValue());
					v.addElement(ht);
					v.addElement(new Boolean(blog_publish.getValue()));
					
					try{
					XmlRpcClient rpc = new XmlRpcClient(blog_url.getValue());
					String returned = (String)rpc.execute("metaWeblog.newPost", v);
					}catch( Throwable  e ){
						e.printStackTrace();
					}

					

}
//####################################################################################################################

}