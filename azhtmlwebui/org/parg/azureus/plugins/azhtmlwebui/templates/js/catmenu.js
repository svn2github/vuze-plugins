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
	
			for (i=1;i<categories.length;i++) {
				//res += '<a href="'+page+'torrent='+hashes[index]+'&setcat='+categories[i]+'">'+unescape(categories[i])+'<\/a>';
				res += '<a href="javascript:setCat( \'' + categories[i] + '\' , \'' + index + '\' );">'+unescape(categories[i])+'<\/a>';
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
	createHTTPReq( cat, "set", index ) ;
}

function createCat(index) {
	var cat	= prompt("Create category");
	
	if (cat != null)	{
				//window.location.replace(url+'torrent='+hash+'&newcat='+cat);
				createHTTPReq( cat , "new" , index);
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
				createHTTPReq( cat , "rem" , index);
	}
}


function createHTTPReq( cat, cmd , index){
	var req = false

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
	
	req.onreadystatechange=function() { processSC( req , 'c'+index ); };
	req.open('GET', 'index.ajax?cat='+cat+'&cmd='+cmd+'&hash='+hashes[index] + '&date=' + new Date().getTime(), true);
	req.send(null);
}

function processSC( req , id ) {

  if(req.readyState == 4) {	      	
  	document.getElementById( id ).innerHTML = "..."
  	if(req.status == 200) {
  		resp = req.responseText
  		document.getElementById( id ).innerHTML = resp
  		
  	} else {
  	  alert("Problem: " + req.statusText)
  	}
  }
}