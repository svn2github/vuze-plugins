/* torrent */


function Torrent( torrentManager, hash, index, pos ) {
	this.torrentManager = torrentManager
	this.hash = hash
	this.index = index
	this.pos = parseInt( pos )
	this.selected = false
	this.displayed = false
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
	if(typeof pos == 'string'){pos = parseInt(pos)}
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
	this.selected = selected
}
Torrent.prototype.isDisplayed = function() {
	return this.displayed
}
Torrent.prototype.setDisplayed = function( displayed ) {
	this.displayed = displayed
}
Torrent.prototype.register = function( torrentManager ) {
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
TorrentManager.prototype.getDisplayedTorrents = function() {
	var allTorrents = $A( this.getTorrents() )
	return allTorrents.findAll( function( torrent ){
		return torrent.isDisplayed()
	});
}
TorrentManager.prototype.getTorrentsByPos = function() {
	var allTorrents = $A( this.getTorrents() )
	return allTorrents.sort( function( a, b) {
		return a.getPos() - b.getPos();
	});
}
TorrentManager.prototype.getDisplayedTorrentsByPos = function() {
	var allTorrents = $A( this.getDisplayedTorrents() )
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
		/*var cell = document.createElement('td');
		cell.style.textAlign = 'center'
		var span = document.createElement('span');
		Element.addClassName(span,'totals');
		span.id='s_stats';
		span.innerHTML = txt;
		cell.appendChild(span);
		parent.appendChild(cell);*/
		$('s_stats').innerHTML = txt;
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
		cell = $( cell )
		Element.addClassName( cell, "torrent_name" )
		cell.id = 'tdname_'+params.hash_short;
		cell.innerHTML = '<b><a href="index.tmpl?d=d&amp;t='+params.hash_short+'">'+params.name+'</a></b>';
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
		cell.innerHTML = params.percent_done + "%";
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

		(tRC.name).innerHTML = '<b><a href="index.tmpl?d=d&amp;t='+params.hash_short+'">'+params.name+'</a></b>';

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
		(tRC.pdone).innerHTML = params.percent_done + "%";
		
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


/* catmenu */

/***********************************************
* Pop-it menu- � Dynamic Drive (www.dynamicdrive.com)
* This notice MUST stay intact for legal use
* Visit http://www.dynamicdrive.com/ for full source code
***********************************************/

var defaultMenuWidth="120px" //set default menu width.


////No need to edit beyond here

var ie5=document.all && !window.opera
var ns6=document.getElementById

if (ie5||ns6)
document.write('<div id="popitmenu" onMouseover="clearhidemenu();" onMouseout="dynamichide(event)"><\/div>')

function iecompattest(){
	return (document.compatMode && document.compatMode.indexOf("CSS")!=-1)? document.documentElement : document.body
}

/*
 * Show Menu:
 * e: 			Mouse event
 * which: 		hash of the torrent
 * torType:	download or seed
 * type:		torrent or category or remove
 * first:			first torrent?
 * last:			last torrent?
 */

function showmenu(e, which, torType, type, first, last){

	if (!document.all&&!document.getElementById)
	return
	if (first == 'true' && last == 'true' && !tracker_enabled)  return 
	clearhidemenu()
	menuobj=ie5? document.all.popitmenu : document.getElementById("popitmenu")
	menuobj.innerHTML=createMenu(which, torType, type, first, last)
	menuobj.style.width=(typeof optWidth!="undefined")? optWidth : defaultMenuWidth
	menuobj.contentwidth=menuobj.offsetWidth
	menuobj.contentheight=menuobj.offsetHeight
	eventX=ie5? event.clientX : e.clientX
	eventY=ie5? event.clientY : e.clientY
	//Find out how close the mouse is to the corner of the window
	var rightedge=ie5? iecompattest().clientWidth-eventX : window.innerWidth-eventX
	var bottomedge=ie5? iecompattest().clientHeight-eventY : window.innerHeight-eventY
	//if the horizontal distance isn't enough to accomodate the width of the context menu
	if (rightedge<menuobj.contentwidth)
	//move the horizontal position of the menu to the left by it's width
	menuobj.style.left=ie5? iecompattest().scrollLeft+eventX-menuobj.contentwidth+"px" : window.pageXOffset+eventX-menuobj.contentwidth+"px"
	else
	//position the horizontal position of the menu where the mouse was clicked
	menuobj.style.left=ie5? iecompattest().scrollLeft+eventX+"px" : window.pageXOffset+eventX+"px"
	//same concept with the vertical position
	if (bottomedge<menuobj.contentheight)
	menuobj.style.top=ie5? iecompattest().scrollTop+eventY-menuobj.contentheight+"px" : window.pageYOffset+eventY-menuobj.contentheight+"px"
	else
	menuobj.style.top=ie5? iecompattest().scrollTop+event.clientY+"px" : window.pageYOffset+eventY+"px"
	menuobj.style.visibility="visible"
	return false
}

function contains_ns6(a, b) {
	//Determines if 1 element in contained in another- by Brainjar.com
	while (b.parentNode)
	if ((b = b.parentNode) == a)
	return true;
	return false;
}

function hidemenu(){
	if (window.menuobj)
	menuobj.style.visibility="hidden"
}

function dynamichide(e){
	if (ie5&&!menuobj.contains(e.toElement))
	hidemenu()
	else if (ns6&&e.currentTarget!= e.relatedTarget&& !contains_ns6(e.currentTarget, e.relatedTarget))
	hidemenu()
}

function delayhidemenu(){
	delayhide=setTimeout("hidemenu()",500)
}

function clearhidemenu(){
	if (window.delayhide)
	clearTimeout(delayhide)
}

if (ie5||ns6)
document.onclick=hidemenu

function createMenu(index, torType, type, first, last) {
	var res='';
	
	var page2 = "";
	
	if(torType == "0") { // downloads
		if(search_on){
			page = page_url_simple + "?search=" + s_search + "&";
		} else {
			page = page_url_simple + "?";
		}
	} else {						// seeds
		if(search_on){
			page = page_url_simple + "&search=" + s_search + "&";
		} else {
			page = page_url_simple + "&";
		}
	}
	
	if (type == "cat") { // categories menu: assign, create, remove
	
			for (i=1;i<categories.length;i++) {
				//res += '<a href="'+page+'torrent='+hashes[index]+'&setcat='+categories[i]+'">'+unescape(categories[i])+'<\/a>';
				res += '<a href="javascript:setCat( \'' + categories[i] + '\' , \'' + index + '\' );">'+(categories[i])+'<\/a>';
			}

		res += '<hr>';
		res += '<a href="javascript:createCat(\''+index+'\');">'+cmd_cat_new+'</a>';
		res += '<a href="javascript:removeCat(\''+index+'\');">'+cmd_cat_rem+'<\/a>';
		return res;
		
	}
	 
	if (type == "tor") {	// torrents menu: move up, down
		
		if (first != 'true') 	res += '<a href="'+page+'mup='+hashes[index]+'">'+cmd_moveup+'<\/a>';
		if (last != 'true') 		res += '<a href="'+page+'mdn='+hashes[index]+'">'+cmd_movedn+'<\/a>';
		if (first != 'true') 	res += '<a href="'+page+'mtop='+hashes[index]+'">'+cmd_movetop+'<\/a>';
		if (last != 'true') 		res += '<a href="'+page+'mbot='+hashes[index]+'">'+cmd_movebtm+'<\/a>';
		
		if (tracker_enabled) {
			if (first != 'true' || last != 'true') res+='<hr>';
			res+= '<a href="'+page+'pub='+hashes[index]+'">'+cmd_pub+'<\/a>';
			res+= '<a href="'+page+'host='+hashes[index]+'">'+cmd_host+'<\/a>';
		}
		
		return res;
	}
	
	if ( type == "rem" ) {	// remove and delete menu

		res += '<a href="'+page+'rem='+hashes[index]+'&del=0">'+cmd_rem+'<\/a>';
		res+='<hr>';
		res += '<a href="'+page+'rem='+hashes[index]+'&del=1">'+cmd_del1+'<\/a>';
		res += '<a href="'+page+'rem='+hashes[index]+'&del=2">'+cmd_del2+'<\/a>';
		res += '<a href="'+page+'rem='+hashes[index]+'&del=3">'+cmd_del3+'<\/a>';
		return res;
	}
	
		if ( type == "trm" ) {	// remove from tracker  menu
		
			res+= '<a href="'+page+'trm='+hashes[index]+'">'+cmd_track_rem+'<\/a>';
			return res;
		}
	
}

function setCat(cat, index) {
	//alert("inside set cat: " + cat)
	createCatReq( cat, "set", index ) ;
}

function createCat(index) {
	var cat	= prompt("Create category");
	
	if (cat != null)	{
				//window.location.replace(url+'torrent='+hash+'&newcat='+cat);
				createCatReq( cat , "new" , index);
	}
}
function removeCat(index) {
	var cat_list = "";
	for (i=1;i<categories.length;i++) {
		if (categories[i]!="Uncategorized") {
		cat_list += unescape(categories[i]) + "  ";
		}
	}
	var cat	= prompt("Remove category\n\n" + cat_list);
	if (cat != null)	{
				//window.location.replace(url+'remcat='+cat);
				createCatReq( cat , "rem" , index);
	}
}

function createCatReq( cat, cmd , index){
	debug2('createCatReq() :: cat: ' + cat + ' / cmd: ' + cmd)
	//alert("hash= " + index );
	
	var hash = ( hashes[index] ) ? hashes[index] : index; 
	
	//$('test').innerHTML += " //hash=" + hash+ "//";

	new Ajax.Request(
		//'c' + index,
		'index.ajax',
		{
			method: 'get',
			parameters: {cat: cat, cmd: cmd , hash: hash , date:  new Date().getTime()},
			onFailure: function(transport){
				alert("Problem: " + transport.statusText)
			},
			onSuccess: function(transport){
				var id = 'c' + hash;
				resp = transport.responseText
				//alert(resp + " / " + decodeURIComponent(resp))
	  			$( id ).innerHTML = decodeURIComponent( resp ).escapeHTML()
			}
		});
		
		if (cmd == "new" || cmd == "rem") {
			updateCatList()
		}
}
function createElt( parent, type, value, inner ) {
	debug2('createElt() :: type: ' + type + ' / value: ' + value + ' / inner: ' + inner)
	elt = document.createElement( type )
	elt = $( elt )
	elt.setAttribute('value', value.strip())
	elt.innerHTML = inner.strip()
	parent.appendChild( elt )
}
var categories = new Array();
var catSelect;

function updateCatList() {
	debug2('updateCatList() ::')
	new Ajax.Request(
		//'c' + index,
		'index.ajax',
		{
			method: 'get',
			parameters: {cat: "cat", cmd: "menu", date:  new Date().getTime()},
			onFailure: function(transport){
				alert("Problem: " + transport.statusText)
			},
			onSuccess: function(transport){
				debug2(transport.responseText)
				//resp = transport.responseXML
				var resp = transport.responseXML
		  		categories.clear();
		  		debug2("updateCatList() :: " + resp)
		  		var nodes = resp.getElementsByTagName('cat');
		  		//alert(nodes.length)
		  		var res1 = ''
		  		
				catSelect.update('')
				
		  		createElt(catSelect, 'option', '-1', 'Assign category')
				createElt(catSelect, 'option', '--', '--')
		  		
		  		var name,value;
		  		for(j = 0; j<nodes.length ; j++ ) {
		  			if( (nodes[j].getElementsByTagName('value')).length == 0){
		  				name = nodes[j].getElementsByTagName('name')[0].firstChild.nodeValue;
		  				debug2(name)
		  				categories[j] = name
		  				createElt(catSelect, 'option', name, name)
		  			}
		  		}
		  		createElt(catSelect, 'option', '--', '--')
		  		for(j = 0; j<nodes.length ; j++ ) {
		  			if( (nodes[j].getElementsByTagName('value')).length > 0){
		  				name = nodes[j].getElementsByTagName('name')[0].firstChild.nodeValue;
		  				value = nodes[j].getElementsByTagName('value')[0].firstChild.nodeValue;
		  				createElt(catSelect, 'option', value, name)
		  			}
		  		}
		  		//Event.observe(catSelect, 'change', function(){ assignCategory( this.value ); this.selectedIndex = 0 } )
		  		
			}
		});
}

function updateSelectedTorrentList( hash, state ) {
	//alert( "state: " + state + ' / index: ' + index + ' / hash: ' + hashes[index]);
	var tor = torrentManager.getTorrentByHash( hash )
	//alert( tor.getHash() )
	var sel = !tor.isSelected();
	if(state!=null) { sel = state }
	tor.setSelected( sel )
	//updatePossibleActions()
}

function assignCategory( cat ){
	//alert("assigning cat: " + cat) 
	var selectedTorrents = torrentManager.getSelectedTorrents();
	//alert( "Number of torrents selected: " + selectedTorrents.length );
	if( cat == "new" ) {
			var cat1	= prompt("Create category");
			if (cat1 != null)	{
				
				selectedTorrents.each( function( torrent ){
					createCatReq( cat1 , "new" , torrent.getHash() );
				});
			}
	} else if( cat == "rem" ) {	
			var cat_list = "";
			for (i=1;i<categories.length;i++) {
				if (categories[i]!="Uncategorized") {
					cat_list += unescape(categories[i]) + "  ";
				}
			}
			var cat1	= prompt("Remove category\n\n" + cat_list);
			if (cat1 != null)	{
				
				selectedTorrents.each( function( torrent ){
					createCatReq( cat1 , "rem" , torrent.getHash() );
				});
			}
	} else if( cat != "--" ) {	
			selectedTorrents.each( function( torrent ){
				//alert( "hash: " + torrent.getHash() )
				setCat(cat, torrent.getHash() ) 
			});
	}
}

function selectTorrents( bool ) {
	var elts = document.getElementsByClassName( 'chkbox' );
	elts = $A( elts );
	elts.each( function ( elt ){
//		(torrentManager.getTorrentByHash( elt.value )).setSelected( ! bool );
		updateSelectedTorrentList( elt.value, bool )
		updateSelectedCheckbox( elt.value )
	})
}

function removeAction( type ) {
	if( isNaN( type ) ) return;
	
	var selectedTorrents = torrentManager.getSelectedTorrents();
	var isNotLast = selectedTorrents.size();
	var index = 1;
	selectedTorrents.each( function( torrent ){
		isNotLast -= index;
		index++; 
		removeTorrent( type, torrent.getHash(), torrent.getIndex(), isNotLast ) 
	});	
}
function removeTorrent( type, hash, index, notlast) {
	new Ajax.Request(
		'index.ajax',
		{
			method: 'get',
			parameters: {del: type, rem: hash , date:  new Date().getTime()},
			onFailure: function(transport){
				alert("Problem: " + transport.statusText)
			},
			onSuccess: function(transport){
				var tr_id = 'r' + hash;
				resp = transport.responseText
				//alert( "remove: " + resp )
				if(resp == "ok") {
					disappearTorrent( tr_id, hash, notlast )
				}
			}
		});
}
function disappearTorrent( id, hash, notlast ) {
	debug2( 'disappear: ' + hash)
	//if( !notlast ) { notlast = false }
	if($( id )) new Effect.Pulsate( id, {duration: 0.5, pulses: 1, afterFinish: function() { if($(id)) $( id ).remove() } } );
	var torrent = torrentManager.getTorrentByHash( hash )
	torrentManager.remove( torrent );
	if(notlast != null && !notlast)  {
		//updateDisplay()
		updateClassNames()
	}
}
function takeAction( type ) {
	if( isNaN( type ) ) return;
	
	var selectedTorrents = torrentManager.getSelectedTorrents();
	var isNotLast = selectedTorrents.size();
	var index = 1;
	
	selectedTorrents.each( function( torrent ){
		isNotLast -= index;
		index++;
		actionTorrent( type, torrent, isNotLast ) 
	});	
}
function actionTorrent( type, torrent, notlast ) {
	var hash = torrent.getHash()
	var index = torrent.getIndex()
	new Ajax.Request(
		'index.ajax',
		{
			method: 'get',
			parameters: {act: type, hash: hash , date:  new Date().getTime()},
			onFailure: function(transport){
				alert("Problem: " + transport.statusText)
			},
			onSuccess: function(transport){
				var tr_id = 'r' + hash;
				var el = $( tr_id ).getElementsByClassName( 'status' );
				resp = transport.responseText
				el[0].update( resp )
				
				//var interval = 1000 + Math.random() * 2000;
				//setTimeout( "updateTorrentTR('"+ hash +"' , '"+ state +"')" , interval );
				var h = hash.split(' ');
				updateTorrentTR( h , state , notlast );
				updateStats( state );
			}
		});
}

function updateTorrentTR( hashes , state , notlast ) {
	debug("UpdateTorrentTR :: entering updateTorrentTR(" + hashes +" , " + state + ")")
	//var torrent = torrentManager.getTorrentByHash( hash )
        if(hashes == "") return
	new Ajax.Request(
		'index.ajax',
		{
			method: 'get',
			parameters: {act: "update", hash: hashes , st: state , date:  new Date().getTime()},
			onFailure: function(transport){
				debug("UpdateTorrentTR :: Problem: " + transport.statusText)
			},
			onSuccess: function(transport, json){
				
				//$('test').innerHTML += transport.responseText + " / ";
				
				var resp = transport.responseText
				
				debug( 'UpdateTorrentTR :: response.size:' + resp.size )
				
				var jason = (json) ? json : eval( '(' + ( resp ) + ')' )
				
				var resTorrents = jason.torrents;
				
				//var resHashes = jason.hashes;
				hashes = hashes.substring(0,hashes.length-1)
				var resHashes = hashes.split(",");
				resHashes = $A( resHashes )
				debug("UpdateTorrentTR :: Hashes : " + hashes);
				debug("UpdateTorrentTR :: resHashes : " + resHashes.compact())
				resHashes = resHashes.compact();//.reverse()
				for(var k=0;k<jason.size;k++){
					
					var torrent = torrentManager.getTorrentByHash( resHashes[k] )
					var tr_id = 'r' + resHashes[k];
					
					debug( 'UpdateTorrentTR :: torrent hash ' + resHashes[k] + ' :: exists already?: ' + $(tr_id))
					
				
					if(debugOn) debug("previous state: " + state + " / new state: " + jason.type + " :: " + ((jason.type != state)?"different state":"same state"))
					
					if(resTorrents[resHashes[k]].index && resTorrents[resHashes[k]].type == state) {
						debug( 'UpdateTorrentTR :: torrent hash: ' + resHashes[k] + ' :: gets populated')
						if( ! $(tr_id) ) {
							createTorrentTR( torrent )
							torrentRow.createCells($(tr_id), resTorrents[resHashes[k]], torrent )
							updateClassNames()
						} else {
							torrentRow.populateCells( resTorrents[resHashes[k]], torrent )
						}

						var moved = torrent.updatePos( resTorrents[resHashes[k]].position )
						if( moved ) updateClassNames()

						if(resTorrents[resHashes[k]].isFP == 1) $('sr_'+resHashes[k]).style.color='#B02B2C'
						debug( 'UpdateTorrentTR :: updating selected chkbox')
						
						updateSelectedCheckbox( resHashes[k] )
						debug( 'UpdateTorrentTR :: tr innerHTML: ' + $(tr_id).innerHTML)
					} else {
						debug( 'UpdateTorrentTR :: torrent hash: ' + resHashes[k] + ' :: disappears')
						disappearTorrent( tr_id , resHashes[k], notlast)
					}
				}
			}
		});
}

var tr_stats = 'stats'

var totalPages = 1; 
var selectedPage = 1;

new PeriodicalExecuter(updateDisplay, 15);
new PeriodicalExecuter(updateTL, 30)

function updateStats( state ) {
/**
 * Fetch upload/download stats, speeds, # torrents in each category
 */
 		
 		debug( "STATS :: ++++++++ updateStats ++++++++  ");
 		
 		new Ajax.Request(
		'index.ajax',
		{
			method: 'get',
			parameters: {act: "stats", st: state , date:  new Date().getTime()},
			onFailure: function(transport){
				debug("STATS :: Problem: " + transport.statusText)
			},
			onSuccess: function(transport, json){
				var resp = transport.responseText
				var jason = (json) ? json : eval( '(' + ( resp ) + ')' )
				//var inside = statsTemplate.evaluate(jason)
				//$(tr_stats).innerHTML = inside
				statsRow.update($(tr_stats), jason)
				$('dls').innerHTML = jason.count_0;
				$('cds').innerHTML = jason.count_1;
				if(jason.count_2) $('cos').innerHTML = jason.count_2;
				debug("STATS :: stats jason: " + jason.count_1)
				
			}
		});
 
}
function updateTL( callback ) {
	debug("UpdateTL :: updating torrent list from server!")
	//$('test').innerHTML += " --updateTL focker-- "
	updateTorrentList( state, callback )
	updatePagination()
	updateStats( state )
}
var torrentsFromServer = new Array();
var torrentsDisplayed = new Array();
var torrentsToRemove = new Array();
var torrentsToAdd = new Array();
	
function updateTorrentList( state, callback ) {
/*
 * Fetch active torrents in state
 * state: downloading:0, seeding:1, complete:2
 */
 
 	debug( "UpdateTorrentList :: --updateTorrentList-- " );
 	debug( "UpdateTorrentList :: callback: " + callback );
 	
 	new Ajax.Request(
		'index.ajax',
		{
			method: 'get',
			parameters: {update: "hashes", st: state, page: selectedPage, date:  new Date().getTime()},
			onFailure: function(transport){
				debug("UpdateTorrentList :: Problem: " + transport.statusText)
			},
			onSuccess: function(transport, json){
				var resp = transport.responseText
				var jason = (json) ? json : eval( '(' + ( resp ) + ')' )
				
				debug('UpdateTorrentList :: hashes:' + resp)
				debug('UpdateTorrentList :: jason size: ' + jason.size)
				
				//if(jason.size > MAX_TORRENTS_PER_PAGE){
					//setNumberOfTorrentsPerPage()
					//return
				//}
				
				var torrents = torrentManager.getTorrents()
				torrents = $A( torrents )
//
//				torrentsDisplayed.clear();
//
//				torrents.each( function( torrent ) {
//					torrentsDisplayed.push( torrent.getHash() )
//				})

                                torrentsDisplayed = torrents.invoke( 'getHash' )
				
				debug('UpdateTorrentList :: torrentManager size: ' + torrents.length)
				
				torrentsFromServer.clear()
				torrentsToRemove.clear()
				
				for(var j=0; j< jason.size; j++){
					torrentsFromServer.push( jason[j].hash )
				}
				
				torrentsToRemove = torrentsDisplayed.findAll( function( torrent ) {
					return (torrentsFromServer.indexOf( torrent ) < 0 )
				})
				debug2( 'torrentsToRemove.size(): ' + torrentsToRemove.size() )
				torrentsToRemove.each( function( hash ) {
					var id = "r" + hash;
					debug('UpdateTorrentList :: torrent ' + hash + ' NOT in list: removing')
					if($(id)) disappearTorrent(id, hash)
				})
				
				var needUpdateClasses = torrentsToRemove.size() != 0 && ( torrentsToRemove.size() != torrentsDisplayed.size() );
				debug2( 'Disappear : needUpdateClasses: ' + needUpdateClasses )
				if( needUpdateClasses ) updateClassNames()
				
				debug2('UpdateTorrentList :: begin callback: ' + callback + ' / typeof: ' + typeof callback)
				if(callback!=null && typeof callback == 'function') { window.setTimeout(function(){callback()},1000); }
				debug('UpdateTorrentList :: end callback')
				
				var h = '';
				
				//alert(jason.length + " : " + jason.size + " : " + jason.count)
				
				torrentsToAdd.clear()
				
				torrentsToAdd = torrentsFromServer.findAll( function( torrent ){
					return torrentsDisplayed.indexOf( torrent ) < 0
				})
				
				for(var i=0; i< jason.size; i++) {
					debug('UpdateTorrentList :: jason['+i+'].hash= ' + jason[i].hash)
					if( ! torrentManager.getTorrentByHash( jason[i].hash ) ) {
						/*
						 * add torrent
						 */
						 debug('UpdateTorrentList :: adding torrent with hash: ' + jason[i].hash)
						var tempTorrent = new Torrent( torrentManager, jason[i].hash, jason[i].index, jason[i].pos )
						h += tempTorrent.getHash().split(" ") + ',';
					}
				}
				
				if(torrentsToAdd.size() > 0){
					needUpdateClasses = torrentsToAdd.size() != torrentsFromServer.size()
					debug2( 'Appear : needUpdateClasses: ' + needUpdateClasses )
					debug2( 'hashes: ' + h)
					debug('UpdateTorrentList :: calling updateTorrentTR: ' + h)
					if(torrentsToRemove.size() > 0) {
						window.setTimeout( "updateTorrentTR('" + h + "'," + state + "," + !needUpdateClasses + " )" , 1000 )
					} else {
						updateTorrentTR( h, state, !needUpdateClasses );
					}
				}
				
			}
		});

}

function createTorrentTR( torrent ) {
	debug('CreateTorrentTR :: creating TR_' + torrent.getHash() )
	
	var hash = torrent.getHash();
	var table = $('t1')
	debug('CreateTorrentTR :: table: ' + table)
	var tbodies = table.getElementsByTagName('tbody')
	var tbody = tbodies[0]
	debug('CreateTorrentTR :: creating row')
	
	var pos = torrent.getPos()
	
	var beforeTorrents = (torrentManager.getDisplayedTorrents()).findAll(function(torrent){
		return torrent.getPos() < pos;
	})
	
	var row = tbody.insertRow( beforeTorrents.size() + 1 );
	//var row = document.createElement('tr')
	row.id='r' + hash
	//tbody.appendChild( row )
	debug('CreateTorrentTR :: adding row to table')
	
	torrent.setDisplayed( true )
	
	row = $( row.id )
	
	var class_name = 'trtorrent' + ( ( ( table.rows.length - 1) % 2 ) ? '' : '_odd' );
	row.classNames().set( class_name )
   	Event.observe(row, 'mouseover', function() { Element.addClassName(row,'trtorrent_hover'); } )
   	Event.observe(row, 'mouseout', function() { Element.removeClassName(row,'trtorrent_hover'); } )
   	Event.observe(row, 'click', function() { updateSelectedTorrentList( hash ); updateSelectedCheckbox( hash ) });
   	
}

function updateDisplay() {
	debug('updating display')
	updateTorrentTR( $A( torrentManager.getTorrents() ).invoke('getHash').join(",") , state);

}
function updateSelectedCheckbox( hash, state ) {
	//alert( "state: " + state + ' / index: ' + index + ' / hash: ' + hashes[index]);
	var cb = $( 'chk_' + hash )
	if(cb){
		var torrent = torrentManager.getTorrentByHash( hash )
		var sel = torrent.isSelected()
		cb.checked = sel ? true : false;
		
		/* set background color for rows of selected torrents */ 
		if(sel) {
			$('r'+hash).addClassName('selected');
		} else {
			$('r'+hash).removeClassName('selected');
		}
	}
}

function updateClassNames( ) {
	debug2('updateClassNames!')
	var torrents = torrentManager.getDisplayedTorrentsByPos();
	for( var i=0; i < torrents.size() ; i++) {
		var class_name = 'trtorrent' + ( ( i % 2 ) ? '' : '_odd' );
		debug2("new class: " + class_name)
		var row = $( 'r' + torrents[i].getHash() )
		debug2("had that class already: " + row.hasClassName( class_name ))
		if( !row.hasClassName( class_name ) ) row.classNames().add( class_name )
		row.classNames().set( class_name )   	
	}
}
var globalHandlers = {
		onCreate: function(){
			Element.show('ajax');
		},
	
		onComplete: function() {
			if(Ajax.activeRequestCount == 0){
				Element.hide('ajax');
			}
		}
	};

Ajax.Responders.register(globalHandlers);

var ST_DOWNLOADING = 0;
var ST_SEEDING = 1;
var ST_COMPLETED = 2;
var ST_OPTIONS = 3;

var tabArray = new Array();
tabArray[ST_DOWNLOADING] = 'tab_down';
tabArray[ST_SEEDING] = 'tab_seed';
tabArray[ST_COMPLETED] = 'tab_comp';
//tabArray[ST_OPTIONS] = 'tab_options';

tabArray = $A( tabArray )

function setState( _state, params ) {
	
	var forceUpdate = false || (params != null && params.force != null && params.force)
	
	if(_state != state  || forceUpdate){
		
		tabArray.each( function(tabId){
			if($(tabId)) $(tabId).removeClassName('tab_selected')
		})
		$( tabArray[_state] ).addClassName('tab_selected')
		
		debug('old state: ' + state);
		state = _state
		debug('new state: ' + state);
		updateTL( updateTorrentTableHeader );

	}
}

function updateTorrentTableHeader() {
	$$('th.d').invoke( state == ST_DOWNLOADING ? 'show' : 'hide' )
}

function displayOptions( ) {
	var tabId = 'tab_options'
	debug('content visible?: ' + $('content').visible())
	if(!$('options').visible()) {
		Effect.Fade('content',{from:0.9999, to:0.3, duration:0.15, afterFinish: function(){Effect.Appear('options',{duration:0.15})}})
	} else {
		Effect.Fade('options',{ duration:0.15, afterFinish: function(){Element.hide('msg');Effect.Appear('content',{from:0.3, to:0.9999, duration:0.15})}})
	}
	debug('options tab selected? ' + $( tabId ).hasClassName('tab_selected') )
	if($( tabId ).hasClassName('tab_selected')) {
		$(tabId).removeClassName('tab_selected')
	} else {
		$(tabId).addClassName('tab_selected')
	}
}
/*
function constructNavBar(){
	new Ajax.Request(
		'index.ajax', 
		{
			method: 'get',
			parameters: {act: 'nav',date: new Date().getTime()},
			onFailure: function(transport){
			},
			onSuccess: function(transport){
				var resp = transport.responseText
				for(var i=0;i<=resp;i++) {
					createNavItem(i)
				}
			}
		}
	);
}
function createNavItem(_stateNb){
	var navUL = $('tabs');
	var navLI = document.createElement('li');
	li.id = tabArray[_stateNb]
	navUL.appendChild(navLI);
}
*/

function gotoPage( page ){
	selectedPage = page;
	updateTL();
}

function updatePagination(){
	new Ajax.Request(
		'index.ajax',
		{
			method: "GET",
			parameters: {update: "pagination", st: state, date:  new Date().getTime()},
			onFailure: function(transport){
				debug("updatePagination :: Problem: " + transport.statusText)
			},
			onSuccess: function(transport){
				totalPages = transport.responseText;
				if(totalPages > 1) {
					var str="<select onchange='gotoPage(this.value)'>";
					if(totalPages < selectedPage) selectedPage = 1;
					$R(1,totalPages).each(function(k){
						str +="<option value=\""+k+"\"" + ((k==selectedPage)?"selected":"") + ">"+k+"</option>";
					});
					str += "</select>";
					$('pagin').update( str )
				} else {
					$('pagin').update( "" )
				}
			}
			
		});
}

var debugOn = false;
var showDebug = false;

function debug( msg ) {}

function debug2( txt ) {
	if(showDebug){
		$('debug').innerHTML += ' <br/> ' + (new Date()).getTime() + ':: ' + txt
		$('debug').scrollTop = $('debug').scrollHeight
	}
}

/* sorttable */

var SORT_COLUMN_INDEX;

function sortables_init() {
    // Find all tables with class sortable and make them sortable
    if (!document.getElementsByTagName) return;
    tbls = document.getElementsByTagName("table");
    for (ti=0;ti<tbls.length;ti++) {
        thisTbl = tbls[ti];
        if (((' '+thisTbl.className+' ').indexOf("azcontent") != -1) && (thisTbl.id) && (' '+thisTbl.className+' ').indexOf("azcontent_") == -1 ) {
            //initTable(thisTbl.id);
            ts_makeSortable(thisTbl);
        }
    }
}

function ts_makeSortable(table) {
    if (table.rows && table.rows.length > 0) {
        var firstRow = table.rows[0];
    }
    if (!firstRow) return;
    
    // We have a first row: assume it's the header, and make its contents clickable links
    for (var i=0;i<firstRow.cells.length;i++) {
        var cell = firstRow.cells[i];
        var txt = ts_getInnerText(cell);
        if ( txt == "#" ) {
        	cell.innerHTML = '<a href="#" class="sortheader" onclick="ts_resortTable(this);return false;">'+txt+'<span class="sortarrow">&nbsp;<img src="images/up.gif" border="0"  width="8" height="8"></span></a>';
//        	cell.innerHTML = '<a href="#" class="sortheader" onclick="ts_resortTable(this);return false;">'+txt+'<span class="sortarrow">&nbsp;&uarr;</span></a>';
        } else {
        	if ( txt != "More" ) cell.innerHTML = '<a href="#" class="sortheader" onclick="ts_resortTable(this);return false;">'+txt+'<span class="sortarrow"></span></a>';
        }
    }
}

function ts_getInnerText(el) {
	if (typeof el == "string") return el;
	if (typeof el == "undefined") { return el };
	if (el.innerText) return el.innerText;	//Not needed but it is faster
	var str = "";
	
	var cs = el.childNodes;
	var l = cs.length;
	for (var i = 0; i < l; i++) {
		switch (cs[i].nodeType) {
			case 1: //ELEMENT_NODE
				str += ts_getInnerText(cs[i]);
				break;
			case 3:	//TEXT_NODE
				str += cs[i].nodeValue;
				break;
		}
	}
	return str;
}

function ts_resortTable(lnk) {
    // get the span
    var span;
    for (var ci=0;ci<lnk.childNodes.length;ci++) {
        if (lnk.childNodes[ci].tagName && lnk.childNodes[ci].tagName.toLowerCase() == 'span') span = lnk.childNodes[ci];
    }
    var spantext = ts_getInnerText(span);
    var td = lnk.parentNode;
    var column = td.cellIndex;
    var table = getParent(td,'TABLE');
    
    // Work out a type for the column
    if (table.rows.length <= 1) return;
    var itm = ts_getInnerText(table.rows[1].cells[column]);
    sortfn = ts_sort_caseinsensitive;
    if (itm.match(/^\d\d[\/-]\d\d[\/-]\d\d\d\d$/)) sortfn = ts_sort_date;
    if (itm.match(/^\d\d[\/-]\d\d[\/-]\d\d$/)) sortfn = ts_sort_date;
    if (itm.match(/^[?$]/)) sortfn = ts_sort_currency;
    if (itm.match(/^[\d\.]+$/)) sortfn = ts_sort_numeric;
    if (itm.match(/^[\d\.]+( | k| M| G| T)B$/)) sortfn = ts_sort_size;
    if (itm.match(/^[\d\.]+( | k| M| G| T)B\/s$/)) sortfn = ts_sort_speed;
    if (itm.match(/[\d\.]+%/) || itm.match(/N\/A/)) sortfn = ts_sort_percent;
    
    SORT_COLUMN_INDEX = column;
    var firstRow = new Array();
    var newRows = new Array();
    var oldRows = new Array();
    for (i=0;i<table.rows[0].length;i++) { firstRow[i] = table.rows[0][i]; }
    for (j=1;j<table.rows.length;j++) { newRows[j-1] = table.rows[j]; }

    oldRows = newRows;
    newRows.sort(sortfn);
    
    if (span.getAttribute("sortdir") == 'up') {
        ARROW = '&nbsp;<img src="images/up.gif" border="0" width="8" height="8">';
//        ARROW = '&nbsp;&uarr;';
        span.setAttribute('sortdir','down');
    } else {
       ARROW = '&nbsp;<img src="images/down.gif" border="0" width="8" height="8">';
//        ARROW = '&nbsp;&darr;';
        newRows.reverse();
        span.setAttribute('sortdir','up');
    }
    
    // We appendChild rows that already exist to the tbody, so it moves them rather than creating new ones
    // don't do sortbottom rows
    for (i=0;i<newRows.length;i++) { 
    	if (!newRows[i].className || (newRows[i].className && (newRows[i].className.indexOf('sortbottom') == -1))) {	
    		table.tBodies[0].appendChild(newRows[i]);
    	}
    }
    // do sortbottom rows only
    for (i=0;i<newRows.length;i++) { 
    	if (newRows[i].className && (newRows[i].className.indexOf('sortbottom') != -1)) {
    		table.tBodies[0].appendChild(newRows[i]);
    	}
    }
    
    for (i=0;i<newRows.length;i++) { 
    		if (i % 2) {
    		
    			newRows[i].setAttribute( 'onMouseOver', "className='trtorrent_hover'" );
    			newRows[i].onmouseover = function() { this.className='trtorrent_hover'; }	// IE

    			if (oldRows[i].className == 'trtorrent_active' || oldRows[i].className == 'trtorrent_odd_active') {
    				newRows[i].setAttribute('class', 'trtorrent_active');
    				newRows[i].setAttribute('className', 'trtorrent_active');			// IE
    			
    				newRows[i].setAttribute( 'onMouseOut', "className='trtorrent_active'" );
    				newRows[i].onmouseout = function() { this.className='trtorrent_active'; }			// IE
    			} else {
    				newRows[i].setAttribute( 'class', 'trtorrent' );
    				newRows[i].setAttribute( 'className', 'trtorrent' );						// IE
    			
    				newRows[i].setAttribute( 'onMouseOut', "className='trtorrent'" );
    				newRows[i].onmouseout = function() { this.className='trtorrent'; }			// IE
    			}
    			
    		} else {
    			
    			newRows[i].setAttribute( 'onMouseOver', "className='trtorrent_hover'" );
    			newRows[i].onmouseover = function() { this.className='trtorrent_hover'; }	// IE

	    		if (oldRows[i].className == 'trtorrent_active' || oldRows[i].className == 'trtorrent_odd_active') {
	    			newRows[i].setAttribute('class', 'trtorrent_odd_active');
	    			newRows[i].setAttribute('className', 'trtorrent_odd_active');
    			
    				newRows[i].setAttribute( 'onMouseOut', "className='trtorrent_odd_active'" );
    				newRows[i].onmouseout = function() { this.className='trtorrent_odd_active'; }			// IE
	    		} else {
	    			newRows[i].setAttribute( 'class', 'trtorrent_odd' );
	    			newRows[i].setAttribute( 'className', 'trtorrent_odd' );
    			
    				newRows[i].setAttribute( 'onMouseOut', "className='trtorrent_odd'" );
    				newRows[i].onmouseout = function() { this.className='trtorrent_odd'; }			// IE
	    		}
    		}
    }
    
    // Delete any other arrows there may be showing
    var allspans = document.getElementsByTagName("span");
    for (var ci=0;ci<allspans.length;ci++) {
        if (allspans[ci].className == 'sortarrow') {
            if (getParent(allspans[ci],"table") == getParent(lnk,"table")) { // in the same table as us?
                allspans[ci].innerHTML = '';
            }
        }
    }
        
    span.innerHTML = ARROW;
}

function getParent(el, pTagName) {
	if (el == null) return null;
	else if (el.nodeType == 1 && el.tagName.toLowerCase() == pTagName.toLowerCase())	// Gecko bug, supposed to be uppercase
		return el;
	else
		return getParent(el.parentNode, pTagName);
}
function ts_sort_date(a,b) {
    // y2k notes: two digit years less than 50 are treated as 20XX, greater than 50 are treated as 19XX
    aa = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]);
    bb = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]);
    if (aa.length == 10) {
        dt1 = aa.substr(6,4)+aa.substr(3,2)+aa.substr(0,2);
    } else {
        yr = aa.substr(6,2);
        if (parseInt(yr) < 50) { yr = '20'+yr; } else { yr = '19'+yr; }
        dt1 = yr+aa.substr(3,2)+aa.substr(0,2);
    }
    if (bb.length == 10) {
        dt2 = bb.substr(6,4)+bb.substr(3,2)+bb.substr(0,2);
    } else {
        yr = bb.substr(6,2);
        if (parseInt(yr) < 50) { yr = '20'+yr; } else { yr = '19'+yr; }
        dt2 = yr+bb.substr(3,2)+bb.substr(0,2);
    }
    if (dt1==dt2) return 0;
    if (dt1<dt2) return -1;
    return 1;
}

