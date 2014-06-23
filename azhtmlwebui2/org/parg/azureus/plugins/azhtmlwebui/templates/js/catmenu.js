/***********************************************
* Pop-it menu- © Dynamic Drive (www.dynamicdrive.com)
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
		
/*		var req = getAjaxReq()
		req.overrideMimeType('text/xml');
		req.onreadystatechange=function() { processSC( req , "popitmenu" , 0 , index); };
		//cat = encodeURIComponent(cat);
		req.open('GET', 'index.ajax?cat=all&cmd=menu' + '&date=' + new Date().getTime(), true);
		req.send(null);*/
	
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

function getAjaxReq() {

	if (window.XMLHttpRequest) // if Mozilla, Safari etc
		req = new XMLHttpRequest()
	else if (window.ActiveXObject){ // if IE
		try {
		req = new ActiveXObject("Msxml2.XMLHTTP")
		} catch (e){
			try{
			req = new ActiveXObject("Microsoft.XMLHTTP")
			} catch (e){}
		}
	}
	else return false
	
	return req;
}

function createCatReq( cat, cmd , index){
	
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
		
		if (cmd == "new") {
			//updateCatList()
		}
}

function updateCatList() {

	new Ajax.Request(
		//'c' + index,
		'index.ajax',
		{
			method: 'get',
			parameters: {cat: cat, cmd: cmd , hash: hashes[index] , date:  new Date().getTime()},
			onFailure: function(transport){
				alert("Problem: " + transport.statusText)
			},
			onSuccess: function(transport){
				var id = 'c' + index;
				resp = transport.responseXML
				var resp = req.responseXML
	  		categories.clear;
	  		var nodes = resp.getElementsByTagName('cat');
	  		//alert(nodes.length)
	  		var res1 = ''
	  		for(j = 0; j<nodes.length ; j++ ) {
	  			var name = nodes[j].getElementsByTagName('name')[0].firstChild.nodeValue;
	  			//alert(name)
	  			categories[j] = name
	  		}
	  		
			}
		});
}

function processSC( req , id , i , index ) {
switch(i) {
  case 0:
	  if(req.readyState == 4) {	      	
	  	document.getElementById( id ).innerHTML = "..."
	  	document.getElementById( 'testtest' ).innerHTML = "..."
	  	if(req.status == 200) {
	  		var resp = req.responseXML
	  		
	  		var nodes = resp.getElementsByTagName('cat');
	  		//alert(nodes.length)
	  		var res1 = ''
	  		for(j = 0; j<nodes.length ; j++ ) {
	  			var name = nodes[j].getElementsByTagName('name')[0].firstChild.nodeValue;
	  			//alert(name)
	  			res1 += '<a href="javascript:setCat( \'' + name + '\' , \'' + index + '\' );">'+decodeURIComponent(name)+'<\/a><br\/>';
	  		}
	  		res1 += '<hr>';
			res1 += '<a href="javascript:createCat(\''+index+'\');">'+cmd_cat_new+'</a>';
			res1 += '<a href="javascript:removeCat(\''+index+'\');">'+cmd_cat_rem+'<\/a>';
	  		
	  		document.getElementById( id ).innerHTML = res1
	  		document.getElementById( 'testtest' ).innerHTML = res1
	  		
	  	} else {
	  	  alert("Problem: " + req.statusText)
	  	}
	  }
  case 1:
  	if(req.readyState == 4) {	      	
	  	document.getElementById( id ).innerHTML = "..."
	  	if(req.status == 200) {
	  		resp = req.responseText
	  		document.getElementById( id ).innerHTML = decodeURIComponent(resp)
	  		
	  	} else {
	  	  alert("Problem: " + req.statusText)
	  	}
	  }
  
  }
}
/*
var cmd_actions = new Array()
var cmd_cat = new Array()
*/
function updateSelectedTorrentList( hash, state ) {
	//alert( "state: " + state + ' / index: ' + index + ' / hash: ' + hashes[index]);
	var tor = torrentManager.getTorrentByHash( hash )
	//alert( tor.getHash() )
	var sel = !tor.isSelected();
	if(state!=null) { sel = state }
	tor.setSelected( sel )
	//updatePossibleActions()
}
/*
function updatePossibleActions() {
	selectedTorrentHashList.each( function(hash) {
		
	});
	updateActions()
}
function updateActions() {
	//var cmd_options = $( 'cmd' ).getElementByTagName('option')
}
*/
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
	if($( id )) new Effect.Pulsate( id, {duration: 1, pulses: 1, afterFinish: function() { if($(id)) $( id ).remove() } } );
	var torrent = torrentManager.getTorrentByHash( hash )
	torrentManager.remove( torrent );
	if(notlast != null && !notlast) updateDisplay()
}
function takeAction( type ) {
	if( isNaN( type ) ) return;
	
	var selectedTorrents = torrentManager.getSelectedTorrents();
	
	selectedTorrents.each( function( torrent ){
		actionTorrent( type, torrent ) 
	});	
}
function actionTorrent( type, torrent ) {
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
				updateTorrentTR( h , state );
				updateStats( state );
			}
		});
}

