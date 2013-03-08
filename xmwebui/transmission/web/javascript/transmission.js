/* Transmission Revision 13022 */
/**
 * Copyright © Dave Perrett, Malcolm Jarvis and Bruno Bierbaumer
 *
 * This file is licensed under the GPLv2.
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

function Transmission()
{
	this.initialize();
}

Transmission.prototype =
{
	/****
	*****
	*****  STARTUP
	*****
	****/

	initialize: function()
	{
		var e;

		// Initialize the helper classes
		this.remote = new TransmissionRemote(this);
		this.inspector = new Inspector(this, this.remote);
		this.prefsDialog = new PrefsDialog(this.remote);
		this.isMenuEnabled = !isMobileDevice;

		// Initialize the implementation fields
		this.filterText    = '';
		this._torrents     = {};
		this._rows         = [];
		this.dirtyTorrents = {};
		this.uriCache      = {};

		// Initialize the clutch preferences
		Prefs.getClutchPrefs(this);

		// Set up user events
		$(".numberinput").forceNumeric();
		$('#pause_all_link').click($.proxy(this.stopAllClicked,this));
		$('#resume_all_link').click($.proxy(this.startAllClicked,this));
		$('#pause_selected_link').click($.proxy(this.stopSelectedClicked,this));
		$('#resume_selected_link').click($.proxy(this.startSelectedClicked,this));
		$('#remove_link').click($.proxy(this.removeClicked,this));
		$('#open_link').click($.proxy(this.openTorrentClicked,this));

		$('#prefs-button').click($.proxy(this.showPrefsDialog,this));

		$('#upload_confirm_button').click($.proxy(this.confirmUploadClicked,this));
		$('#upload_cancel_button').click($.proxy(this.hideUploadDialog,this));
		$('#turtle-button').click($.proxy(this.toggleTurtleClicked,this));
		$('#compact-button').click($.proxy(this.toggleCompactClicked,this));
		$('#torrent_container').bind('dragover', $.proxy(this.dragenter,this));
		$('#torrent_container').bind('dragenter', $.proxy(this.dragenter,this));
		$('#torrent_container').bind('drop', $.proxy(this.drop,this));

		// tell jQuery to copy the dataTransfer property from events over if it exists
		jQuery.event.props.push("dataTransfer");

		$('#torrent_upload_form').submit(function() { $('#upload_confirm_button').click(); return false; });
		$('#inspector_close').click($.proxy(this.toggleInspector,this));

		if (!isMobileDevice) {
			$(document).bind('keydown', $.proxy(this.keyDown,this) );
			$(document).bind('keyup', $.proxy(this.keyUp, this) );
			$('#torrent_container').click( $.proxy(this.deselectAll,this) );
			$('#inspector_link').click( $.proxy(this.toggleInspector,this) );

			this.setupSearchBox();
			this.createContextMenu();
		}

		if (this.isMenuEnabled)
			this.createSettingsMenu();
 
		e = {};
		e.torrent_list              = $('#torrent_list')[0];
		e.toolbar_buttons           = $('#toolbar ul li');
		e.toolbar_pause_button      = $('#toolbar #pause_selected')[0];
		e.toolbar_pause_all_button  = $('#toolbar #pause_all')[0];
		e.toolbar_start_button      = $('#toolbar #resume_selected')[0];
		e.toolbar_start_all_button  = $('#toolbar #resume_all')[0];
		e.toolbar_remove_button     = $('#toolbar #remove')[0];
		this.elements = e;

		// Apply the prefs settings to the gui
		this.initializeSettings();

		// Get preferences & torrents from the daemon
		var async = false;
		this.loadDaemonPrefs(async);
		this.loadDaemonStats(async);
		this.initializeTorrents();
		this.refreshTorrents();
		this.togglePeriodicSessionRefresh(true);

		this.filterSetup();
	},

	loadDaemonPrefs: function(async) {
		this.remote.loadDaemonPrefs(function(data) {
			var o = data['arguments'];
			Prefs.getClutchPrefs(o);
			this.updateGuiFromSession(o);
		}, this, async);
	},

	loadImages: function() {
		for (var i=0, row; row=arguments[i]; ++i)
			jQuery("<img>").attr("src", row);
	},

	/*
	 * Load the clutch prefs and init the GUI according to those prefs
	 */
	initializeSettings: function()
	{
		Prefs.getClutchPrefs(this);

		if (this.isMenuEnabled)
		{
			$('#sort_by_' + this[Prefs._SortMethod]).selectMenuItem();

			if (this[Prefs._SortDirection] === Prefs._SortDescending)
				$('#reverse_sort_order').selectMenuItem();
		}

		this.initCompactMode();

		/* >> Vuze: Pairing Logout, hide turtle mode, 'Add' for isMobileDevice (Why?) */
		// determine whether to display logout button
		$.get("/isServicePaired", {}, function(responseText) {
			var json = eval('(' + responseText + ')')
			if (json.servicepaired)
				$("#log_out").show()
		});

		$("div.limit_turtle").hide()

		if (isMobileDevice) {
			$("div#open_link span").text("Add")
		}
        /* << Vuze */
	},
	
	/*
	 * Set up the search box
	 */
	setupSearchBox: function()
	{
		var tr = this;
		var search_box = $('#torrent_search');
		search_box.bind('keyup click', function() {
			tr.setFilterText(this.value);
		});
		if (!$.browser.safari)
		{
			search_box.addClass('blur');
			search_box[0].value = 'Filter';
			search_box.bind('blur', function() {
				if (this.value === '') {
					$(this).addClass('blur');
					this.value = 'Filter';
					tr.setFilterText(null);
				}
			}).bind('focus', function() {
				if ($(this).is('.blur')) {
					this.value = '';
					$(this).removeClass('blur');
				}
			});
		}
	},

	/**
	 * Create the torrent right-click menu
	 */
	createContextMenu: function() {
		var tr = this;
		var bindings = {
			context_pause_selected:       function() { tr.stopSelectedTorrents(); },
			context_resume_selected:      function() { tr.startSelectedTorrents(false); },
			context_resume_now_selected:  function() { tr.startSelectedTorrents(true); },
			context_remove:               function() { tr.removeSelectedTorrents(); },
			context_removedata:           function() { tr.removeSelectedTorrentsAndData(); },
			context_verify:               function() { tr.verifySelectedTorrents(); },
			context_reannounce:           function() { tr.reannounceSelectedTorrents(); },
			context_toggle_inspector:     function() { tr.toggleInspector(); },
			context_select_all:           function() { tr.selectAll(); },
			context_deselect_all:         function() { tr.deselectAll(); },
			context_move_top:             function() { tr.moveTop(); },
			context_move_up:              function() { tr.moveUp(); },
			context_move_down:            function() { tr.moveDown(); },
			context_move_bottom:          function() { tr.moveBottom(); }
		};

		// Set up the context menu
		$('ul#torrent_list').contextMenu('torrent_context_menu', {
			bindings:          bindings,
			menuStyle:         { width: '310px', backgroundColor: '#fff', border: 'none', padding: '5px 0', textAlign: 'left' },
			itemStyle:         { backgroundColor: 'transparent', margin: '0', padding: '3px 10px 3px 20px', color: '#000', cursor: 'default', border: 'none'},
			itemHoverStyle:    { backgroundColor: '#24e', color: '#fff', border: 'none'},
			shadow:            false,
			boundingElement:   $('div#torrent_container'),
			boundingRightPad:  20,
			boundingBottomPad: 5,
			onContextMenu: function(ev) {
				var element = $(ev.target).closest('.torrent')[0];
				var i = $('#torrent_list > li').index(element);
				if ((i!==-1) && !tr._rows[i].isSelected())
					tr.setSelectedRow(tr._rows[i]);
				return true;
			}
		});
	},

	createSettingsMenu: function() {
		var tr = this;
		$('#settings_menu').transMenu({
			selected_char: '&#x2714;',
			/* >> Vuze
			direction: 'up',
			*/
			direction: 'down',
			/* << Vuze */
			onClick: $.proxy(this.onMenuClicked,this)
		});

		$('#unlimited_download_rate').selectMenuItem();
		$('#unlimited_upload_rate').selectMenuItem();
	},


	/****
	*****
	*****  UTILITIES
	*****
	****/

	getAllTorrents: function()
	{
		var torrents = [];
		for (var key in this._torrents)
			torrents.push(this._torrents[key]);
		return torrents;
	},

	getTorrentIds: function(torrents)
	{
		return $.map(torrents.slice(0), function(t) {return t.getId();});
	},

	scrollToRow: function(row)
	{
		if (isMobileDevice) // FIXME: why?
			return;

		var list = $('#torrent_container'),
		    scrollTop = list.scrollTop(),
		    innerHeight = list.innerHeight(),
		    offsetTop = row.getElement().offsetTop,
		    offsetHeight = $(row.getElement()).outerHeight();

		if (offsetTop < scrollTop)
			list.scrollTop(offsetTop);
		else if (innerHeight + scrollTop < offsetTop + offsetHeight)
			list.scrollTop(offsetTop + offsetHeight - innerHeight);
	},

	seedRatioLimit: function() {
		var p = this._prefs;
		if (p && p.seedRatioLimited)
			return p.seedRatioLimit;
		return -1;
	},

	setPref: function(key, val)
	{
		this[key] = val;
		Prefs.setValue(key, val);
	},

	/****
	*****
	*****  SELECTION
	*****
	****/

	getSelectedRows: function() {
		return $.grep(this._rows, function(r) {return r.isSelected();});
	},

	getSelectedTorrents: function() {
		return $.map(this.getSelectedRows(),function(r) {
			return r.getTorrent();
		});
	},

	getSelectedTorrentIds: function() {
		return this.getTorrentIds(this.getSelectedTorrents());
	},

	setSelectedRow: function(row) {
		$(this.elements.torrent_list).children('.selected').removeClass('selected');
		this.selectRow(row);
	},

	selectRow: function(row) {
		$(row.getElement()).addClass('selected');
		this.callSelectionChangedSoon();
	},

	deselectRow: function(row) {
		$(row.getElement()).removeClass('selected');
		this.callSelectionChangedSoon();
	},

	selectAll: function() {
		$(this.elements.torrent_list).children().addClass('selected');
		this.callSelectionChangedSoon();
	},
	deselectAll: function() {
		$(this.elements.torrent_list).children('.selected').removeClass('selected');
		this.callSelectionChangedSoon();
		delete this._last_torrent_clicked;
	},

	indexOfLastTorrent: function() {
		for (var i=0, r; r=this._rows[i]; ++i)
			if (r.getTorrentId() === this._last_torrent_clicked)
				return i;
		return -1;
	},

	// Select a range from this row to the last clicked torrent
	selectRange: function(row)
	{
		var last = this.indexOfLastTorrent();

		if (last === -1)
		{
			this.selectRow(row);
		}
		else // select the range between the prevous & current
		{
			var next = this._rows.indexOf(row);
			var min = Math.min(last, next);
			var max = Math.max(last, next);
			for (var i=min; i<=max; ++i)
				this.selectRow(this._rows[i]);
		}

		this.callSelectionChangedSoon();
	},

	selectionChanged: function()
	{
		this.updateButtonStates();

		this.inspector.setTorrents(this.inspectorIsVisible() ? this.getSelectedTorrents() : []);

		clearTimeout(this.selectionChangedTimer);
		delete this.selectionChangedTimer;

	},

	callSelectionChangedSoon: function()
	{
		if (!this.selectionChangedTimer)
		{
			var callback = $.proxy(this.selectionChanged,this),
			    msec = 200;
			this.selectionChangedTimer = setTimeout(callback, msec);
		}
	},

	/*--------------------------------------------
	 *
	 *  E V E N T   F U N C T I O N S
	 *
	 *--------------------------------------------*/

	/*
	 * Process key event
	 */
	keyDown: function(ev)
	{
		var handled = false,
		    rows = this._rows,
		    up = ev.keyCode === 38, // up key pressed
		    dn = ev.keyCode === 40, // down key pressed
		    shift = ev.keyCode === 16; // shift key pressed

		if ((up || dn) && rows.length)
		{
			var last = this.indexOfLastTorrent(),
			    i = last,
			    anchor = this._shift_index,
			    r,
			    min = 0,
			    max = rows.length - 1;

			if (dn && (i+1 <= max))
				++i;
			else if (up && (i-1 >= min))
				--i;

			var r = rows[i];

			if (anchor >= 0)
			{
				// user is extending the selection
				// with the shift + arrow keys...
				if (   ((anchor <= last) && (last < i))
				    || ((anchor >= last) && (last > i)))
				{
					this.selectRow(r);
				}
				else if (((anchor >= last) && (i > last))
				      || ((anchor <= last) && (last > i)))
				{
					this.deselectRow(rows[last]);
				}
			}
			else
			{
				if (ev.shiftKey)
					this.selectRange(r);
				else
					this.setSelectedRow(r);
			}
			this._last_torrent_clicked = r.getTorrentId();
			this.scrollToRow(r);
			handled = true;
		}
		else if (shift)
		{
			this._shift_index = this.indexOfLastTorrent();
		}

		return !handled;
	},

	keyUp: function(ev) {
		if (ev.keyCode === 16) // shift key pressed
			delete this._shift_index;
	},

	isButtonEnabled: function(ev) {
		var p = (ev.target || ev.srcElement).parentNode;
		return p.className!=='disabled'
		    && p.parentNode.className!=='disabled';
	},

	stopAllClicked: function(ev) {
		if (this.isButtonEnabled(ev)) {
			this.stopAllTorrents();
			this.hideMobileAddressbar();
		}
	},

	stopSelectedClicked: function(ev) {
		if (this.isButtonEnabled(ev)) {
			this.stopSelectedTorrents();
			this.hideMobileAddressbar();
		}
	},

	startAllClicked: function(ev) {
		if (this.isButtonEnabled(ev)) {
			this.startAllTorrents();
			this.hideMobileAddressbar();
		}
	},

	startSelectedClicked: function(ev) {
		if (this.isButtonEnabled(ev)) {
			this.startSelectedTorrents(false);
			this.hideMobileAddressbar();
		}
	},

	openTorrentClicked: function(ev) {
		if (this.isButtonEnabled(ev)) {
			$('body').addClass('open_showing');
			this.uploadTorrentFile();
			this.updateButtonStates();
		}
	},

	dragenter: function(ev) {
		if (ev.dataTransfer && ev.dataTransfer.types) {
			var types = ["text/uri-list", "text/plain"];
			for (var i = 0; i < types.length; ++i) {
				// it would be better to look at the links here;
				// sadly, with Firefox, trying would throw.
				if (ev.dataTransfer.types.contains(types[i])) {
					ev.stopPropagation();
					ev.preventDefault();
					ev.dropEffect = "copy";
					return false;
				}
			}
		}
		else if (ev.dataTransfer) {
			ev.dataTransfer.dropEffect = "none";
		}
		return true;
	},

	drop: function(ev)
	{
		var i, uri, uris=null,
		    types = ["text/uri-list", "text/plain"];
		    paused = this.shouldAddedTorrentsStart();

		if (!ev.dataTransfer || !ev.dataTransfer.types)
			return true;

		for (i=0; !uris && i<types.length; ++i)
			if (ev.dataTransfer.types.contains(types[i]))
				uris = ev.dataTransfer.getData(types[i]).split("\n");

		for (i=0; uri=uris[i]; ++i) {
			if (/^#/.test(uri)) // lines which start with "#" are comments
				continue;
			if (/^[a-z-]+:/i.test(uri)) // close enough to a url
				this.remote.addTorrentByUrl(uri, paused);
		}

		ev.preventDefault();
		return false;
	},

	hideUploadDialog: function() {
		$('body.open_showing').removeClass('open_showing');
		$('#upload_container').hide();
		this.updateButtonStates();
	},

	confirmUploadClicked: function() {
		this.uploadTorrentFile(true);
		this.hideUploadDialog();
	},

	removeClicked: function(ev) {
		if (this.isButtonEnabled(ev)) {
			this.removeSelectedTorrents();
			this.hideMobileAddressbar();
		}
	},

	// turn the periodic ajax session refresh on & off
	togglePeriodicSessionRefresh: function(enabled) {
		clearInterval(this.sessionInterval);
		delete this.sessionInterval;
		if (enabled) {
		        var callback = $.proxy(this.loadDaemonPrefs,this),
			    msec = 8000;
			this.sessionInterval = setInterval(callback, msec);
		}
	},

	toggleTurtleClicked: function()
	{
		var o = {};
		o[RPC._TurtleState] = !$('#turtle-button').hasClass('selected');
		this.remote.savePrefs(o);
	},

	/*--------------------------------------------
	 *
	 *  I N T E R F A C E   F U N C T I O N S
	 *
	 *--------------------------------------------*/

	showPrefsDialog: function()
	{
		this.prefsDialog.show();
	},

	setFilterText: function(search) {
		this.filterText = search ? search.trim() : null;
		this.refilter(true);
	},

	setSortMethod: function(sort_method) {
		this.setPref(Prefs._SortMethod, sort_method);
		this.refilter(true);
	},

	setSortDirection: function(direction) {
		this.setPref(Prefs._SortDirection, direction);
		this.refilter(true);
	},

	onMenuClicked: function(ev)
	{
		var o, dir,
		    id = ev.target.id,
		    remote = this.remote,
		    element = $(ev.target);

		if (element.hasClass('sort-mode'))
		{
			element.parent().find('.sort-mode').each(function() {
				element.parent().deselectMenuItem();
			});
			element.selectMenuItem();
			this.setSortMethod(id.replace(/sort_by_/, ''));
		}
		else if (element.hasClass('upload-speed'))
		{
			o = {};
			o[RPC._UpSpeedLimit] = parseInt(ev.target.innerHTML);
			o[RPC._UpSpeedLimited] = true;
			remote.savePrefs(o);
		}
		else if (element.hasClass('download-speed'))
		{
			o = {};
			o[RPC._DownSpeedLimit] = parseInt(ev.target.innerHTML);
			o[RPC._DownSpeedLimited] = true;
			remote.savePrefs(o);
		}
		else switch (id)
		{
			/* >> Vuze: Add compact view menu back in */
			case 'compact_view':
				this.toggleCompactClicked();
				break;
			/* << Vuze */
			
			case 'preferences':
				this.showPrefsDialog();
				break;

			case 'statistics':
				this.showStatsDialog();
				break;

			case 'about-button':
				o = 'Transmission ' + this.serverVersion;
				$('#about-dialog #about-title').html(o);
				$('#about-dialog').dialog({
					title: 'About',
					show: 'fade',
					hide: 'fade'
				});
				break;

			case 'homepage':
				window.open('http://www.vuze.com/');
				break;

			case 'tipjar':
				window.open('http://www.transmissionbt.com/donate.php?sourceRef=Vuze');
				break;	

			case 'unlimited_download_rate':
				o = {};
				o[RPC._DownSpeedLimited] = false;
				remote.savePrefs(o);
				break;

			case 'limited_download_rate':
				o = {};
				o[RPC._DownSpeedLimited] = true;
				remote.savePrefs(o);
				break;

			case 'unlimited_upload_rate':
				o = {};
				o[RPC._UpSpeedLimited] = false;
				remote.savePrefs(o);
				break;

			case 'limited_upload_rate':
				o = {};
				o[RPC._UpSpeedLimited] = true;
				remote.savePrefs(o);
				break;

			case 'reverse_sort_order':
				if (element.menuItemIsSelected()) {
					dir = Prefs._SortAscending;
					element.deselectMenuItem();
				} else {
					dir = Prefs._SortDescending;
					element.selectMenuItem();
				}
				this.setSortDirection(dir);
				break;

			default:
				console.log('unhandled: ' + id);
				break;

		}
		$('#settings_menu').trigger('closemenu');
		ev.stopImmediatePropagation();
	},

	onTorrentChanged: function(ev, tor)
	{
		// update our dirty fields
		this.dirtyTorrents[ tor.getId() ] = true;

		// enqueue ui refreshes
		this.refilterSoon();
		this.updateButtonsSoon();
	},

	updateFromTorrentGet: function(updates, removed_ids)
	{
		var i, o, t, id, needed, needinfo = [],
		    callback, fields;

		for (i=0; o=updates[i]; ++i)
		{
			id = o.id;
			if ((t = this._torrents[id]))
			{
				needed = t.needsMetaData();
				t.refresh(o);
				if (needed && !t.needsMetaData())
					needinfo.push(id);
			}
			else {
				t = this._torrents[id] = new Torrent(o);
				this.dirtyTorrents[id] = true;
				callback = $.proxy(this.onTorrentChanged,this);
				$(t).bind('dataChanged',callback);
				// do we need more info for this torrent?
				if(!('name' in t.fields) || !('status' in t.fields))
					needinfo.push(id);
			}
		}

		if (needinfo.length) {
			// whee, new torrents! get their initial information.
			fields = ['id'].concat(Torrent.Fields.Metadata,
			                       Torrent.Fields.Stats);
			this.updateTorrents(needinfo, fields);
			this.refilterSoon();
		}

		if (removed_ids) {
			this.deleteTorrents(removed_ids);
			this.refilterSoon();
		}
	},

	updateTorrents: function(ids, fields)
	{
		this.remote.updateTorrents(ids, fields,
		                           this.updateFromTorrentGet, this);
	},

	refreshTorrents: function()
	{
		var callback = $.proxy(this.refreshTorrents,this),
		    msec = this[Prefs._RefreshRate] * 1000,
		    fields = ['id'].concat(Torrent.Fields.Stats);

		// send a request right now
		this.updateTorrents('recently-active', fields);

		// schedule the next request
		clearTimeout(this.refreshTorrentsTimeout);
		this.refreshTorrentsTimeout = setTimeout(callback, msec);
	},

	initializeTorrents: function()
	{
		var fields = ['id'].concat(Torrent.Fields.Metadata,
		                           Torrent.Fields.Stats);
		this.updateTorrents(null, fields);
	},

	onRowClicked: function(ev)
	{
		var meta_key = ev.metaKey || ev.ctrlKey,
		    row = ev.currentTarget.row;

		// handle the per-row "torrent_resume" button
		if (ev.target.className === 'torrent_resume') {
			this.startTorrent(row.getTorrent());
			return;
		}

		// handle the per-row "torrent_pause" button
		if (ev.target.className === 'torrent_pause') {
			this.stopTorrent(row.getTorrent());
			return;
		}

		// Prevents click carrying to parent element
		// which deselects all on click
		ev.stopPropagation();
		// but still hide the context menu if it is showing
		$('#jqContextMenu').hide();

		/* >> Vuze */
		// handle the per-row "torrent_info" button
		if (ev.target.className === 'torrent_info') {
			if (!row.isSelected()) {
				this.setSelectedRow(row);
			}
			this.setInspectorVisible(true);
			return;
		}
		/* << Vuze */

		if (isMobileDevice) {
			if (row.isSelected())
				this.setInspectorVisible(true);
			this.setSelectedRow(row);

		} else if (ev.shiftKey) {
			this.selectRange(row);
			// Need to deselect any selected text
			window.focus();

		// Apple-Click, not selected
		} else if (!row.isSelected() && meta_key) {
			this.selectRow(row);

		// Regular Click, not selected
		} else if (!row.isSelected()) {
			this.setSelectedRow(row);

		// Apple-Click, selected
		} else if (row.isSelected() && meta_key) {
			this.deselectRow(row);

		// Regular Click, selected
		} else if (row.isSelected()) {
			this.setSelectedRow(row);
		}

		this._last_torrent_clicked = row.getTorrentId();
	},

	deleteTorrents: function(ids)
	{
		var i, id;

		if (ids && ids.length)
		{
			for (i=0; id=ids[i]; ++i) {
				this.dirtyTorrents[id] = true;
				delete this._torrents[id];
			}
			this.refilter();
		}
	},

	/*
	 * Select a torrent file to upload
	 * FIXME
	 */
	uploadTorrentFile: function(confirmed)
	{
		// Display the upload dialog
		if (! confirmed) {
			$('input#torrent_upload_file').attr('value', '');
			$('input#torrent_upload_url').attr('value', '');
			$('input#torrent_auto_start').attr('checked', this.shouldAddedTorrentsStart());
			$('#upload_container').show();
			$('#torrent_upload_url').focus();

		// Submit the upload form
		} else {
			var args = {};
			var remote = this.remote;
			var paused = !$('#torrent_auto_start').is(':checked');
			if ('' != $('#torrent_upload_url').val()) {
				remote.addTorrentByUrl($('#torrent_upload_url').val(), { paused: paused });
			} else {
				/* Vuze: Not sure if we want ../upload instead */
				args.url = '/transmission/upload?paused=' + paused;
				args.type = 'POST';
				args.data = { 'X-Transmission-Session-Id' : remote._token };
				args.dataType = 'xml';
				args.iframe = true;
				$('#torrent_upload_form').ajaxSubmit(args);
			}
		}
	},

	removeSelectedTorrents: function() {
		var torrents = this.getSelectedTorrents();
		if (torrents.length)
			this.promptToRemoveTorrents(torrents);
	},

	removeSelectedTorrentsAndData: function() {
		var torrents = this.getSelectedTorrents();
		if (torrents.length)
			this.promptToRemoveTorrentsAndData(torrents);
	},

	promptToRemoveTorrents:function(torrents)
	{
		if (torrents.length === 1)
		{
			var torrent = torrents[0],
			    header = 'Remove ' + torrent.getName() + '?',
			    message = 'Once removed, continuing the transfer will require the torrent file. Are you sure you want to remove it?';
			dialog.confirm(header, message, 'Remove', 'transmission.removeTorrents', torrents);
		}
		else
		{
			var header = 'Remove ' + torrents.length + ' transfers?',
			    message = 'Once removed, continuing the transfers will require the torrent files. Are you sure you want to remove them?';
			dialog.confirm(header, message, 'Remove', 'transmission.removeTorrents', torrents);
		}
	},

	promptToRemoveTorrentsAndData:function(torrents)
	{
		if (torrents.length === 1)
		{
			var torrent = torrents[0],
			    header = 'Remove ' + torrent.getName() + ' and delete data?',
			    message = 'All data downloaded for this torrent will be deleted. Are you sure you want to remove it?';
			dialog.confirm(header, message, 'Remove', 'transmission.removeTorrentsAndData', torrents);
		}
		else
		{
			var header = 'Remove ' + torrents.length + ' transfers and delete data?',
			    message = 'All data downloaded for these torrents will be deleted. Are you sure you want to remove them?';
			dialog.confirm(header, message, 'Remove', 'transmission.removeTorrentsAndData', torrents);
		}
	},

	removeTorrents: function(torrents) {
		var ids = this.getTorrentIds(torrents);
		this.remote.removeTorrents(ids, this.refreshTorrents, this);
	},

	removeTorrentsAndData: function(torrents) {
		this.remote.removeTorrentsAndData(torrents);
	},

	verifySelectedTorrents: function() {
		this.verifyTorrents(this.getSelectedTorrents());
	},

	reannounceSelectedTorrents: function() {
		this.reannounceTorrents(this.getSelectedTorrents());
	},

	startSelectedTorrents: function(force) {
		this.startTorrents(this.getSelectedTorrents(), force);
	},
	startAllTorrents: function() {
		this.startTorrents(this.getAllTorrents(), false);
	},
	startTorrent: function(torrent) {
		this.startTorrents([ torrent ], false);
	},

	startTorrents: function(torrents, force) {
		this.remote.startTorrents(this.getTorrentIds(torrents), force,
		                          this.refreshTorrents, this);
	},
	verifyTorrent: function(torrent) {
		this.verifyTorrents([ torrent ]);
	},
	verifyTorrents: function(torrents) {
		this.remote.verifyTorrents(this.getTorrentIds(torrents),
		                           this.refreshTorrents, this);
	},

	reannounceTorrent: function(torrent) {
		this.reannounceTorrents([ torrent ]);
	},
	reannounceTorrents: function(torrents) {
		this.remote.reannounceTorrents(this.getTorrentIds(torrents),
		                               this.refreshTorrents, this);
	},

	stopSelectedTorrents: function() {
		this.stopTorrents(this.getSelectedTorrents());
	},
	stopAllTorrents: function() {
		this.stopTorrents(this.getAllTorrents());
	},
	stopTorrent: function(torrent) {
		this.stopTorrents([ torrent ]);
	},
	stopTorrents: function(torrents) {
		this.remote.stopTorrents(this.getTorrentIds(torrents),
		                         this.refreshTorrents, this);
	},
	changeFileCommand: function(torrentId, rowIndices, command) {
		this.remote.changeFileCommand(torrentId, rowIndices, command);
	},

	hideMobileAddressbar: function(delaySecs) {
		if (isMobileDevice && !scroll_timeout) {
			var callback = $.proxy(this.doToolbarHide,this),
			    msec = delaySecs*1000 || 150;
			scroll_timeout = setTimeout(callback,msec);
		}
	},
	doToolbarHide: function() {
		window.scrollTo(0,1);
		scroll_timeout=null;
	},

	// Queue
	moveTop: function() {
		this.remote.moveTorrentsToTop(this.getSelectedTorrentIds(),
		                              this.refreshTorrents, this);
	},
	moveUp: function() {
		this.remote.moveTorrentsUp(this.getSelectedTorrentIds(),
		                           this.refreshTorrents, this);
	},
	moveDown: function() {
		this.remote.moveTorrentsDown(this.getSelectedTorrentIds(),
		                             this.refreshTorrents, this);
	},
	moveBottom: function() {
		this.remote.moveTorrentsToBottom(this.getSelectedTorrentIds(),
		                                 this.refreshTorrents, this);
	},

	/***
	****
	***/

	updateGuiFromSession: function(o)
	{
		var limit, limited, e, b, text,
                    fmt = Transmission.fmt,
                    menu = $('#settings_menu');

		this.serverVersion = o.version;

		this.prefsDialog.set(o);

		if (RPC._TurtleState in o)
		{
			b = o[RPC._TurtleState];
			e = $('#turtle-button');
			text = [ 'Click to ', (b?'disable':'enable'),
			         ' Temporary Speed Limits (',
			         fmt.speed(o[RPC._TurtleUpSpeedLimit]),
			         ' up,',
			         fmt.speed(o[RPC._TurtleDownSpeedLimit]),
			         ' down)' ].join('');
			e.toggleClass('selected', b);
			e.attr('title', text);
		}

		if (this.isMenuEnabled && (RPC._DownSpeedLimited in o)
		                       && (RPC._DownSpeedLimit in o))
		{
			limit = o[RPC._DownSpeedLimit];
			limited = o[RPC._DownSpeedLimited];

			e = menu.find('#limited_download_rate');
                        e.html('Limit (' + fmt.speed(limit) + ')');

                        if (!limited)
                        	e = menu.find('#unlimited_download_rate');
                        e.deselectMenuSiblings().selectMenuItem();
		}

		if (this.isMenuEnabled && (RPC._UpSpeedLimited in o)
		                       && (RPC._UpSpeedLimit in o))
		{
			limit = o[RPC._UpSpeedLimit];
			limited = o[RPC._UpSpeedLimited];

			e = menu.find('#limited_upload_rate');
                        e.html('Limit (' + fmt.speed(limit) + ')');

                        if (!limited)
                        	e = menu.find('#unlimited_upload_rate');
                        e.deselectMenuSiblings().selectMenuItem();
		}
		
		vz.updatePrefs(o);
	},

	updateStatusbar: function()
	{
		var i, row, text,
		    u=0, d=0,
		    fmt = Transmission.fmt,
		    torrents = this.getAllTorrents();

		this.refreshFilterButton();

		// up/down speed
		for (i=0; row=torrents[i]; ++i) {
			u += row.getUploadSpeed();
			d += row.getDownloadSpeed();
		}

		text = u ? '&uarr; ' + fmt.speedBps(u) : '';
		setInnerHTML($('#statusbar #speed-up-label')[0], text);

		text = d ? '&darr; ' + fmt.speedBps(d) : '';
		setInnerHTML($('#statusbar #speed-dn-label')[0], text);
	},

	setEnabled: function(key, flag)
	{
		$(key).toggleClass('disabled', !flag);
	},

	updateButtonsSoon: function()
	{
		if (!this.buttonRefreshTimer)
		{
			var callback = $.proxy(this.updateButtonStates,this),
			    msec = 100;
			this.buttonRefreshTimer = setTimeout(callback, msec);
		}
	},

	updateButtonStates: function()
	{
		var e = this.elements,
		    haveActive = false,
		    havePaused = false,
		    haveSel = false,
		    haveActiveSel = false,
		    havePausedSel = false;

		clearTimeout(this.buttonRefreshTimer);
		delete this.buttonRefreshTimer;

		for (var i=0, row; row=this._rows[i]; ++i) {
			var isStopped = row.getTorrent().isStopped();
			var isSelected = row.isSelected();
			if (!isStopped) haveActive = true;
			if (isStopped) havePaused = true;
			if (isSelected) haveSel = true;
			if (isSelected && !isStopped) haveActiveSel = true;
			if (isSelected && isStopped) havePausedSel = true;
		}

		this.setEnabled(e.toolbar_pause_button,       haveActiveSel);
		this.setEnabled(e.toolbar_start_button,       havePausedSel);
		this.setEnabled(e.toolbar_remove_button,      haveSel);
		this.setEnabled(e.toolbar_pause_all_button,   haveActive);
		this.setEnabled(e.toolbar_start_all_button,   havePaused);
	},

	/****
	*****
	*****  INSPECTOR
	*****
	****/

	inspectorIsVisible: function()
	{
		return $('#torrent_inspector').is(':visible');
	},
	toggleInspector: function()
	{
		this.setInspectorVisible(!this.inspectorIsVisible());
	},
	setInspectorVisible: function(visible)
	{
		if (visible)
			this.inspector.setTorrents(this.getSelectedTorrents());

		// update the ui widgetry
		$('#torrent_inspector').toggle(visible);
		if (isMobileDevice) {
			$('body').toggleClass('inspector_showing',visible);
			this.hideMobileAddressbar();
		} else {
			var w = visible ? $('#torrent_inspector').outerWidth() + 1 + 'px' : '0px';
			$('#torrent_container')[0].style.right = w;
		}
		setInnerHTML($('ul li#context_toggle_inspector')[0], (visible?'Hide':'Show')+' Inspector');
	},

	/****
	*****
	*****  FILTER
	*****
	****/

	filterSetup: function()
	{
		var tr = this,
		    popup = $('#filter-popup');

		popup.dialog({
			autoOpen: false,
			position: isMobileDevice ? 'center' : [40,80],
			show: 'blind',
			hide: 'blind',
			title: 'Show',
			width: 315
		});

		$('#filter-button').click(function() {
			if (popup.is(":visible"))
				popup.dialog('close');
			else {
				tr.refreshFilterPopup();
				popup.dialog('open');
			}
		});
		this.refreshFilterButton();
	},

	refreshFilterButton: function()
	{
		var o, tmp, text, torrent_count,
		    state = this[Prefs._FilterMode],
		    state_all = state === Prefs._FilterAll,
		    state_string = this.getStateString(state),
		    tracker = this.filterTracker,
		    tracker_all = !tracker,
		    tracker_string = tracker ? this.getReadableDomain(tracker) : '',
		    visible_count = this._rows.length;

		// count the total number of torrents
		torrent_count = 0;
		o = this._torrents;
		for (tmp in o)
			if (o.hasOwnProperty(tmp))
				++torrent_count;

		text = 'Show <span class="filter-selection">';
		if (state_all && tracker_all)
			text += 'All';
		else if (state_all)
			text += tracker_string;
		else if (tracker_all)
			text += state_string;
		else
			text += state_string + '</span> at <span class="filter-selection">' + tracker_string;
		text += '</span> &mdash; ';

		if (torrent_count !== visible_count)
			text += visible_count.toStringWithCommas() + ' of ' + torrent_count.toStringWithCommas();
		else if (torrent_count === 1)
			text += '1 Transfer';
		else
			text += torrent_count.toStringWithCommas() + ' Transfers';
		$('#filter-button').html(text);
	},

	refilterSoon: function()
	{
		if (!this.refilterTimer) {
			var tr = this,
			    callback = function(){tr.refilter(false);},
			    msec = 100;
			this.refilterTimer = setTimeout(callback, msec);
		}
	},

	sortRows: function(rows)
	{
		var i, tor, row,
		    id2row = {},
		    torrents = [];

		for (i=0; row=rows[i]; ++i) {
			tor = row.getTorrent();
			torrents.push(tor);
			id2row[ tor.getId() ] = row;
		}

		Torrent.sortTorrents(torrents, this[Prefs._SortMethod],
		                               this[Prefs._SortDirection]);

		for (i=0; tor=torrents[i]; ++i)
			rows[i] = id2row[ tor.getId() ];
	},

	refilter: function(rebuildEverything)
	{
		var i, e, id, t, row, tmp, rows, clean_rows, dirty_rows, frag,
		    sort_mode = this[Prefs._SortMethod],
		    sort_direction = this[Prefs._SortDirection],
		    filter_mode = this[Prefs._FilterMode],
		    filter_text = this.filterText,
		    filter_tracker = this.filterTracker,
		    renderer = this.torrentRenderer,
		    list = this.elements.torrent_list,
		    old_sel_count = $(list).children('.selected').length;

		clearTimeout(this.refilterTimer);
		delete this.refilterTimer;

		if (rebuildEverything) {
			$(list).empty();
			this._rows = [];
			for (id in this._torrents)
				this.dirtyTorrents[id] = true;
		}

		// rows that overlap with dirtyTorrents need to be refiltered.
		// those that don't are 'clean' and don't need refiltering.
		clean_rows = [];
		dirty_rows = [];
		for (i=0; row=this._rows[i]; ++i) {
			if(row.getTorrentId() in this.dirtyTorrents)
				dirty_rows.push(row);
			else
				clean_rows.push(row);
		}

		// remove the dirty rows from the dom
		e = [];
		for (i=0; row=dirty_rows[i]; ++i)
			e.push (row.getElement());
		$(e).detach();

		// drop any dirty rows that don't pass the filter test
		tmp = [];
		for (i=0; row=dirty_rows[i]; ++i) {
			id = row.getTorrentId();
			t = this._torrents[ id ];
			if (t && t.test(filter_mode, filter_text, filter_tracker))
				tmp.push(row);
			delete this.dirtyTorrents[id];
		}
		dirty_rows = tmp;

		// make new rows for dirty torrents that pass the filter test
		// but don't already have a row
		for (id in this.dirtyTorrents) {
			t = this._torrents[id];
			if (t && t.test(filter_mode, filter_text, filter_tracker)) {
				row = new TorrentRow(renderer, this, t);
				e = row.getElement();
				e.row = row;
				dirty_rows.push(row);
				$(e).click($.proxy(this.onRowClicked,this));
				$(e).dblclick($.proxy(this.toggleInspector,this));
			}
		}

		// sort the dirty rows
		this.sortRows (dirty_rows);

		// now we have two sorted arrays of rows
		// and can do a simple two-way sorted merge.
		rows = [];
		var ci=0, cmax=clean_rows.length;
		var di=0, dmax=dirty_rows.length;
		frag = document.createDocumentFragment();
		while (ci!=cmax || di!=dmax)
		{
			var push_clean;

			if (ci==cmax)
				push_clean = false;
			else if (di==dmax)
				push_clean = true;
			else {
				var c = Torrent.compareTorrents(
				           clean_rows[ci].getTorrent(),
				           dirty_rows[di].getTorrent(),
				           sort_mode, sort_direction);
				push_clean = (c < 0);
			}

			if (push_clean)
				rows.push(clean_rows[ci++]);
			else {
				row = dirty_rows[di++];
				e = row.getElement();
				if (ci !== cmax)
					list.insertBefore(e, clean_rows[ci].getElement());
				else
					frag.appendChild(e);
				rows.push(row);
			}
		}
		list.appendChild(frag);

		// update our implementation fields
		this._rows = rows;
		this.dirtyTorrents = {};

		// jquery's even/odd starts with 1 not 0, so invert its logic
		e = []
		for (i=0; row=rows[i]; ++i)
			e.push(row.getElement());
		$(e).filter(":odd").addClass('even'); 
		$(e).filter(":even").removeClass('even'); 

		// sync gui
		this.updateStatusbar();
		this.refreshFilterButton();
		if (old_sel_count !== $(list).children('.selected').length)
			this.selectionChanged();
	},

	setFilterMode: function(mode)
	{
		// set the state
		this.setPref(Prefs._FilterMode, mode);

		// refilter
		this.refilter(true);
	},

	refreshFilterPopup: function()
	{
		var i, j, o, s, state, states, counts,
		    sel_state, fragment, div,
		    tor, torrents, name, names,
		    trackers = this.getTrackers(),
		    tr = this;

		/***
		****  States
		***/

		states = [ Prefs._FilterAll,
		           Prefs._FilterActive,
		           Prefs._FilterDownloading,
		           Prefs._FilterSeeding,
		           Prefs._FilterPaused,
		           Prefs._FilterFinished ];

		counts = {};
		for (i=0; state=states[i]; ++i)
			counts[state] = 0;

		torrents = this.getAllTorrents();
		for (i=0; tor=torrents[i]; ++i)
			for (j=0; state=states[j]; ++j)
				if (tor.testState(state))
					counts[state]++;

		sel_state = tr[Prefs._FilterMode];
		fragment = document.createDocumentFragment();
		for (i=0; s=states[i]; ++i)
		{
			div = document.createElement('div');
			div.id = 'show-state-' + s;
			div.className = 'row' + (s === sel_state ? ' selected':'');
			div.innerHTML = '<span class="filter-img"></span>'
			              + '<span class="filter-name">' + tr.getStateString(s) + '</span>'
			              + '<span class="count">' + counts[s].toStringWithCommas() + '</span>';
			$(div).click({'state':s}, function(ev) {
				tr.setFilterMode(ev.data.state);
				$('#filter-popup').dialog('close');
			});
			fragment.appendChild(div);
		}
		$('#filter-by-state .row').remove();
		$('#filter-by-state')[0].appendChild(fragment);

		/***
		****  Trackers
		***/

		names = [];
		for (name in trackers)
			names.push (name);
		names.sort();

		fragment = document.createDocumentFragment();
		div = document.createElement('div');
		div.id = 'show-tracker-all';
		div.className = 'row' + (tr.filterTracker ? '' : ' selected');
		div.innerHTML = '<span class="filter-img"></span>'
		              + '<span class="filter-name">All</span>'
		              + '<span class="count">' + torrents.length.toStringWithCommas() + '</span>';
		$(div).click(function() {
			tr.setFilterTracker(null);
			$('#filter-popup').dialog('close');
		});
		fragment.appendChild(div);
		for (i=0; name=names[i]; ++i) {
			o = trackers[name];
			div = document.createElement('div');
			div.id = 'show-tracker-' + name;
			div.className = 'row' + (o.domain === tr.filterTracker  ? ' selected':'');
			div.innerHTML = '<img class="filter-img" src="http://'+o.domain+'/favicon.ico"/>'
			              + '<span class="filter-name">'+ name + '</span>'
			              + '<span class="count">'+ o.count.toStringWithCommas() + '</span>';
			$(div).click({domain:o.domain}, function(ev) {
				tr.setFilterTracker(ev.data.domain);
				$('#filter-popup').dialog('close');
			});
			fragment.appendChild(div);
		}
		$('#filter-by-tracker .row').remove();
		$('#filter-by-tracker')[0].appendChild(fragment);
	},

	getStateString: function(mode)
	{
		switch (mode)
		{
			case Prefs._FilterActive:      return 'Active';
			case Prefs._FilterSeeding:     return 'Seeding';
			case Prefs._FilterDownloading: return 'Downloading';
			case Prefs._FilterPaused:      return 'Paused';
			case Prefs._FilterFinished:    return 'Finished';
			default:                       return 'All';
		}
	},

	setFilterTracker: function(domain)
	{
		// update which tracker is selected in the popup
		var key = domain ? this.getReadableDomain(domain) : 'all',
		    id = '#show-tracker-' + key;
		$(id).addClass('selected').siblings().removeClass('selected');

		this.filterTracker = domain;
		this.refilter(true);
	},

	// example: "tracker.ubuntu.com" returns "ubuntu.com"
	getDomainName: function(host)
	{
		var dot = host.indexOf('.');
		if (dot !== host.lastIndexOf('.'))
			host = host.slice(dot+1);
		return host;
	},

	// example: "ubuntu.com" returns "Ubuntu"
	getReadableDomain: function(name)
	{
		if (name.length)
			name = name.charAt(0).toUpperCase() + name.slice(1);
		var dot = name.indexOf('.');
		if (dot !== -1)
			name = name.slice(0, dot);
		return name;
	},

	getTrackers: function()
	{
		var ret = {};

		var torrents = this.getAllTorrents();
		for (var i=0, torrent; torrent=torrents[i]; ++i)
		{
			var names = [];
			var trackers = torrent.getTrackers();
			for (var j=0, tracker; tracker=trackers[j]; ++j)
			{
				var uri, announce = tracker.announce;

				if (announce in this.uriCache)
					uri = this.uriCache[announce];
				else {
					uri = this.uriCache[announce] = parseUri (announce);
					uri.domain = this.getDomainName (uri.host);
					uri.name = this.getReadableDomain (uri.domain);
				}

				if (!(uri.name in ret))
					ret[uri.name] = { 'uri': uri,
					                  'domain': uri.domain,
					                  'count': 0 };

				if (names.indexOf(uri.name) === -1)
					names.push(uri.name);
			}
			for (var j=0, name; name=names[j]; ++j)
				ret[name].count++;
		}

		return ret;
	},

	/***
	****
	****  Compact Mode
	****
	***/

	toggleCompactClicked: function()
	{
		this.setCompactMode(!this[Prefs._CompactDisplayState]);
	},
	setCompactMode: function(is_compact)
	{
		var key = Prefs._CompactDisplayState,
		    was_compact = this[key];

		if (was_compact !== is_compact) {
			this.setPref(key, is_compact);
			this.onCompactModeChanged();
		}
	},
	initCompactMode: function()
	{
		this.onCompactModeChanged();
	},
	onCompactModeChanged: function()
	{
		var compact = this[Prefs._CompactDisplayState];

		// update the ui: context menu
		// (disabled in iphone mode...)
		if (!isMobileDevice) {
			var e = $('#settings_menu #compact_view');
			if (compact)
				e.selectMenuItem();
			else
				e.deselectMenuItem();
		}

		// update the ui: footer button
		$("#compact-button").toggleClass('selected',compact);

		// update the ui: torrent list
		this.torrentRenderer = compact ? new TorrentRendererCompact()
		                               : new TorrentRendererFull();
		this.refilter(true);
	},

	/***
	****
	****  Statistics
	****
	***/

	// turn the periodic ajax stats refresh on & off
	togglePeriodicStatsRefresh: function(enabled) {
		clearInterval(this.statsInterval);
		delete this.statsInterval;
		if (enabled) {
			var callback = $.proxy(this.loadDaemonStats,this),
                            msec = 5000;
			this.statsInterval = setInterval(callback, msec);
		}
	},

	loadDaemonStats: function(async) {
		this.remote.loadDaemonStats(function(data) {
			this.updateStats(data['arguments']);
		}, this, async);
	},

	// Process new session stats from the server
	updateStats: function(stats)
	{
		var s, ratio,
		    fmt = Transmission.fmt;

		s = stats["current-stats"];
		ratio = Math.ratio(s.uploadedBytes,s.downloadedBytes);
		$('#stats-session-uploaded').html(fmt.size(s.uploadedBytes));
		$('#stats-session-downloaded').html(fmt.size(s.downloadedBytes));
		$('#stats-session-ratio').html(fmt.ratioString(ratio));
		$('#stats-session-duration').html(fmt.timeInterval(s.secondsActive));

		s = stats["cumulative-stats"];
		ratio = Math.ratio(s.uploadedBytes,s.downloadedBytes);
		$('#stats-total-count').html(s.sessionCount + " times");
		$('#stats-total-uploaded').html(fmt.size(s.uploadedBytes));
		$('#stats-total-downloaded').html(fmt.size(s.downloadedBytes));
		$('#stats-total-ratio').html(fmt.ratioString(ratio));
		$('#stats-total-duration').html(fmt.timeInterval(s.secondsActive));
	},


	showStatsDialog: function() {
		this.loadDaemonStats();
		this.hideMobileAddressbar();
		this.togglePeriodicStatsRefresh(true);
		$('#stats-dialog').dialog({
			close: $.proxy(this.onStatsDialogClosed,this),
			show: 'fade',
			hide: 'fade',
			title: 'Statistics'
		});
	},

	onStatsDialogClosed: function() {
		this.hideMobileAddressbar();
		this.togglePeriodicStatsRefresh(false);
	}
};