function ts_sort_currency(a,b) { 
    aa = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'');
    bb = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'');
    return parseFloat(aa) - parseFloat(bb);
}

function ts_sort_numeric(a,b) { 
    aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]));
    if (isNaN(aa)) aa = 0;
    bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX])); 
    if (isNaN(bb)) bb = 0;
    return aa-bb;
}

function ts_sort_size(a,b) { 
    a_unit = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]);
	b_unit = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]);
    if (a_unit.match(/B$/)) aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,''));
    if (a_unit.match(/kB$/)) aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024;
    if (a_unit.match(/MB$/)) aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024 * 1024;
    if (a_unit.match(/GB$/)) aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024 * 1024 * 1024;
    if (a_unit.match(/TB$/)) aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024 * 1024 * 1024 * 1024;
    if (b_unit.match(/B$/)) bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,''));
    if (b_unit.match(/kB$/)) bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024;
    if (b_unit.match(/MB$/)) bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024 * 1024;
    if (b_unit.match(/GB$/)) bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024 * 1024 * 1024;
    if (b_unit.match(/TB$/)) bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024 * 1024 * 1024 * 1024;
    return aa-bb;
}

function ts_sort_speed(a,b) { 
    a_unit = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]);
	b_unit = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]);
    if (a_unit.match(/B\/s$/)) aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,''));
    if (a_unit.match(/kB\/s$/)) aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024;
    if (a_unit.match(/MB\/s$/)) aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024 * 1024;
    if (a_unit.match(/GB\/s$/)) aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024 * 1024 * 1024;
    if (a_unit.match(/TB\/s$/)) aa = parseFloat(ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024 * 1024 * 1024 * 1024;
    if (b_unit.match(/B\/s$/)) bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,''));
    if (b_unit.match(/kB\/s$/)) bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024;
    if (b_unit.match(/MB\/s$/)) bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024 * 1024;
    if (b_unit.match(/GB\/s$/)) bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024 * 1024 * 1024;
    if (b_unit.match(/TB\/s$/)) bb = parseFloat(ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).replace(/[^0-9.]/g,'')) * 1024 * 1024 * 1024 * 1024;
    return aa-bb;
}

