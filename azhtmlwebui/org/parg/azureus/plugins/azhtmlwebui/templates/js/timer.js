var timer = null

function start() {
	//document.getElementById('stopimg').style.display = 'block';
	//document.getElementById('startimg').style.display = 'none';
	now = now - 1
	if (now == 0) refreshPage()
	if(now < 10) now = "0" + now
	document.getElementById("timer").innerHTML = now
	timer = setTimeout("start()",1000)
}
function refreshPage() {
	window.location.replace( page_url );
}
function stop() {
	clearTimeout(timer)
	//document.getElementById('stopimg').style.display = 'none';
	document.getElementById('stopimg').innerHTML='<img src="images/start.gif" border="0" width="8" onMouseOver="style.cursor=\'pointer\';" onClick="javascript:restart();">';
	//document.getElementById('startimg').style.display = 'block';
}
function restart() {
	document.getElementById('stopimg').innerHTML='<img src="images/pause.gif" border="0" width="8" onMouseOver="style.cursor=\'pointer\';" onClick="javascript:stop();">';
	start();
}
function setRefresh(n) {
	if(document.getElementById('refresh').value != refresh_rate) {
		var page="";
		if(n == "0") { // downloads
		if(search_on){
			page = page_url_simple + "?search=" + search + "&";
		} else {
			page = page_url_simple + "?";
		}
	} else {						// seeds
		if(search_on){
			page = page_url_simple + "&search=" + search + "&";
		} else {
			page = page_url_simple + "&";
		}
	}
		window.location.replace( page+"refresh=" + document.getElementById('refresh').value );
	}
}

var page_request = false

function ajax_set(spanID){

var rate = document.getElementById(spanID).value;
if (window.XMLHttpRequest) // if Mozilla, Safari etc
	page_request = new XMLHttpRequest()
else if (window.ActiveXObject){ // if IE
	try {
	page_request = new ActiveXObject("Msxml2.XMLHTTP")
	} catch (e){
		try{
		page_request = new ActiveXObject("Microsoft.XMLHTTP")
		} catch (e){}
	}
}
else return false

page_request.onreadystatechange=function() { processStateChange(spanID); };
page_request.open('GET', 'index.ajax?'+spanID+'='+rate + '&date=' + new Date().getTime(), true);
page_request.send(null);
}

function processStateChange(id) {
  if(page_request.readyState == 4) {	      	
  	document.getElementById('timer').innerHTML = "..."
  	if(page_request.status == 200) {
  		resp = page_request.responseText
  		document.getElementById(id).value = resp
  		now = resp
  	} else {
  	  alert("Problem: " + page_request.statusText)
  	}
  }
}