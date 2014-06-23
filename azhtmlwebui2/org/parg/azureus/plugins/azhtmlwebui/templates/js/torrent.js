
function Torrent( torrentManager, hash, index, pos ) {
	this.torrentManager = torrentManager
	this.hash = hash
	this.index = index
	this.pos = pos
	this.selected = false
	this.register( torrentManager )
	this.torrentRowCells = null;
	
}
Torrent.prototype.setTorrentRowCells = function( torrentRowCells ){
	this.torrentRowCells = torrentRowCells;
}
Torrent.prototype.getTorrentRowCells = function() {
	return this.torrentRowCells;
}
Torrent.prototype.setHash = function( hash ) {
	this.hash = hash
}
Torrent.prototype.getHash = function() {
	return this.hash
}
Torrent.prototype.setIndex = function( index ) {
	this.index = index
}
Torrent.prototype.getIndex = function() {
	return this.index
}
Torrent.prototype.setPos = function( pos ) {
	this.pos = pos
}
Torrent.prototype.getPos = function() {
	return this.pos
}
Torrent.prototype.updatePos = function( pos ) {
	if( this.getPos() == pos ) {
		return false
	} else {
		this.setPos( pos )
		return true
	}
}
Torrent.prototype.isSelected = function() {
	return this.selected
}
Torrent.prototype.setSelected = function( selected ) {
	//alert( " is Selected! ")
	this.selected = selected
}
Torrent.prototype.register = function( torrentManager ) {
	//alert('torrent added!')
	torrentManager.add( this )
}
function TorrentManager(){	
	this.test = 'blah'
	this.list = new Array()
	this.length = this.list.length
}
TorrentManager.prototype.getTorrents = function() {
	return this.list
}
TorrentManager.prototype.setTorrents = function( torrents ) {
	this.list = torrents
}
TorrentManager.prototype.getTorrentByHash = function( hash ) {
	var arr = $A( this.list )
	return arr.find( function ( tor ){
			return tor.getHash() == hash
		});
}
TorrentManager.prototype.add = function( torrent ) {
	var arr = this.getTorrents()
	arr.push( torrent )
	this.setTorrents( arr )
}
TorrentManager.prototype.remove = function( torrent ) {
	var arr = this.getTorrents()
	this.setTorrents( arr.without( torrent ) )
}
TorrentManager.prototype.getSelectedTorrents = function() {
	var allTorrents = $A( this.getTorrents() )
	return allTorrents.findAll( function( torrent ){
		return torrent.isSelected()
	});
}
TorrentManager.prototype.getNonSelectedTorrents = function() {
	var allTorrents = $A( this.getTorrents() )
	return allTorrents.findAll( function( torrent ){
		return !torrent.isSelected()
	});
}
TorrentManager.prototype.getTorrentsByPos = function() {
	var allTorrents = $A( this.getTorrents() )
	return allTorrents.sort( function( a, b) {
		return a.getPos() - b.getPos();
	});
}

var tmpl = '<td><input type="checkbox" class="chkbox" id="chk_#{hash_short}" value="#{hash_short}"></td>'
tmpl += '<td>#{position}</td>'
tmpl += '<td><b><a href="index.tmpl?d=d&amp;t=#{hash_short}">#{short_name}</a></b></td>'
//tmpl += '<td>&loz;</td>'
//tmpl += '<td><span><a href="#{torrent_magnet}">&curren;</a></span></td>'
tmpl += '<td>&nbsp;</td>'
tmpl += '<td>&nbsp;</td>'
tmpl += '<td id="c#{hash_short}">#{category}</td>'
tmpl += '<td id="s#{hash_short}" class="status">#{status}</td>'
tmpl += '<td>#{size}</td>'
var tmpl_seed = tmpl
tmpl += '<td>#{downloaded}</td>'
tmpl += '<td>#{uploaded}</td>'
tmpl_seed += '<td>#{uploaded}</td>'
tmpl += '<td>#{percent_done}%</td>'
tmpl += '<td>#{dl_speed}</td>'
tmpl += '<td>#{ul_speed}</td>'
tmpl_seed += '<td>#{ul_speed}</td>'
tmpl += '<td class="seeds">#{seeds} (#{total_seeds})</td>'
tmpl += '<td class="peers">#{peers} (#{total_peers})</td>'
tmpl += '<td id="sr_#{hash_short}">#{share_ratio}</td>'
tmpl_seed += '<td class="seeds">#{seeds} (#{total_seeds})</td>'
tmpl_seed += '<td class="peers">#{peers} (#{total_peers})</td>'
tmpl_seed += '<td id="sr_#{hash_short}">#{share_ratio}</td>'
tmpl += '<td>#{eta}</td>'
tmpl += '<td>#{avail}</td>'
tmpl_seed += '<td>#{avail}</td>'