function ts_sort_percent(a,b) {
    a_content = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]);
	b_content = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]);
	if (a_content.match(/N\/A/) && b_content.match(/N\/A/)) return 0;
	if (a_content.match(/N\/A/) && !b_content.match(/N\/A/)) return -1;
	if (!a_content.match(/N\/A/) && b_content.match(/N\/A/)) return 1;
	a1 = a_content.substring(0,a_content.indexOf('%'));
	b1 = b_content.substring(0,b_content.indexOf('%'));
    aa = parseFloat(a1.replace(/[^0-9.]/g,''));
    bb = parseFloat(b1.replace(/[^0-9.]/g,''));
	return aa-bb;
}

function ts_sort_caseinsensitive(a,b) {
    aa = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]).toLowerCase();
    bb = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]).toLowerCase();
    if (aa==bb) return 0;
    if (aa<bb) return -1;
    return 1;
}

function ts_sort_default(a,b) {
    aa = ts_getInnerText(a.cells[SORT_COLUMN_INDEX]);
    bb = ts_getInnerText(b.cells[SORT_COLUMN_INDEX]);
    if (aa==bb) return 0;
    if (aa<bb) return -1;
    return 1;
}

/* search */

function f(){document.searching.search.focus();}
	
function search() {
	
		var searchString = document.searching.search.value;
		
		if (searchString == "") {
			f()
		} else {
			/*
			new Ajax.Request(
				'index.ajax',
				{
				method: 'get'
				parameters: {act: 'search', term: searchString, state: state, date: new Date().getTime() }
				onFailure: function(transport){
				}
				onSuccess: function(transport){
					
				}
			});
			*/
			if(seed){
				var s = "&search=";
			}else{
				var s = "?search=";
			}
			window.location.replace(page_url_simple + s + searchString);
		}
	
}


