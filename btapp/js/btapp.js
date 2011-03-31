try {
// checking for btapp doesn't work.. the prototype stuff seems to get lost!
//if (!btapp) { 
	function vzWrapAndEval(s) {
		if (s == null) {
			return null;
		}
		return eval('(' + s + ')');
	}
	
	window.btapp =  
	{
		peer_id : 'VuzeConstantPeerID',
		settings : {
			all : function() { return vzWrapAndEval(bt2vuze('settings.all')) }, 
		  keys : function() { return bt2vuze('settings.keys') }, 
		  get : function(key) { return vzWrapAndEval(bt2vuze('settings.get', key)) }, 
		  set : function(key, val) { bt2vuze('settings.set', key, val) }
		},
	
		add : {
			torrent : function(tor) { return vzWrapAndEval(bt2vuze('add.torrent', tor)) }, 
		  rss_feed : function() { return vzWrapAndEval(bt2vuze('add.rss_feed')) }, 
		  rss_filter : function() { return vzWrapAndEval(bt2vuze('add.rss_filter')) } 
		},
	
		events : { 
			set : function(key, func) { bt2vuze('events.set', key, func.toString()) } 
		},
	
		torrent : {
			all : function() { return vzWrapAndEval(bt2vuze('torrent.all')) }, 
		  keys : function() { return bt2vuze('torrent.keys') }, 
		  get : function(key) { return new vzTorrent(key, "btapp.torrent.get"); } 
		},
	
		language : {
			all : function() { return vzWrapAndEval(bt2vuze('language.all')) } 
		},
		
		stash: {
			all : function() { return vzWrapAndEval(bt2vuze('stash.all')); }, 
		  keys : function() { return bt2vuze('stash.keys'); }, 
		  get : function(key) {
		  	var o = bt2vuze('stash.get', key); 
		  	if (o == null) throw "key '" + key + "' not in stash";
		  	return o;
		  }, 
		  set : function(key, val) { bt2vuze('stash.set', key, val) },
		  unset : function(key) { bt2vuze('stash.unset', key) }
		},
		
		log : function(key) { return vzWrapAndEval(bt2vuze('log', key)) }, 
		resource : function(key) { return vzWrapAndEval(bt2vuze('resource', key)) }, 
		sendmsg : function(key, x, y, s) { alert("sendmsg(" + key + ", ..) not supported") }
	};
	
} catch (err) {
	alert("btapp registration error!\n" + err);
}

