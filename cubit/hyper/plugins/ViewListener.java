/******************************************************************************
Cubit distribution
Copyright (C) 2008 Bernard Wong

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

The copyright owner can be contacted by e-mail at bwong@cs.cornell.edu
*******************************************************************************/

package hyper.plugins;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
// import java.text.Collator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
//import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
// import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ws.commons.util.Base64;
import org.cornell.hyper.overlay.Consumer;
import org.cornell.hyper.overlay.MovieEntry;
import org.cornell.hyper.overlay.Producer;
// import org.cornell.hyper.overlay.XmlRPCClient;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
// import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
// import org.eclipse.swt.widgets.Listener;
// import org.eclipse.swt.widgets.Event;
// import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.utils.search.SearchException;
import org.gudy.azureus2.plugins.utils.search.SearchInstance;
import org.gudy.azureus2.plugins.utils.search.SearchObserver;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.plugins.utils.search.SearchResult;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

public class ViewListener implements UISWTViewEventListener, SearchProvider {
	
	/**
	 * Background search thread
	 */
	protected class AzureusSearchInstance implements 
			Runnable, Consumer<MovieEntry>, SearchInstance {
		
		protected class searchResult implements SearchResult {			
			private String movieName;
			private String magLink;
			private String torName;
			
			protected searchResult(
					String movieName, String magLink, String torName) {
				this.movieName = movieName;
				this.magLink = magLink;
				this.torName = torName;
			}

			public Object getProperty(int property_name) {
				System.out.println(property_name);
				if (property_name == SearchResult.PR_NAME) {
					System.out.println("Returning " + torName);
					return torName;							
				}				
				if (property_name == SearchResult.PR_DOWNLOAD_LINK) {
					/*
					String curMagLink = magLink.replaceFirst("http://", "");
					System.out.println("Returning link " + curMagLink);
					return curMagLink;
					*/
					System.out.println("Returning link " + magLink);
					return magLink;
				}	
				
				if ( property_name == SearchResult.PR_ACCURACY ){
					
					return( new Long( new Random().nextInt(100)));
				}
				return null;
			}
		}
		
		private boolean				done = false;
		private List<String>		keywords;
		private List<MovieEntry>	movieList;
		private final int			refreshRateMS = 1000;
		private Set<MovieEntry>		allEntries;		
		private SearchObserver		observer;
		public static final int		normalResultsSize = 32;
			
		/**
		 * Search thread constructor
		 * 
		 * @param searchText	Text string for the search
		 * @param numMovies		Maximum number of results
		 * @param cb			Callback with search results
		 */
		public AzureusSearchInstance(
				String searchText, int numMovies, SearchObserver observer) {
			this.observer 	= observer;
			movieList 		= new ArrayList<MovieEntry>();
			allEntries		= new HashSet<MovieEntry>();
			// Issue the search, with this object as the callback
			
			System.out.println( "Cubit: max_res=" +  observer.getProperty( SearchObserver.PR_MAX_RESULTS_WANTED ));
			
			keywords = parentPlugin.issueSearch(searchText, numMovies, this);
			// If there are no keywords from this search text, it's done
			if (keywords == null) { 
				done = true; 
			}		
			new Thread(this).start();
		}
		
		/**
		 * Callback from producer. Allows the producer to check if the consumer is 
		 * still running.
		 */
		public synchronized boolean checkConsumer(Producer<MovieEntry> inProduce) {
			return !done;
		}

		/**
		 * Callback from producer. Returns new MovieEntry to the consumer.
		 */
		public synchronized boolean newProducerResult(
				Producer<MovieEntry> inProduce, MovieEntry result) {
			if (done) { return false; }
			// Remove duplicates
			if (!allEntries.contains(result)) {
				movieList.add(result);
				allEntries.add(result);
			}
			//notify();
			return true;
		}		

		/**
		 * Callback from producer. Indicates that the producer has completed.
		 */
		public synchronized void producerComplete(
				Producer<MovieEntry> inProduce, boolean success) {				
			done = true;
			notify();			
		}

		public void run() 		{ blockAsync(); }
		
		public void cancel() 	{ stopSearch(); }
			
		/**
		 * Stop the SearchConsumer thread.
		 */
		public synchronized void stopSearch() {
			done = true;
			notify();
		}
		
		/**
		 * Adds a single movie to the results table. Currently only called
		 * by addAllMovies.
		 * 
		 * @param curMovie
		 */
		private synchronized void addMovie(final MovieEntry curMovie) {
			final String movieName = curMovie.getMovName();
			final String magLink = curMovie.getMagLink();
			final String torName = curMovie.getTorName();
			int movDist = parentPlugin.getEditDist(movieName, keywords);			
			observer.resultReceived(this, 
					new searchResult(movieName, magLink, torName)); 
		}
		
		/**
		 * Add all movies currently available to the table. Currently only called 
		 * by blockAsync.
		 */
		private synchronized void addAllMovies() {
			final List<MovieEntry> localEntries = new ArrayList<MovieEntry>(movieList);
			movieList.clear();
			Iterator<MovieEntry> it = localEntries.iterator();
			while (it.hasNext()) {
				addMovie(it.next());
			}
		}
				
		/**
		 * Main event loop. Add movies to the table at the refreshRateMS
		 * intervals. 
		 */
		private synchronized void blockAsync() {
			addAllMovies();
			while (!done) {			
				try {
					// Poll based on refresh rate. The reason why
					// polling is necessary is to provide some 
					// degree of aggregation of results and limit
					// the number of screen updates.
					wait(refreshRateMS);	
				} catch (InterruptedException e) {
					break; // If interrupted, just break out of the loop
				}
				addAllMovies();
			}
			// Done, turn the search buttons back on and tell 
			// SearchCallback that the search is complete.
			//signalComplete();
			observer.complete();
		}
	}
		