/* toggle */

function search_check() {
	if (search_on == true) {
		var search_area = document.getElementById('search');
		var search_text = document.getElementById('search_text');
		if(search_area) search_area.style.display = "block";
		if(search_text) search_text.style.display = "none";
	}
}

function toggle() {
	var search = document.getElementById('search');
	if (search.style.display == "block") {
		search.style.display = "none";
	} else {
		search.style.display = "block";
	}
}

function swap() {
	var search_area = document.getElementById('search');
	var search_text = document.getElementById('search_text');
	if(search_text && search_area){
		if (search_area.style.display == "block") {
			search_area.style.display = "none";
			search_text.style.display = "block";
		} else {
			search_area.style.display = "block";
			search_text.style.display = "none";
		}
	}
}


/* json */

/*
    json.js
    2006-04-28

    This file adds these methods to JavaScript:

        object.toJSONString()

            This method produces a JSON text from an object. The
            object must not contain any cyclical references.

        array.toJSONString()

            This method produces a JSON text from an array. The
            array must not contain any cyclical references.

        string.parseJSON()

            This method parses a JSON text to produce an object or
            array. It will return false if there is an error.
*/

function _JSONUtils() {
    var m = {
            '\b': '\\b',
            '\t': '\\t',
            '\n': '\\n',
            '\f': '\\f',
            '\r': '\\r',
            '"' : '\\"',
            '\\': '\\\\'
        },
        s = {
            array: function (x) {
                var a = ['['], b, f, i, l = x.length, v;
                for (i = 0; i < l; i += 1) {
                    v = x[i];
                    f = s[typeof v];
                    if (f) {
                        v = f(v);
                        if (typeof v == 'string') {
                            if (b) {
                                a[a.length] = ',';
                            }
                            a[a.length] = v;
                            b = true;
                        }
                    }
                }
                a[a.length] = ']';
                return a.join('');
            },
            'boolean': function (x) {
                return String(x);
            },
            'null': function (x) {
                return "null";
            },
            number: function (x) {
                return isFinite(x) ? String(x) : 'null';
            },
            object: function (x) {
                if (x) {
                    if (x instanceof Array) {
                        return s.array(x);
                    }
                    var a = ['{'], b, f, i, v;
                    for (i in x) {
                        v = x[i];
                        f = s[typeof v];
                        if (f) {
                            v = f(v);
                            if (typeof v == 'string') {
                                if (b) {
                                    a[a.length] = ',';
                                }
                                a.push(s.string(i), ':', v);
                                b = true;
                            }
                        }
                    }
                    a[a.length] = '}';
                    return a.join('');
                }
                return 'null';
            },
            string: function (x) {
                if (/["\\\x00-\x1f]/.test(x)) {
                    x = x.replace(/([\x00-\x1f\\"])/g, function(a, b) {
                        var c = m[b];
                        if (c) {
                            return c;
                        }
                        c = b.charCodeAt();
                        return '\\u00' +
                            Math.floor(c / 16).toString(16) +
                            (c % 16).toString(16);
                    });
                }
                return '"' + x + '"';
            }
        };


	this.toJSONString = function (obj) {
		if( typeof obj == 'array' ) {
			return s.array(obj);
		} else {
	        return s.object(obj);
		}
	}

	this.parseJSON = function( str ) {
	    try {
	        return eval('(' + str + ')');
	    } catch (e) {
	        return false;
	    }
	
	}
	
};

var JSONUtils = new _JSONUtils();


/* funcs_up */

var txt = "upfile";

function selectUploadType(n) {
	
	if (n == 0) {
		document.getElementById('local').style.display = 'block';
		document.getElementById('url').style.display = 'none';
	} else {
		document.getElementById('url').style.display = 'block';
		document.getElementById('local').style.display = 'none';
	}
}

function sendUpload() {

	var content = "document." + txt + ".value";
	
	if (content == "") {
		alert('Please select something.');
	} else {
		if (txt == "upfile") {
			window.location.replace( page_url + "&local=1" );
		} else {
			window.location.replace( page_url+"&upurl=" + document.myform.upurl.value );
		}
	}
}

function processSC( req , id ) {

  if(req.readyState == 4) {	      	
  	document.getElementById( 'up_msg' ).innerHTML += "..."
  	if(req.status == 200) {
  		resp = req.responseText
  		document.getElementById( 'up_msg' ).innerHTML = resp
  		
  	} else {
  	  alert("Problem: " + req.statusText)
  	}
  }
}

function urlUpload() {
	
	var form = $('myform_url')
	var params = form.serialize()
	alert("urlUpload! : index.ajax?" + params)
	
	new Ajax.Request( "index.ajax?" + params, {
		method: 'GET',
		onFailure: function() {
		},
		onSuccess: function( transport ) {
			var resp = transport.responseText
			//var jason = (json) ? json : eval( '(' + ( resp ) + ')' )
			$('up_msg').update( resp )
			form.reset()
		}
	})
}

function localUpload() {
	
	var form = $('myform_local')
	
	new Ajax.Request( "index.ajax", {
		method: 'POST',
		onFailure: function() {
		},
		onSuccess: function( transport ) {
			var resp = transport.responseText
			//var jason = (json) ? json : eval( '(' + ( resp ) + ')' )
			$('up_msg').update( resp )
			form.reset()
		}
	})
}

function toggleUpload() {
	var tabId = 'tab_upload'
	debug('content visible?: ' + $('content').visible())
	if(!$('upDiv').visible()) {
		Effect.Fade('content',{from:0.9999, to:0.15, duration:0.5, afterFinish: function(){Effect.Appear('upDiv',{duration:0.15})}})
	} else {
		Effect.Fade('upDiv',{ duration:0.15, afterFinish: function(){Effect.Appear('content',{from:0.3, to:0.9999, duration:0.15})}})
	}
	debug('upDiv tab selected? ' + $( tabId ).hasClassName('tab_selected') )
	if($( tabId ).hasClassName('tab_selected')) {
		$(tabId).removeClassName('tab_selected')
	} else {
		$(tabId).addClassName('tab_selected')
	}
}


/* multifile */

/**
 * Convert a single file-input element into a 'multiple' input list
 *
 * Usage:
 *
 *   1. Create a file input element (no name)
 *      eg. <input type="file" id="first_file_element">
 *
 *   2. Create a DIV for the output to be written to
 *      eg. <div id="files_list"></div>
 *
 *   3. Instantiate a MultiSelector object, passing in the DIV and an (optional) maximum number of files
 *      eg. var multi_selector = new MultiSelector( document.getElementById( 'files_list' ), 3 );
 *
 *   4. Add the first element
 *      eg. multi_selector.addElement( document.getElementById( 'first_file_element' ) );
 *
 *   5. That's it.
 *
 *   You might (will) want to play around with the addListRow() method to make the output prettier.
 *
 *   You might also want to change the line 
 *       element.name = 'file_' + this.count;
 *   ...to a naming convention that makes more sense to you.
 * 
 * Licence:
 *   Use this however/wherever you like, just don't blame me if it breaks anything.
 *
 * Credit:
 *   If you're nice, you'll leave this bit:
 *  
 *   Class by Stickman -- http://www.the-stickman.com
 *      with thanks to:
 *      [for Safari fixes]
 *         Luis Torrefranca -- http://www.law.pitt.edu
 *         and
 *         Shawn Parker & John Pennypacker -- http://www.fuzzycoconut.com
 *      [for duplicate name bug]
 *         'neal'
 */
function MultiSelector( list_target, max ){

	// Where to write the list
	this.list_target = list_target;
	// How many elements?
	this.count = 0;
	// How many elements?
	this.id = 0;
	// Is there a maximum?
	if( max ){
		this.max = max;
	} else {
		this.max = -1;
	};
	
	/**
	 * Add a new file input element
	 */
	this.addElement = function( element ){

		// Make sure it's a file input element
		if( element.tagName == 'INPUT' && element.type == 'file' ){

			// Element name -- what number am I?
			element.name = 'upfile_' + this.id++;

			// Add reference to this object
			element.multi_selector = this;

			// What to do when a file is selected
			element.onchange = function(){

				// New file input
				var new_element = document.createElement( 'input' );
				new_element.type = 'file';

				// Add new element
				this.parentNode.insertBefore( new_element, this );

				// Apply 'update' to element
				this.multi_selector.addElement( new_element );

				// Update list
				this.multi_selector.addListRow( this );

				// Hide this: we can't use display:none because Safari doesn't like it
				this.style.position = 'absolute';
				this.style.left = '-1000px';

			};
			// If we've reached maximum number, disable input element
			if( this.max != -1 && this.count >= this.max ){
				element.disabled = true;
			};

			// File element counter
			this.count++;
			// Most recent element
			this.current_element = element;
			
		} else {
			// This can only be applied to file input elements!
			alert( 'Error: not a file input element' );
		};

	};

	/**
	 * Add a new row to the list of files
	 */
	this.addListRow = function( element ){

		// Row div
		var new_row = document.createElement( 'span' );
		new_row.setAttribute("className", "totals");
		new_row.setAttribute("class", "totals");
		new_row.style.clear = "left";
		new_row.style.cssFloat = "left";
		new_row.style.marginRight = "0.4em";

		// Delete button
		var new_row_button = document.createElement( 'input' );
		new_row_button.type = 'button';
		new_row_button.value = 'Remove';

		// References
		new_row.element = element;

		// Delete function
		new_row_button.onclick= function(){

			// Remove element from form
			this.parentNode.element.parentNode.removeChild( this.parentNode.element );

			// Remove this row from the list
			this.parentNode.parentNode.removeChild( this.parentNode );

			// Decrement counter
			this.parentNode.element.multi_selector.count--;

			// Re-enable input element (if it's disabled)
			this.parentNode.element.multi_selector.current_element.disabled = false;

			// Appease Safari
			//    without it Safari wants to reload the browser window
			//    which nixes your already queued uploads
			return false;
		};

		// Set row value
		new_row.innerHTML = element.value;

		// Add button
		new_row.appendChild( new_row_button );

		// Add it to the list
		this.list_target.appendChild( new_row );
		
	};

};

/* funcs_op */

function setParams() {

	var maxDL 		= $("max_dl").value;
	var maxActive	= $("max_active").value;
	var maxActiveSeed	= $("max_active_seed").value;
	var maxActiveSeedEnabled = ($("max_active_seed_enabled").checked)?"1":"0";
	var maxConnPerTor	= $("max_conn_pertorrent").value;
	var maxConn	= $("max_conn").value;
	var maxDown 	= $("max_dl_speed").value;
	var maxAutoUpEnabled = ($("max_ul_speed_auto").checked)?true:false;
	var maxUp 		= $("max_ul_speed").value;
	var maxUpSeed 		= $("max_ul_speed_seed").value;
	var maxUpSeedEnabled = ($("max_ul_speed_seed_enabled").checked)?"1":"0";
	var maxUps 		= $("max_ups").value;
	var maxUpsSeed 		= $("max_ups_seed").value;
	var compTabEnabled = ($("comp_tab").checked)?true:false;
	var paginationPerPage = $("pagination_per_page").value;
	var oldPerPage = paginationPerPage;
	
	var strAlert = "";
	
	if(isNaN(maxDL) || isNaN(maxActive) || isNaN(maxConn) || isNaN(maxDown) || isNaN(maxUp) || isNaN(maxUps) || isNaN(maxUpsSeed) || isNaN(paginationPerPage)) {
		strAlert+="Numbers Only\n"
	} else {
		if(maxDL < 0) {
			strAlert+="Please set a positive number of max downloads\n";
		}
		if(maxActive < 0) {
			strAlert+="Please set a positive number of max active\n"
		}
		if(maxActiveSeed < 0) {
			strAlert+="Please set a positive number of max active when only seeding\n"
		}
		if((maxDL == 0 || parseInt(maxDL) > parseInt(maxActive)) && maxActive != 0) {
			strAlert+="Max downloads cannot be higher than Max active\n"
		}
		if(maxConnPerTor < 0) {
			strAlert+="Please set a positive number of max connections per torrent\n"
		}
		if(maxConn < 0) {
			strAlert+="Please set a positive number of max connections globally\n"
		}
		if(maxDown < 0) {
			strAlert+="Please set a positive number for max down speed\n"
		}
		if(maxUp < 0) {
			strAlert+="Please set a positive number for max up speed\n"
		}
		if(maxUpSeed < 0) {
			strAlert+="Please set a positive number for max up speed when only seeding\n"
		}
		if(maxUps < 0) {
			strAlert+="Please set a positive number for max uploads\n"
		}
		if(maxUpsSeed < 0) {
			strAlert+="Please set a positive number for max uploads when only seeding\n"
		}
	}
	
	if(strAlert == "") {

		new Ajax.Request(
			'index.ajax',
			{
				method: 'get',
				parameters: {max_dl:maxDL,max_active:maxActive,max_active_seed:maxActiveSeed,max_active_seed_enabled:maxActiveSeedEnabled,max_conn_pertor:maxConnPerTor,max_conn:maxConn,max_dl_speed:maxDown,max_ups:maxUps,max_ups_seed:maxUpsSeed,
							max_auto_up:(maxAutoUpEnabled)?"1":"0",
							max_ul_speed:maxUp,max_ul_speed_seed:maxUpSeed,max_ul_speed_seed_enabled:maxUpSeedEnabled,
							comp_tab:(compTabEnabled)? "1":"0",
							pagination_per_page:paginationPerPage,
							date: new Date().getTime()},
				onFailure: function(transport) {
					$('msg').innerHTML = "Problem: " + transport.statusText
				},
				onSuccess: function(transport) {
					$('msg').innerHTML = (transport.responseText).gsub('\\.','.<br/>')
			  		Element.show('msg');
			  		new Effect.Pulsate('msg')
			  		
			  		if(compTabEnabled) {
			  			if(!$('tab_comp').visible()){
			  				new Effect.Appear('tab_comp',{afterFinish:function(){new Effect.Shake('tab_comp',{duration:0.5})}})
			  				updateStats( state )
			  			}
			  		} else {
			  			if($('tab_comp').visible()){
			  				new Effect.Pulsate('tab_comp',{duration:0.5,pulses:2,afterFinish:function(){new Effect.Fade('tab_comp',{afterFinish:function(){setState(ST_SEEDING, {force: true})}})}})
			  				updateStats( state )
			  			}
			  		}
			  		//if( state != ST_DOWNLOADING ) updateDisplay( state )
			  		updateTL()
				}
			}
		);
		
	} else {
		$('msg').innerHTML = '<span style="color:#DD6633">' + strAlert + '</span>';
	}
}

function switchOnlySeeding(id, isChecked) {
	$(id).disabled=!isChecked;
}
function switchAutoSeeding(id, id1, id2, isChecked) {
	if(!isChecked) {
		$(id).disabled=false;
		$(id1).disabled=false;
		$(id2).disabled=false;
		switchOnlySeeding('max_ul_speed_seed',$(id2).checked)
	} else {
		$(id).disabled=true;
		$(id1).disabled=true;
		$(id2).disabled=true;
		switchOnlySeeding('max_ul_speed_seed',false)
	}
}
Event.observe(window,'load',function(e){
    categories = $A( categories )
    catSelect = $('cat_list')

    torrentsFromServer = $A( torrentsFromServer )
    torrentsDisplayed = $A( torrentsDisplayed )
    torrentsToRemove = $A( torrentsToRemove )
    torrentsToAdd = $A( torrentsToAdd )

    if(showDebug) $('debug').show()

    sortables_init()

    if(!$("comp_tab").checked) $('tab_comp').hide()
})