function updateTorrentTR( hashes , state ) {
	debug("UpdateTorrentTR :: entering updateTorrentTR(" + hashes +" , " + state + ")")
	//var torrent = torrentManager.getTorrentByHash( hash )
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
						var inside = null
						//if(resTorrents[resHashes[k]].type != "0") {
						//	inside = sdTorrentTemplate.evaluate(resTorrents[resHashes[k]])
						//} else {
						//	inside = dlTorrentTemplate.evaluate(resTorrents[resHashes[k]])
						//}
						if( ! $(tr_id) ) {
							createTorrentTR( torrent )
							torrentRow.createCells($(tr_id), resTorrents[resHashes[k]], torrent )
						} else {
							torrentRow.populateCells( resTorrents[resHashes[k]], torrent )
						}
						//debug( 'UpdateTorrentTR :: with: ' + inside)
						var moved = torrent.updatePos( resTorrents[resHashes[k]].position )
						if( moved ) updateClassNames()
						//debug( 'UpdateTorrentTR :: id: ' + $(tr_id).id)
						//debug( 'UpdateTorrentTR :: innerHTML: ' + $(tr_id).innerHTML)
						//$(tr_id).innerHTML = inside
						if(resTorrents[resHashes[k]].isFP == 1) $('sr_'+resHashes[k]).style.color='#B02B2C'
						debug( 'UpdateTorrentTR :: updating selected chkbox')
						
						updateSelectedCheckbox( resHashes[k] )
						debug( 'UpdateTorrentTR :: tr innerHTML: ' + $(tr_id).innerHTML)
					} else {
						debug( 'UpdateTorrentTR :: torrent hash: ' + resHashes[k] + ' :: disappears')
						disappearTorrent( tr_id , resHashes[k])
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
				
				var torrents = torrentManager.getTorrents()
				
				debug('UpdateTorrentList :: torrentManager size: ' + torrents.length)
				
				var totalDisappeared = 0
				var temp = 0
				var hash = null
				var list = new Array();
				for(var i=0; i< torrents.length; i++){
					temp = 0
					hash = torrents[i].getHash()
					for(var j=0; j< jason.size; j++){
						
						if (hash == jason[j].hash) {
							temp = 1
							debug('UpdateTorrentList :: torrent ' + hash + ' in list: keeping')
						}
						
					}
					if( temp == 0) {
						var id = "r" + hash;
						debug('UpdateTorrentList :: torrent ' + hash + ' NOT in list: removing')
						if($(id)) disappearTorrent(id, hash)
						
					}
					totalDisappeared += temp;
				}
				
				debug('UpdateTorrentList :: begin callback: ' + callback + ' / typeof: ' + typeof callback)
				if(callback!=null && (typeof callback == 'string')) { window.setTimeout("eval("+callback+")",1000); }
				debug('UpdateTorrentList :: end callback')
				
				var h = '';
				
				//alert(jason.length + " : " + jason.size + " : " + jason.count)
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
				
				if(h!=''){
					debug('UpdateTorrentList :: calling updateTorrentTR: ' + h)
					if(totalDisappeared) {
						window.setTimeout( "updateTorrentTR('" + h + "'," + state + " )" , 1000 )
					} else {
						updateTorrentTR( h, state );
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
	debug('CreateTorrentTR :: tbody: ' + tbody)
	//table.appendChild(tbody);
	debug('CreateTorrentTR :: creating row')
	var row = document.createElement("tr");
	row.id='r' + hash
	//row.style.display='none'
	debug('CreateTorrentTR :: adding row to table')
	tbody.appendChild(row);
	/*
	var row = table.insertRow(2);
	row.id='r' + hash
	row.style.display='none'
	//table.appendChild(row);*/
	debug('CreateTorrentTR :: appearing row')
	//Effect.Appear(row.id)
	
	row = $( row.id )
	
	//var class_name = 'trtorrent' + ( ( ( ($( row.id ).previousSiblings()).size() - 1) % 2 ) ? '' : '_odd' );
	
	var class_name = 'trtorrent' + ( ( ( table.rows.length - 1) % 2 ) ? '' : '_odd' );
	debug('CreateTorrentTR :: classname')
	//if( !row.hasClassName( class_name ) ) row.classNames().add( class_name )
	debug('CreateTorrentTR :: setting classname')
	row.classNames().set( class_name )
   	debug('CreateTorrentTR :: observing mouseover')
   	Event.observe(row, 'mouseover', function() { Element.addClassName(row,'trtorrent_hover'); } )
   	debug('CreateTorrentTR :: observing mouseout')
   	Event.observe(row, 'mouseout', function() { Element.removeClassName(row,'trtorrent_hover'); } )
   	debug('CreateTorrentTR :: observing click')
   	Event.observe(row, 'click', function() { updateSelectedTorrentList( hash ); updateSelectedCheckbox( hash ) });
   	
   	debug('CreateTorrentTR :: row visible?: ' + row.visible() )
}

function updateDisplay() {
	debug('updating display')
	var torrents = torrentManager.getTorrents()
	torrents = $A( torrents )
	//var _torrents = new Array();
	var _torrents = ""
	var ind = 0;
	torrents.each( function ( torrent ){
		//var inter =  Math.random() * 200 * torrents.size();
		//setTimeout( "updateTorrentTR('"+ torrent.getHash() +"' , '"+ state +"')" , inter );
		//_torrents.push(  torrent.getHash() )
		_torrents += torrent.getHash() + ",";
	})
	//$('test').innerHTML += " $$$$ " + _torrents + " $$$$ ";
	//updateTorrentTR( _torrents.toJSONString() , state);
	if(_torrents != "") updateTorrentTR( _torrents , state);

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
	var torrents = torrentManager.getTorrentsByPos();
	for( var i=0; i < torrents.size() ; i++) {
		var class_name = 'trtorrent' + ( ( i % 2 ) ? '' : '_odd' );
		//alert("new class: " + class_name)
		var row = $( 'r' + torrents[i].getHash() )
		//alert("had that class already: " + row.hasClassName( class_name ))
		if( !row.hasClassName( class_name ) ) row.classNames().add( class_name )
		row.classNames().set( class_name )   	
	}
}
var myGlobalHandlers = {
		onCreate: function(){
			Element.show('ajax');
		},
	
		onComplete: function() {
			if(Ajax.activeRequestCount == 0){
				Element.hide('ajax');
			}
		}
	};

Ajax.Responders.register(myGlobalHandlers);

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
		updateTL( "updateTorrentTableHeader()" );
		//setTimeout( "updateDisplay()", 1500);
	}
}

function updateTorrentTableHeader() {
	var d = document.getElementsByClassName('d')
	if(state == ST_DOWNLOADING) {
		d.each(function(el){Element.show(el)})
	} else {
		d.each(function(el){Element.hide(el)})
	}
}

function displayOptions( ) {
	var tabId = 'tab_options'
	debug('content visible?: ' + $('content').visible())
	$('msg').update('');
	if(!$('options').visible()) {
		Effect.Fade('content',{from:0.9999, to:0.3, duration:0.5, afterFinish: function(){Effect.Appear('options')}})
	} else {
		Effect.Fade('options',{ afterFinish: function(){Effect.Appear('content',{from:0.3, to:0.9999, duration:0.5})}})
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
var showDebug = true;

function debug( txt ) {
	if(showDebug){
		$('debug').innerHTML += ' <br/> ' + (new Date()).getTime() + ':: ' + txt
		$('debug').scrollTop = $('debug').scrollHeight
	}
}

Event.observe(window, 'load', function(e){
	if(showDebug) $('debug').show()
})