try {
if (typeof bt2vuze !== "undefined") {
	// The 'peer' variable in vzTorrent
	function vzTorrentVarPeer(torrent, hash) {
		this.hash = hash;
	}
	vzTorrentVarPeer.prototype.get = function(key) { return new vzPeer(this, this.hash, key) };
	vzTorrentVarPeer.prototype.keys = function() { return bt2vuze('torrent.peer.keys', this.hash) };
	vzTorrentVarPeer.prototype.set = function(key, value) { return bt2vuze('torrent.peer.set', this.hash, key, value) };
	vzTorrentVarPeer.prototype.all = function() { return vzWrapAndEval(bt2vuze('torrent.peer.all', this.hash)) } ;
	
	// the 'file' variable in vzTorrent
	function vzTorrentVarFile(torrent, hash, sr) {
		this.hash = hash;
		this.torrent = torrent;
	}
	vzTorrentVarFile.prototype.get = function(key) { return new vzTorrentFile(this, this.hash, key) };
	vzTorrentVarFile.prototype.keys = function() { return bt2vuze('torrent.file.keys', this.hash) };
	vzTorrentVarFile.prototype.all = function() { return vzWrapAndEval(bt2vuze('torrent.file.all', this.hash)) }; 
	
	// the 'properties' variable in vzTorrent
	function vzTorrentVarProperties(hash, sr) {
		this.hash = hash;
	}
	vzTorrentVarProperties.prototype.get = function(id) { return bt2vuze('torrent.properties.get', this.hash, id) };
	vzTorrentVarProperties.prototype.keys = function() { return bt2vuze('torrent.properties.keys', this.hash) };
	vzTorrentVarProperties.prototype.set = function(key, value) { bt2vuze('torrent.properties.set', this.hash, key, value) };
	vzTorrentVarProperties.prototype.all = function() { return vzWrapAndEval(bt2vuze('torrent.properties.all', this.hash)) };
	
	// vzTorrent object returned from btapp.torrent.get/all, vzFile.torrent, vzPeer.torrent
	function vzTorrent(hash, sr) {
		this.hash = hash;
		this.peer = new vzTorrentVarPeer(this, this.hash);
		this.file = new vzTorrentVarFile(this, this.hash);
		this.properties = new vzTorrentVarProperties(this.hash);
		btapp.log('created vzTorrent ' + hash + ' via ' + sr);
	}
	vzTorrent.prototype.recheck = function() { return vzWrapAndEval(bt2vuze('torrent.recheck', this.hash)) };
	vzTorrent.prototype.pause = function() { return vzWrapAndEval(bt2vuze('torrent.pause', this.hash)) };
	vzTorrent.prototype.stop = function() { return vzWrapAndEval(bt2vuze('torrent.stop', this.hash)) };
	vzTorrent.prototype.unpause = function() { return vzWrapAndEval(bt2vuze('torrent.unpause', this.hash)) };
	vzTorrent.prototype.remove = function() { return vzWrapAndEval(bt2vuze('torrent.remove', this.hash)) };
	vzTorrent.prototype.start = function(force) { return vzWrapAndEval(bt2vuze('torrent.start', this.hash)) };
	
	///
	
	// the 'properties' variable in vzPeer
	function vzPeerVarProperties(hash, peerid) {
		this.hash = hash;
		this.id = peerid;
	}
	vzPeerVarProperties.prototype.get = function(key) { return bt2vuze('peer.properties.get', this.hash, this.id, key) };
	vzPeerVarProperties.prototype.keys = function() { return bt2vuze('peer.properties.keys', this.hash, this.id) };
	vzPeerVarProperties.prototype.set = function(key, value) { bt2vuze('peer.properties.set', this.hash, this.id, key, value) };
	vzPeerVarProperties.prototype.all = function() { return vzWrapAndEval(bt2vuze('peer.properties.all', this.hash, this.id)) };
	
	// Peer object returned from vzTorrent.peer.get/all
	function vzPeer(torrent, hash, peerid) {
		this.hash = hash;
		this.id = peerid;
		this.torrent = torrent;
		this.properties = new vzPeerVarProperties(this.hash, this.id);
	}
	vzPeer.prototype.send = function() { return vzWrapAndEval(bt2vuze('torrent.peer.send')) };
	vzPeer.prototype.recv = function() { return vzWrapAndEval(bt2vuze('torrent.peer.recv')) };
	
	///
	
	// the 'properties' variable in vzTorrentFile
	function vzTorrentFileVarProperties(hash, index) {
		this.hash = hash;
		this.index = index;
	}
	vzTorrentFileVarProperties.prototype.get = function(id) { return bt2vuze('torrent.file.properties.get', this.hash, this.index, id) };
	vzTorrentFileVarProperties.prototype.keys = function() { return bt2vuze('torrent.file.properties.keys', this.hash, this.index) };
	vzTorrentFileVarProperties.prototype.set = function(key, value) { bt2vuze('torrent.file.properties.set', this.hash, this.index, key, value) };
	vzTorrentFileVarProperties.prototype.all = function() { return vzWrapAndEval(bt2vuze('torrent.file.properties.all', this.hash, this.index)) };
	
	// File object returned from vzTorrent.file.get/all
	function vzTorrentFile(torrent, hash, index, name) {
		this.hash = hash;
		this.index = index;
		this.name = name;
		this.torrent = torrent;
		this.properties = new vzTorrentFileVarProperties(this.hash, this.index);
	}
	vzTorrentFile.prototype.open = function() { return vzWrapAndEval(bt2vuze('torrent.file.open', this.hash, this.index)) };
	vzTorrentFile.prototype.get_data = function() { return vzWrapAndEval(bt2vuze('torrent.file.get_data', this.hash, this.index)) };
	
	btapp.log("btapp registered");
}
} catch (err) {
	alert("btapp object registration error..\n" + JSON.stringify(err));
}
