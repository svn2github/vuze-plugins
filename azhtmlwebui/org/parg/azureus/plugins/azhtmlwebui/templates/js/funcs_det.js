var page_request = false

function makePOSTrequest(hash, index, priority){

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

var parameters = "hash=" + hash + "&index=" + index + "&priority=" + priority;
page_request.onreadystatechange=function() { processStateChange2(index, priority); };
page_request.open('GET', 'index.ajax?' + parameters + '&date=' + new Date().getTime(), true);
page_request.setRequestHeader("Cache-Control","no-cache");
page_request.send(null);
}

function processStateChange2(index, priority) {
  if(page_request.readyState == 4) {	      	
  	document.getElementById('msg' + index).innerHTML = "Updating..."
  	if(page_request.status == 200) {
  		var p = page_request.responseText;
  		if(p != 5) {
  			document.getElementById('msg' + index).innerHTML = "Ok!"
  			document.getElementById('s' + index).options[priority].selected = true;
  		} else {
  			document.getElementById('msg' + index).innerHTML = "Error!"
  		}
  	} else {
  	  document.getElementById('msg' + index).innerHTML = "Problem: " + page_request.statusText
  	}
  }
}

function ajax_priority(hash, index, priority) {
	makePOSTrequest(hash, index, priority)
}