var dlTorrentTemplate = new Template( tmpl )
var sdTorrentTemplate = new Template( tmpl_seed )

var tmpl_stats = '<td align="center"><br><span class="totals">#{total_download} (#{total_dl}) [#{max_dl_speed}] down<br>' +
            	'#{total_upload} (#{total_up}) [#{max_upload_speed}] up <br>' +
            	'#{total_transferred} / #{total_size} <br>' +
            	'Free space: #{usable_space}</span></td>';

var statsTemplate = new Template( tmpl_stats )
var statsRow = {
	update: function(parent, params){
		debug( 'statsRow.update :: parent: ' + parent + ' :: params: ' + params)
		var txt = ''+params.total_download+' ('+params.total_dl+') ['+params.max_dl_speed+'] down<br>' +
            	''+params.total_upload+' ('+params.total_up+') ['+params.max_upload_speed+'] up <br>' +
            	''+params.total_transferred+' / '+params.total_size+' <br>' +
            	'Free space: '+params.usable_space+'';
		if($('s_stats')){
			debug( 'statsRow.update :: updating cell')
			this.updateCell(txt)
		} else {
			debug( 'statsRow.update :: creating cell')
			this.createCell(parent, txt)
		}
	},
	createCell: function( parent, txt ){
		var cell = document.createElement('td');
		cell.style.textAlign = 'center'
		var span = document.createElement('span');
		Element.addClassName(span,'totals');
		span.id='s_stats';
		span.innerHTML = txt;
		cell.appendChild(span);
		parent.appendChild(cell);
	},
	updateCell: function( txt ){
		debug( 'statsRow.update :: IN updating cell')
		$('s_stats').innerHTML = txt;
	}
}