	/**
	 * Updates the table after the search thread completes.
	 * 
	 * Invariant: Only the display thread should be calling any of 
	 * the methods in an instantiation of this class. Any thread that 
	 * wants to call a method here should wrap it inside a 
	 * display.asyncExec(...).
	 * 
	 * TODO: Double check that creating an item at an index does not overwrite
	 * TODO: Add edit distance filter to the options tab
	 * TODO: Add ratings column
	 */
	protected class SearchCallback 
		extends SelectionAdapter implements KeyListener {
		
		/**
		 * Background search thread
		 */
		protected class SearchConsumer implements Runnable, Consumer<MovieEntry> {
			private boolean				done = false;
			private List<String>		keywords;
			private List<MovieEntry>	movieList;
			private final int			refreshRateMS = 1000;
			private Set<MovieEntry>		allEntries;
			
			// Increment size for the progress bar
			private final int inc = (int) Math.round(searchPBar.getMaximum() / 
						((double) parentPlugin.defaultSearchTimeout / refreshRateMS));
			
			/**
			 * Search thread constructor
			 * 
			 * @param searchText	Text string for the search
			 * @param numMovies		Maximum number of results
			 * @param cb			Callback with search results
			 */
			public SearchConsumer(
					String searchText, int numMovies, SearchCallback cb) {
				movieList 	= new ArrayList<MovieEntry>();
				allEntries	= new HashSet<MovieEntry>();
				// Issue the search, with this object as the callback
				keywords = parentPlugin.issueSearch(searchText, numMovies, this);
				// If there are no keywords from this search text, it's done
				if (keywords == null) { 
					done = true; 
				}		
				new Thread(this).start();
			}
			
			/**
			 * Callback from producer. Allows the producer to check if the consumer is 
			 * still running.
			 */
			public synchronized boolean checkConsumer(Producer<MovieEntry> inProduce) {
				return !done;
			}

			/**
			 * Callback from producer. Returns new MovieEntry to the consumer.
			 */
			public synchronized boolean newProducerResult(
					Producer<MovieEntry> inProduce, MovieEntry result) {
				if (done) { return false; }
				// Remove duplicates
				if (!allEntries.contains(result)) {
					movieList.add(result);
					allEntries.add(result);
				}
				//notify();
				return true;
			}		

			/**
			 * Callback from producer. Indicates that the producer has completed.
			 */
			public synchronized void producerComplete(
					Producer<MovieEntry> inProduce, boolean success) {				
				done = true;
				notify();			
			}

			public void run() { blockAsync(); }
				
			/**
			 * Stop the SearchConsumer thread.
			 */
			public synchronized void stopSearch() {
				done = true;
				notify();
			}
			
			/**
			 * Adds a single movie to the results table. Currently only called
			 * by addAllMovies.
			 * 
			 * @param curMovie
			 */
			private synchronized void addMovie(final MovieEntry curMovie) {
				String movieName = curMovie.getMovName();
				String magLink = curMovie.getMagLink();
				String torName = curMovie.getTorName();
				int movDist = parentPlugin.getEditDist(movieName, keywords);
				if (!resultsTable.isDisposed()) {
					TableItem[] curItems = resultsTable.getItems();
					boolean objAdded = false;
					String[] newRow = new String[]{
							torName, String.valueOf(movDist), magLink, movieName};
					for (int i = 0; i < curItems.length; i++) {						
						int curDist = Integer.parseInt(curItems[i].getText(1));							
						if (movDist < curDist) {
							TableItem item = new TableItem(resultsTable, 0, i);					
							item.setText(newRow);
							objAdded = true;
							break;
						}				
					}
					if (!objAdded) {
						TableItem item = new TableItem(resultsTable, 0);					
						item.setText(newRow);						
					}
					resultsTable.setTopIndex(0);
				}
			}
			
			/**
			 * Add all movies currently available to the table. Currently only called 
			 * by blockAsync.
			 */
			private synchronized void addAllMovies() {
				final List<MovieEntry> localEntries = new ArrayList<MovieEntry>(movieList);
				movieList.clear();
				display.asyncExec(new Runnable() {						
					public void run() { 
						searchPBar.setSelection(searchPBar.getSelection() + inc);
						Iterator<MovieEntry> it = localEntries.iterator();
						while (it.hasNext()) {
							addMovie(it.next());
						}
					}
				});
			}					
			
			/** 
			 * Turn the search button and other GUI components back to the
			 * non-searching state, and indicate to the parent object that
			 * the search is complete. Currently only called by blockAsync.
			 */
			private synchronized void signalComplete() {
				display.asyncExec(new Runnable() {						
					public void run() {						
						logChannel.log("Search complete");				
						// Make sure the GUI objects are still there.
						if (!searchButton.isDisposed()) { 
							searchButton.setEnabled(true);
							searchButton.setText("Search");
						}
						if (!searchText.isDisposed()) { 
							searchText.setEnabled(true);
						}
						if (!searchPBar.isDisposed()) {
							searchPBar.setVisible(false);
							searchPBar.setSelection(searchPBar.getMinimum());
						}
						searchComplete();
					}
				});					
			}
			
			/**
			 * Main event loop. Add movies to the table at the refreshRateMS
			 * intervals. 
			 */
			private synchronized void blockAsync() {
				addAllMovies();
				while (!done) {			
					try {
						// Poll based on refresh rate. The reason why
						// polling is necessary is to provide some 
						// degree of aggregation of results and limit
						// the number of screen updates.
						wait(refreshRateMS);	
					} catch (InterruptedException e) {
						break; // If interrupted, just break out of the loop
					}
					addAllMovies();
				}
				// Done, turn the search buttons back on and tell 
				// SearchCallback that the search is complete.
				signalComplete();
			}
		}
		
		private final Button 		searchButton;
		private final Text			searchText;
		private final ProgressBar	searchPBar;
		private final Table			resultsTable;
		private final Display		display;
		private final int			normalResultsSize = 32;
		private SearchConsumer 		curSearchConsumer = null;
		
		/**
		 * SearchCallback constructor.
		 * 
		 * @param searchButton		The button object for the search button
		 * @param searchText		Text string for the search
		 * @param ProgressBar		Progress bar for search 
		 * @param resultsTable		The table containing the search results
		 */
		public SearchCallback(
				final Button searchButton, 
				final Text searchText,
				final ProgressBar searchPBar,
				final Table resultsTable) {
			this.searchButton 		= searchButton;    // Search Button
			this.searchText			= searchText;	   // Text box for search string
			this.searchPBar			= searchPBar;
			this.resultsTable 		= resultsTable;	   // Table of results			
			display = parentPlugin.getDisplay();
		}
		
		/**
		 * Issue search if a button is clicked. Used when SearchCallback is a 
		 * event listener for a Button object. 
		 */
		public void widgetSelected(SelectionEvent e) {
			if (curSearchConsumer == null) {
				issueSearch();
			} else {
				curSearchConsumer.stopSearch();
			}
		}
		
		/**
		 * Issue search if enter is pressed. Used when SearchCallback is a 
		 * event listener for a Text object. 
		 */
		public void keyPressed(KeyEvent event) {
			if (event.keyCode == SWT.CR || event.keyCode == SWT.KEYPAD_CR) {
				if (curSearchConsumer == null) {
					issueSearch();
				} else {
					curSearchConsumer.stopSearch();
				}
			}
		}
		
		/**
		 * keyReleased not used.
		 */
		public void keyReleased(KeyEvent event) {}
		
		// Make sure this is only called by the display thread.
		private void searchComplete() {
			if (curSearchConsumer != null) {
				curSearchConsumer = null;
			} 
		}
				
		private void issueSearch() {
			if (display != null && curSearchConsumer == null) {
				searchButton.setText("Cancel");
				searchText.setEnabled(false);		// Gray out the text box for search
				resultsTable.removeAll();			// Remove old values
				searchPBar.setVisible(true);
				
				// Fetch the search string from the text box
				String searchStr = searchText.getText();
				
				// Write out the search string to the log
				logChannel.log("Searching for: " + searchStr);
				
				// Create a SearchConsumer
				curSearchConsumer = new 
					SearchConsumer(searchStr, normalResultsSize, this);
			}
		}		
	}
		
