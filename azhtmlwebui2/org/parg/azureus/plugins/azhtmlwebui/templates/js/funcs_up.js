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

function createHTTPReq(){
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
	
	req.onreadystatechange=function() { processSC( req ); };
	req.open('GET', 'index.ajax?autodir=1' + '&date=' + new Date().getTime(), true);
	req.send(null);
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
	Effect.toggle( 'upDiv', 'blind' );
}