var torrentRow = {
	
	createCells: function(tr,params,torrent){
		var obj = {};
		
		debug( 'createCells :: torrent hash: ' + params.hash_short + ' :: gets created')
		var cell = document.createElement('td');
		cell.id = 'tdchk_'+params.hash_short;
/*		var inp = document.createElement('input')
		inp.setAttribute('type','checkbox');
		Element.addClassName(inp, 'chkbox');
		inp.setAttribute('id','chk_'+params.short_name);
		inp.setAttribute('value', params.hash_short);
		cell.appendChild(inp);*/
		cell.innerHTML = '<input type="checkbox" class="chkbox" id="chk_'+params.hash_short+'" value="'+params.hash_short+'">';
		tr.appendChild(cell);
		obj.chk = cell;
		
		cell = document.createElement('td');
		cell.id = 'tdpos_'+params.hash_short;
		cell.innerHTML = params.position;
		tr.appendChild(cell);
		obj.pos = cell;
		
		cell = document.createElement('td');
		cell.id = 'tdname_'+params.hash_short;
		cell.innerHTML = '<b><a href="index.tmpl?d=d&amp;t='+params.hash_short+'">'+params.short_name+'</a></b>';
		tr.appendChild(cell);
		obj.name = cell;
		
		cell = document.createElement('td');
		cell.id = 'tdmag_'+params.hash_short;
		cell.innerHTML = '&nbsp;';
		tr.appendChild(cell);
		obj.mag = cell;
		
		cell = document.createElement('td');
		cell.id = 'tdtrac_'+params.hash_short;
		cell.innerHTML = '&nbsp;';
		tr.appendChild(cell);
		obj.trac = cell;
		
		cell = document.createElement('td');
		cell.id = 'c'+params.hash_short;
		cell.innerHTML = params.category;
		tr.appendChild(cell);
		obj.cat = cell;
		
		cell = document.createElement('td');
		cell.id = 's'+params.hash_short;
		cell.innerHTML = params.status;
		Element.addClassName(cell, 'status');
		tr.appendChild(cell);
		obj.status = cell;
		
		cell = document.createElement('td');
		cell.id = 'tdsize_'+params.hash_short;
		cell.innerHTML = params.size;
		tr.appendChild(cell);
		obj.size = cell;
		
		if(params.type=="0"){
		cell = document.createElement('td');
		cell.id = 'tddled_'+params.hash_short;
		cell.innerHTML = params.downloaded;
		tr.appendChild(cell);
		obj.dled = cell;
		}		
		cell = document.createElement('td');
		cell.id = 'tduled_'+params.hash_short;
		cell.innerHTML = params.uploaded;
		tr.appendChild(cell);
		obj.uled = cell;
		
		if(params.type=="0"){
		cell = document.createElement('td');
		cell.id = 'tdpdone_'+params.hash_short;
		cell.innerHTML = params.percent_done;
		tr.appendChild(cell);
		obj.pdone = cell;
		
		cell = document.createElement('td');
		cell.id = 'tddlsp_'+params.hash_short;
		cell.innerHTML = params.dl_speed;
		tr.appendChild(cell);
		obj.dlsp = cell;
		}
		
		cell = document.createElement('td');
		cell.id = 'tdulsp_'+params.hash_short;
		cell.innerHTML = params.ul_speed;
		tr.appendChild(cell);
		obj.ulsp = cell;
		
		cell = document.createElement('td');
		cell.id = 'tdseed_'+params.hash_short;
		cell.innerHTML = params.seeds + ' ('+params.total_seeds+')';
		Element.addClassName(cell, 'seeds');
		tr.appendChild(cell);
		obj.seed = cell;
		
		cell = document.createElement('td');
		cell.id = 'tdpeer_'+params.hash_short;
		cell.innerHTML = params.peers + ' ('+params.total_peers+')';
		Element.addClassName(cell, 'peers');
		tr.appendChild(cell);
		obj.peer = cell;
		
		cell = document.createElement('td');
		cell.id = 'sr_'+params.hash_short;
		cell.innerHTML = params.share_ratio;
		tr.appendChild(cell);
		obj.sr = cell;
		
		if(params.type=="0"){
		cell = document.createElement('td');
		cell.id = 'tdeta_'+params.hash_short;
		cell.innerHTML = params.eta;
		tr.appendChild(cell);
		obj.eta = cell;
		}
		
		cell = document.createElement('td');
		cell.id = 'tdavail_'+params.hash_short;
		cell.innerHTML = params.avail;
		tr.appendChild(cell);
		obj.avail = cell;

		torrent.setTorrentRowCells(obj)
		
		debug( 'createCells :: torrent hash: ' + params.hash_short + ' :: created')
	},
	populateCells: function(params,torrent){
		
		var tRC = torrent.getTorrentRowCells();
		

		(tRC.pos).innerHTML = params.position;

		(tRC.name).innerHTML = '<b><a href="index.tmpl?d=d&amp;t='+params.hash_short+'">'+params.short_name+'</a></b>';

		(tRC.mag).innerHTML = '&nbsp;';

		(tRC.trac).innerHTML = '&nbsp;';

		(tRC.cat).innerHTML = params.category;
		
		(tRC.status).innerHTML = params.status;
		
		(tRC.size).innerHTML = params.size;
		
		if(params.type=="0"){
		(tRC.dled).innerHTML = params.downloaded;
		}		
		(tRC.uled).innerHTML = params.uploaded;
		
		if(params.type=="0"){
		(tRC.pdone).innerHTML = params.percent_done;
		
		(tRC.dlsp).innerHTML = params.dl_speed;
		}
		
		cell = $('tdulsp_'+params.hash_short);
		(tRC.ulsp).innerHTML = params.ul_speed;
		
		cell = $('tdseed_'+params.hash_short);
		(tRC.seed).innerHTML = params.seeds + ' ('+params.total_seeds+')';
		
		cell = $('tdpeer_'+params.hash_short);
		(tRC.peer).innerHTML = params.peers + ' ('+params.total_peers+')';
		
		cell = $('sr_'+params.hash_short);
		(tRC.sr).innerHTML = params.share_ratio;
		
		if(params.type=="0"){
		(tRC.eta).innerHTML = params.eta;
		}
		
		(tRC.avail).innerHTML = params.avail;
		
		debug( 'populateCells :: torrent hash: ' + params.hash_short + ' :: populated')

	}
}