	private UISWTView view = null;
	private HyperPlugin parentPlugin;
	private LoggerChannel logChannel;
	private Table resultsTable;
	private Clipboard clipboard;
	private TabItem browserTab;
	private TabFolder tabFolder;
	private Browser browser;
	
	public ViewListener(HyperPlugin parentPlugin) {
		this.parentPlugin = parentPlugin;
		logChannel = this.parentPlugin.getLogger();
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				if (view != null) {
					return false;
				}
				view = event.getView();
				break;
	
			case UISWTViewEvent.TYPE_INITIALIZE:
				initialize((Composite) event.getData());
				break;
	
			case UISWTViewEvent.TYPE_REFRESH:
				refresh();
				break;
	
			case UISWTViewEvent.TYPE_DESTROY:
				view = null;
				break;
	
			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				break;
	
			case UISWTViewEvent.TYPE_FOCUSGAINED:
				break;
	
			case UISWTViewEvent.TYPE_FOCUSLOST:
				break;
	
			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				break;
		}
		return true;
	}
	
	/*
	protected Menu createPopupMenu(Shell shell) {
	    Menu m = new Menu(shell, SWT.POP_UP);
	    shell.setMenu(m);
	    return m;
	}
	protected Menu createPopupMenu(Shell shell, Control owner) {
	    Menu m = createPopupMenu(shell);
	    owner.setMenu(m);
	    return m;
	}
	*/	
	
	protected void registerCallback(final MenuItem mi, 
            final Object handler, 
            final String handlerName) {
		mi.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					System.err.println("Issueing callback");
					Method m = handler.getClass().getMethod(handlerName, null);
					m.invoke(handler, null);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
	}
	
	protected MenuItem createMenuItem(Menu parent, int style, String text, 
            Image icon, int accel, boolean enabled, 
            String callback) {
		MenuItem mi = new MenuItem(parent, style);
		if (text != null) {
			mi.setText(text);
		}
		if (icon != null) {
			mi.setImage(icon);
		}
		if (accel != -1) {
			mi.setAccelerator(accel);
		}
		mi.setEnabled(enabled);
		if (callback != null) {
			registerCallback(mi, this, callback);
		}
		return mi;
	}	
	
	public void fetchTorrent() {
		if(resultsTable == null || resultsTable.isDisposed()) {
			return;
		}		
		//Get the selection
		TableItem[] items = resultsTable.getSelection();

		//If an item is selected (double click selects at least one)
		if(items.length == 1) {
			//Grab selected item
			TableItem item = items[0];
			String url = item.getText(2);
			System.out.println("URL is: " + url);
			DownloadManager dm = parentPlugin.getPluginInterface().getDownloadManager();
			try {
				dm.addDownload(new URL(url));
			} catch (MalformedURLException e) {
				logChannel.log("Invalid URL: " + url);
			} catch (DownloadException e) {
				logChannel.log("Cannot open download manager");
			}
		}		
	}
	
	public void copyToClipboard() {		
		if(resultsTable == null || resultsTable.isDisposed()) {
			return;
		}		
		//Get the selection
		TableItem[] items = resultsTable.getSelection();

		//If an item is selected (double click selects at least one)
		if(items.length == 1) {		
			//Grab selected item		
			TableItem item = items[0];
			String url = item.getText(2);
			if (url.length() > 0) {
				//status.setText("");
				clipboard.setContents(new Object[] { url },
						new Transfer[] { TextTransfer.getInstance() });
			} 			
			//else {
			//	status.setText("nothing to copy");
			//}
		}	     
	}
	
	private void createSearchTab(final TabFolder tabFolder, TabItem searchTab) {
    	// Main canvas for the search part
    	Composite searchComp = new Composite(tabFolder, SWT.NONE);
    	searchComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL)); {    	
	        GridLayout gridLayout = new GridLayout();
	        //gridLayout.numColumns = 5;
	        gridLayout.numColumns = 4;
	    	searchComp.setLayout(gridLayout);
    	}
    	// Add it to the search tab
    	searchTab.setControl(searchComp);
		
        // Search label
    	Label searchLabel = new Label(searchComp, SWT.LEFT);
    	searchLabel.setText("Search String:");    	
        searchLabel.setLayoutData(new GridData());    	
    	
        // Search text field
    	final Text searchText = new Text(searchComp, SWT.BORDER | SWT.LEFT);{
        	GridData data = new GridData();
        	data.horizontalSpan = 1;
        	data.widthHint = 200;
        	searchText.setLayoutData(data);
        }

    	/*
        // Number of results label
    	Label numResultsLabel = new Label(searchComp, SWT.LEFT);
    	numResultsLabel.setText("Number of Results:");
    	numResultsLabel.setLayoutData(new GridData());    	    	    
    	
    	// Number of results combo-box
    	final Combo numResultsCombo = new Combo(
    			searchComp, SWT.DROP_DOWN | SWT.READ_ONLY); {    		
    		numResultsCombo.add("Normal");
    		numResultsCombo.add("Large");
    		numResultsCombo.select(0);
    		GridData data = new GridData();
    		data.horizontalSpan = 1;
    		numResultsCombo.setLayoutData(data);    		
    	}
    	*/
	         	
        // Search buttons
    	Button searchButton = new Button(searchComp, SWT.PUSH);
        searchButton.setText("Search");
        
        ProgressBar searchProgress = 
        	new ProgressBar(searchComp, SWT.HORIZONTAL | SWT.SMOOTH);
        searchProgress.setMinimum(0);
        searchProgress.setMaximum(100);        
        searchProgress.setVisible(false);
        
        // Separate the search area from the results area
        final Label sepLabel = new Label(
        		searchComp, SWT.SEPARATOR | SWT.HORIZONTAL); {
	    	GridData data = new GridData(GridData.FILL_HORIZONTAL);
	    	data.horizontalSpan = 5;
	    	sepLabel.setLayoutData(data);
        }        	
        	                
    	// Create the table
    	resultsTable = new Table(searchComp, SWT.SINGLE); {    	    
            final TableColumn fileCol = 
            	new TableColumn(resultsTable, SWT.LEFT);
            fileCol.setText("Filename");
            fileCol.setWidth(200);
            
            final TableColumn distCol = 
            	new TableColumn(resultsTable, SWT.CENTER);
            distCol.setText("Dist.");
            distCol.setWidth(50);
            
            final TableColumn magnetCol =
            	new TableColumn(resultsTable, SWT.CENTER);
            magnetCol.setText("URL");
            magnetCol.setWidth(0);
            magnetCol.setResizable(false);
            
            final TableColumn commentCol = 
            	new TableColumn(resultsTable, SWT.LEFT);
            commentCol.setText("Comment");
            commentCol.setWidth(200);                
            
        	GridData data = new GridData(GridData.FILL_BOTH);
        	//data.horizontalSpan = 5;
        	data.horizontalSpan = 4;
        	resultsTable.setLayoutData(data);
    	}
    	
   	
	    Menu rightClickMenu = new Menu(resultsTable.getShell(), SWT.POP_UP);
	    //MenuItem curItem = 
	    createMenuItem(rightClickMenu, SWT.PUSH, 
	    		"&Download Torrent", null, -1, true, "fetchTorrent");	    	    
	    new MenuItem(rightClickMenu, SWT.SEPARATOR);
	    //createMenuItem(rightClickMenu, SWT.PUSH,
	    //		"&Properties", null, -1, true, "");
	    createMenuItem(rightClickMenu, SWT.PUSH,
	    		"Read/Post Comments", null, -1, true, "openHashTalk");	    	    
	    createMenuItem(rightClickMenu, SWT.PUSH,
	    		"Copy Torrent URI to Clipboard", null, -1, true, "copyToClipboard");
	    resultsTable.setMenu(rightClickMenu);
	    rightClickMenu.setVisible(false);
	    
	    clipboard = new Clipboard(parentPlugin.getDisplay());	    
	    /*
	    
	    //shell.setMe-nu(m);
	    //return m;
		//Menu curMenu = createPopupMenu(shell);
		//MenuItem item = createMenuItem(curMenu, SWT.PUSH, "&About", 
        //        null, -1, true, "")
		MenuItem item = new MenuItem(curMenu, SWT.PUSH);
	    item.setText("Menu Item");
	    resultsTable.setMenu(curMenu);
	    curMenu.setVisible(false);
	    */	       
    	    	    	    
    	resultsTable.addMouseListener(new MouseAdapter() {
    		public void mouseDown(MouseEvent event) {
    			System.err.println("Mouse down " + event);
    			// Check that it is a right click
    			if (event.button == 3) {
    				System.err.println("In right click");    				
    				if (resultsTable.getMenu() != null && 
    						!resultsTable.getMenu().isDisposed()) { 
    					resultsTable.getMenu().setVisible(true);
    				}
    				
    				/*
    				
    				// TODO: Maybe set it to visible instead
    				// of creating a new menu every click?
    				if (resultsTable.getMenu() != null && 
    						!resultsTable.getMenu().isDisposed()) {    					
    					System.err.println("Disposing old menu");
    					resultsTable.getMenu().dispose();    					
    				}
    				
    			    Menu curMenu = new Menu(resultsTable.getShell(), SWT.POP_UP);
    			    //shell.setMe-nu(m);
    			    //return m;
    				//Menu curMenu = createPopupMenu(shell);
    				//MenuItem item = createMenuItem(curMenu, SWT.PUSH, "&About", 
                    //        null, -1, true, "")
    				MenuItem item = new MenuItem(curMenu, SWT.PUSH);
    			    item.setText("Menu Item");
    			    resultsTable.setMenu(curMenu);
    			    //shell.setMenu(curMenu);   
    			    //resultsTable.getShell().setMenu(curMenu);
    			    //curMenu.setLocation(event.x, event.y);
    			    curMenu.setVisible(true);
    			    //curMenu.setEnabled(true);
    			     *     			     
    			     */
    			}
    		}
    		
	  		public void mouseDoubleClick(MouseEvent event) {
	  			//System.out.println("Mouse double click");
	  			fetchTorrent();
	  		}
	  	});    	
	    resultsTable.setHeaderVisible(true);
	    resultsTable.setLinesVisible(true);
	    
	    SearchCallback tmpCB = new SearchCallback(
        		//searchButton, searchText, numResultsCombo, 
        		searchButton, searchText, searchProgress, resultsTable); 
	    
	    // Set the listeners to the search button and the search text
	    // after creating the table, as it modifies it.
        searchButton.addSelectionListener(tmpCB);        
        searchText.addKeyListener(tmpCB);
	}
	
	private void setDataTable(
			final Table seedsTable, String pluginKey, String splitKey) {
    	PluginConfig configModel = parentPlugin.getConfig();
    	String seedString = configModel.getPluginStringParameter(pluginKey);
    	String[] seedList = seedString.split(splitKey);    	
    	for (int i = 0; i < seedList.length; i++) {        	
        	if (!seedList[i].trim().equals("")) {  
        		TableItem item = new TableItem(seedsTable, 0);
        		item.setText(seedList[i]);
    		}
    	}
	}
	
	private void modifyTable(final Table seedsTable,
			String pluginKey, String splitKey, 
			Set<String> newItems, 
			Set<String> removeItems) {
    	String seedStr = parentPlugin.getConfig(
				).getPluginStringParameter(pluginKey);    	
    	String[] seedArray = seedStr.split(splitKey);
    	Set<String> seedSet = new TreeSet<String>();
    	seedSet.addAll(newItems);
    	for (int i = 0; i < seedArray.length; i++) {
    		if (!removeItems.contains(seedArray[i])) {
    			seedSet.add(seedArray[i]);	
    		}
    	}
    	StringBuffer strBuf = new StringBuffer();
    	Iterator<String> seedIt = seedSet.iterator();
    	while (seedIt.hasNext()) {
    		String curStr = seedIt.next().trim();
    		if (!curStr.equals("")) {
    			strBuf.append(curStr);
    			strBuf.append(splitKey);
    		}
    	}
       	parentPlugin.getConfig().setPluginParameter(
       			pluginKey, strBuf.toString());       	
    	seedsTable.removeAll();
    	setDataTable(seedsTable, pluginKey, splitKey);
	}
	
	private void createURLsGroup(final Composite optionsComp) {
		final String configStr 	= "urls";
		final String configSep	= " ";
		
	  	Group optionsGrp = new Group(optionsComp, SWT.NONE);
	  	optionsGrp.setText("NAT checker URLs");
	  	optionsGrp.setLayoutData(new GridData()); {
	  		GridLayout gridLayout = new GridLayout();
	  		gridLayout.numColumns = 2;
	  		optionsGrp.setLayout(gridLayout);
	  	}
	  	
	  	Label seedLabel = new Label(optionsGrp, SWT.LEFT);
	  	seedLabel.setText("Enter additional NAT-checker URL"); {
	  		GridData data = new GridData();
	  		data.horizontalSpan = 2;
	  		seedLabel.setLayoutData(data);
	  	}
	  	        
	  	final Label sepLabel1 = new Label(
	  			optionsGrp, SWT.SEPARATOR | SWT.HORIZONTAL); {
	  		GridData data = new GridData(GridData.FILL_HORIZONTAL);
	  		data.horizontalSpan = 2;
	  		sepLabel1.setLayoutData(data);
	  	}   
	      
	      // Add seed node text field
	  	final Text addText = new Text(optionsGrp, SWT.BORDER); {
	  		GridData data = new GridData();
		  	data.horizontalSpan = 1;
		  	data.widthHint = 250;
		  	addText.setLayoutData(data);
	  	}
	  	
	  	// Add buttons
	  	Button addButton = new Button(optionsGrp, SWT.PUSH);
	  	addButton.setText("Add URL");
	  	addButton.setLayoutData(new GridData()); 
	      
	  	// Create the table
	  	final Table seedsTable = new Table(optionsGrp, SWT.SINGLE | SWT.CHECK); {    	    
	  		final TableColumn seedCol = new TableColumn(seedsTable, SWT.LEFT);
	  		seedCol.setWidth(320);	
	      	GridData data = new GridData();
	      	data.horizontalSpan = 2;
	      	data.heightHint = 125;
	      	seedsTable.setLayoutData(data);        	
	  	}
	  	seedsTable.setLinesVisible(true);
	  	setDataTable(seedsTable, configStr, configSep);
	  	
	  	// Selection listener for add button
	  	addButton.addSelectionListener(new SelectionAdapter() {
	  		public void widgetSelected(SelectionEvent e) {
	  			String newSeed = addText.getText();
	  			if (newSeed.trim().equals("")) {
	  				return;	// Nothing to add
	  			}
	  			Set<String> newSeeds = new HashSet<String>();
	  			newSeeds.add(newSeed);            	
	  			modifyTable(seedsTable, configStr, 
	  				configSep, newSeeds, new HashSet<String>());
	  		}
	  	});	
	
	  	final Label sepLabel2 = new Label(
	  			optionsGrp, SWT.SEPARATOR | SWT.HORIZONTAL); {
	    	GridData data = new GridData(GridData.FILL_HORIZONTAL);
	    	data.horizontalSpan = 2;
	    	sepLabel2.setLayoutData(data);
	  	}   
	      
	  	Composite buttonComp = new Composite(optionsGrp, SWT.NONE);
	  	buttonComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	  	GridLayout buttonLayout = new GridLayout();
	  	buttonLayout.numColumns = 2;
	  	buttonComp.setLayout(buttonLayout);               
	
	  	// Remove items button
	  	Button removeButton = new Button(buttonComp, SWT.PUSH);
	  	removeButton.setText("Remove Checked");        
	  	removeButton.addSelectionListener(new SelectionAdapter() {
	  		public void widgetSelected(SelectionEvent e) {         
	          	TableItem[] itemsArray = seedsTable.getItems();
	          	Set<String> checkedSet = new HashSet<String>();
	          	for (int i = 0; i < itemsArray.length; i++) {
	          		TableItem curItem = itemsArray[i];
	          		if (curItem.getChecked()) {
	          			checkedSet.add(curItem.getText());
	          		}
	          	}
	          	modifyTable(seedsTable, configStr, 
	          		configSep, new HashSet<String>(), checkedSet);
	  		}
		});
		removeButton.setLayoutData(new GridData());	
	      
		// Reset items button
		Button resetButton = new Button(buttonComp, SWT.PUSH);
		resetButton.setText("Reset");        
		resetButton.addSelectionListener(new SelectionAdapter() {        	
			public void widgetSelected(SelectionEvent e) {
	          	parentPlugin.getConfig().setPluginParameter(
	          			configStr, parentPlugin.getNATCheckURL());
	          	seedsTable.removeAll();
	          	setDataTable(seedsTable, configStr, configSep);
			}
		});
	    resetButton.setLayoutData(new GridData()); 		
	}
	
	
	private void createSeedsGroup(final Composite optionsComp) {
		final String configStr 	= "seedNodes";
		final String configSep	= " ";
		
	      // Search Group
    	Group optionsGrp = new Group(optionsComp, SWT.NONE);
    	optionsGrp.setText("Seed Nodes");
    	optionsGrp.setLayoutData(new GridData()); {
    		GridLayout gridLayout = new GridLayout();
    		gridLayout.numColumns = 2;
    		optionsGrp.setLayout(gridLayout);
    	}
    	
    	Label seedLabel = new Label(optionsGrp, SWT.LEFT);
    	seedLabel.setText("Enter additional nodes (hostname:port)"); {
    		GridData data = new GridData();
    		data.horizontalSpan = 2;
    		seedLabel.setLayoutData(data);
    	}
    	        
        final Label sepLabel1 = new Label(optionsGrp, SWT.SEPARATOR | SWT.HORIZONTAL); {
	    	GridData data = new GridData(GridData.FILL_HORIZONTAL);
	    	data.horizontalSpan = 2;
	    	sepLabel1.setLayoutData(data);
        }   
        
        // Add seed node text field
    	final Text addText = new Text(optionsGrp, SWT.BORDER); {
        	GridData data = new GridData();
        	data.horizontalSpan = 1;
        	data.widthHint = 250;
        	addText.setLayoutData(data);
        }
    	
        // Add buttons
        Button addButton = new Button(optionsGrp, SWT.PUSH);
        addButton.setText("Add Seed");
        addButton.setLayoutData(new GridData()); 
        
    	// Create the table
    	final Table seedsTable = new Table(optionsGrp, SWT.SINGLE | SWT.CHECK); {    	    
            final TableColumn seedCol = new TableColumn(seedsTable, SWT.LEFT);
            //seedCol.setText("Seed Node");
            seedCol.setWidth(320);

        	GridData data = new GridData();
        	data.horizontalSpan = 2;
        	data.heightHint = 125;
        	seedsTable.setLayoutData(data);        	
    	}
    	seedsTable.setLinesVisible(true);
    	setDataTable(seedsTable, configStr, configSep);
    	
    	// Selection listener for add button
        addButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
            	String newSeed = addText.getText();
            	if (newSeed.trim().equals("")) {
            		return;	// Nothing to add
            	}
            	Set<String> newSeeds = new HashSet<String>();
            	newSeeds.add(newSeed);            	
            	modifyTable(seedsTable, configStr, 
            		configSep, newSeeds, new HashSet<String>());
            }
        });	

        final Label sepLabel2 = new Label(
        		optionsGrp, SWT.SEPARATOR | SWT.HORIZONTAL); {
	    	GridData data = new GridData(GridData.FILL_HORIZONTAL);
	    	data.horizontalSpan = 2;
	    	sepLabel2.setLayoutData(data);
        }   
        
    	Composite buttonComp = new Composite(optionsGrp, SWT.NONE);
    	buttonComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	GridLayout buttonLayout = new GridLayout();
    	buttonLayout.numColumns = 2;
    	buttonComp.setLayout(buttonLayout);               

        // Remove items button
        Button removeButton = new Button(buttonComp, SWT.PUSH);
        removeButton.setText("Remove Checked");        
        removeButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {         
            	TableItem[] itemsArray = seedsTable.getItems();
            	Set<String> checkedSet = new HashSet<String>();
            	for (int i = 0; i < itemsArray.length; i++) {
            		TableItem curItem = itemsArray[i];
            		if (curItem.getChecked()) {
            			checkedSet.add(curItem.getText());
            		}
            	}
            	modifyTable(seedsTable, configStr, 
            		configSep, new HashSet<String>(), checkedSet);
            }
        });
        removeButton.setLayoutData(new GridData());	
        
        // Reset items button
        Button resetButton = new Button(buttonComp, SWT.PUSH);
        resetButton.setText("Reset");        
        resetButton.addSelectionListener(new SelectionAdapter() {        	
            public void widgetSelected(SelectionEvent e) {
            	parentPlugin.getConfig().setPluginParameter(
            			configStr, parentPlugin.getSeedNodes());
            	seedsTable.removeAll();
            	setDataTable(seedsTable, configStr, configSep);
            }
        });
        resetButton.setLayoutData(new GridData()); 		
	}
	
    private void createOptionsTab(final TabFolder tabFolder, TabItem optionsTab) {
    	// Main canvas for the options tab
    	Composite optionsComp = new Composite(tabFolder, SWT.NONE);
    	optionsComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	GridLayout optionLayout = new GridLayout();
    	optionLayout.numColumns = 2;
    	optionsComp.setLayout(optionLayout);    	
    	// Add it to the options tab
    	optionsTab.setControl(optionsComp);
    	
    	createSeedsGroup(optionsComp);
    	createURLsGroup(optionsComp); 
    }
	
	private void initialize(Composite parent) {
    	tabFolder = new TabFolder(parent, SWT.BORDER);
    	tabFolder.setLayout(new GridLayout());
    	
    	TabItem searchTab = new TabItem(tabFolder, SWT.NULL);
    	searchTab.setText("Search");
    	//TabItem insertTab = new TabItem(tabFolder, SWT.NULL);
    	//insertTab.setText("Insert");
    	TabItem optionsTab = new TabItem(tabFolder, SWT.NULL);
    	optionsTab.setText("Options");
    	
    	//TabItem browserTab = new TabItem(tabFolder, SWT.NULL);
    	//browserTab.setText("Browser");
    	    	    
    	// Create the search 
    	createSearchTab(tabFolder, searchTab);
    	createOptionsTab(tabFolder, optionsTab); 
    	//createBrowser(tabFolder, browserTab);    	    
    }
	
	public void openHashTalk() {		
		if(resultsTable == null || resultsTable.isDisposed()) {
			return;
		}		
		//Get the selection
		TableItem[] items = resultsTable.getSelection();

		//If an item is selected (double click selects at least one)
		if(items.length == 1) {		
			//Grab selected item		
			TableItem item = items[0];
			String tor = item.getText(0);
			String url = item.getText(2);
			String com = item.getText(3);
			String full = tor + ":" + url + ":" + com;
						
			MessageDigest md;
			try {
				md = MessageDigest.getInstance("SHA1");
				md.update(full.getBytes());
				String urlHash = Base64.encode(md.digest());
				
				if (browserTab == null) {
					browserTab = new TabItem(tabFolder, SWT.NULL);
					browserTab.setText("Browser");
					createBrowser(tabFolder, browserTab);				
				}
				browser.setUrl("http://hashtalk.org/forum/title/" + urlHash);
				tabFolder.setSelection(browserTab);
			} catch (NoSuchAlgorithmException e) {
				System.err.println("Cannot perform SHA1 hash");
				e.printStackTrace();
			}			
		}	     
	}	
		
	
	/**
	 *  Creates the main window's contents
	 *  
	 *  NOTE: Code modified from 
	 *  http://www.java2s.com/Code/Java/SWT-JFace-Eclipse/Implementsawebbrowser.htm
	 *  	   
	 *  @param shell the main window
	 */
	private void createBrowser(final TabFolder tabFolder, TabItem browserTab) {
    	// Main canvas for the options tab
    	Composite browserComp = new Composite(tabFolder, SWT.NONE);
    	//browserComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    	//GridLayout browserLayout = new GridLayout();
    	//optionLayout.numColumns = 2;
    	browserComp.setLayout(new FormLayout());    	
    	// Add it to the options tab
    	browserTab.setControl(browserComp);
						
		//shell.setLayout(new FormLayout());

		// Create the composite to hold the buttons and text field
		Composite controls = new Composite(browserComp, SWT.NONE);
		FormData data = new FormData();
		data.top = new FormAttachment(0, 0);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		controls.setLayoutData(data);

		// Create the web browser
		browser = new Browser(browserComp, SWT.NONE);
		data = new FormData();
		data.top = new FormAttachment(controls);
		data.bottom = new FormAttachment(100, 0);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		browser.setLayoutData(data);
		


		// Create the controls and wire them to the browser
		controls.setLayout(new GridLayout(6, false));

		// Create the back button
		Button button = new Button(controls, SWT.PUSH);
	    button.setText("Back");
	    button.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		browser.back();
	    	}
	    });

	    // Create the forward button
	    button = new Button(controls, SWT.PUSH);
	    button.setText("Forward");
	    button.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		browser.forward();
	    	}
	    });

	    // Create the refresh button
	    button = new Button(controls, SWT.PUSH);
	    button.setText("Refresh");
	    button.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		browser.refresh();
	    	}
	    });

	    // Create the stop button
	    button = new Button(controls, SWT.PUSH);
	    button.setText("Stop");
	    button.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		browser.stop();
	    	}
	    });

	    // Create the address entry field and set focus to it
	    final Text url = new Text(controls, SWT.BORDER);
	    url.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    url.setFocus();
	    url.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent event) {
				// TODO Auto-generated method stub
				if (event.keyCode == SWT.CR || event.keyCode == SWT.KEYPAD_CR) {				
					browser.setUrl(url.getText());
				}
			}

			public void keyReleased(KeyEvent arg0) {
				// Do nothing				
			}   	
	    });
	    
		browser.addLocationListener(new LocationListener() {
			public void changed(LocationEvent event) {
				url.setText(event.location);
			}

			public void changing(LocationEvent event) {
			}
        });	    

	    // Create the go button
	    button = new Button(controls, SWT.PUSH);
	    button.setText("Go");
	    button.addSelectionListener(new SelectionAdapter() {
	    	public void widgetSelected(SelectionEvent event) {
	    		browser.setUrl(url.getText());
	    	}
	    });

	    // Allow users to hit enter to go to the typed URL
	    //shell.setDefaultButton(button);
	}	
	
    private void refresh() {}

	public Object getProperty(int property) {
		if (property == SearchProvider.PR_NAME ){
			return "Cubit Plugin";
		}
		if ( property == SearchProvider.PR_USE_ACCURACY_FOR_RANK ){
			return true;
		}
	    return null;
	}

	public SearchInstance search(Map search_parameters, SearchObserver observer)
			throws SearchException {
		// TODO Auto-generated method stub		
		Iterator mapIt = search_parameters.keySet().iterator();
		while (mapIt.hasNext()) {
			Object curObj = mapIt.next();
			System.out.println("--> " + curObj + " " + search_parameters.get(curObj));
		}
		// "s" --> search
		// "m" --> mature
		if (search_parameters.containsKey(SearchProvider.SP_SEARCH_TERM)) {
			return new AzureusSearchInstance(
					(String)search_parameters.get(SearchProvider.SP_SEARCH_TERM),					
					AzureusSearchInstance.normalResultsSize, observer);
		}		
		//return new searchInstance(search_parameters, observer);
		return null;
	}

	public void setProperty(int property, Object value) {
		// Do nothing for now		
	}
}
