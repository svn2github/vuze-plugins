function setParams() {

	var params = ""

	var maxDL 		= document.getElementById("max_dl").value;
	var maxActive	= document.getElementById("max_active").value;
	var maxActiveSeed	= document.getElementById("max_active_seed").value;
	var maxActiveSeedEnabled = (document.getElementById("max_active_seed_enabled").checked)?"1":"0";
	var maxConnPerTor	= document.getElementById("max_conn_pertorrent").value;
	var maxConn	= document.getElementById("max_conn").value;
	var maxDown 	= document.getElementById("max_dl_speed").value;
	var maxAutoUpEnabled = (document.getElementById("max_ul_speed_auto").checked)?true:false;
	var maxUp 		= document.getElementById("max_ul_speed").value;
	var maxUpSeed 		= document.getElementById("max_ul_speed_seed").value;
	var maxUpSeedEnabled = (document.getElementById("max_ul_speed_seed_enabled").checked)?"1":"0";
	var maxUps 		= document.getElementById("max_ups").value;
	var maxUpsSeed 		= document.getElementById("max_ups_seed").value;
	var compTabEnabled = (document.getElementById("comp_tab").checked)?true:false;
	
	var strAlert = "";
	
	if(isNaN(maxDL) || isNaN(maxActive) || isNaN(maxConn) || isNaN(maxDown) || isNaN(maxUp) || isNaN(maxUps) || isNaN(maxUpsSeed)) {
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
		params = "max_dl=" + maxDL + "&max_active=" + maxActive + "&max_active_seed=" + maxActiveSeed + "&max_active_seed_enabled=" + maxActiveSeedEnabled + "&max_conn_pertor=" + maxConnPerTor + "&max_conn=" + maxConn + "&max_dl_speed=" + maxDown + "&max_ups=" + maxUps + "&max_ups_seed=" + maxUpsSeed;
		params += (maxAutoUpEnabled)? "&max_auto_up=1" : "&max_auto_up=0" + "&max_ul_speed=" + maxUp + "&max_ul_speed_seed=" + maxUpSeed  + "&max_ul_speed_seed_enabled=" + maxUpSeedEnabled;
		params += "&comp_tab=" + ((compTabEnabled)? "1":"0");
		makePOSTreq(params)
	} else {
		document.getElementById('msg').innerHTML = '<span style="color:#DD6633">' + strAlert + '</span>';
	}
}

function switchOnlySeeding(id, isChecked) {
	if(isChecked) {
		document.getElementById(id).disabled=false;
	} else {
		document.getElementById(id).disabled=true;
	}
}
function switchAutoSeeding(id, id1, id2, isChecked) {
	if(!isChecked) {
		document.getElementById(id).disabled=false;
		document.getElementById(id1).disabled=false;
		document.getElementById(id2).disabled=false;
		switchOnlySeeding('max_ul_speed_seed',document.getElementById(id2).checked)
	} else {
		document.getElementById(id).disabled=true;
		document.getElementById(id1).disabled=true;
		document.getElementById(id2).disabled=true;
		switchOnlySeeding('max_ul_speed_seed',false)
	}
}
var page_request = false

function makePOSTreq(parameters){

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

page_request.onreadystatechange=function() { processStateChange(); };
page_request.open('GET', 'index.ajax?' + parameters + '&date=' + new Date().getTime(), true);
page_request.setRequestHeader("Cache-Control","no-cache");
page_request.send(null);
}

function processStateChange() {
  if(page_request.readyState == 4) {	      	
  	document.getElementById('msg').innerHTML = "Updating..."
  	if(page_request.status == 200) {
  		document.getElementById('msg').innerHTML = page_request.responseText
  		document.getElementById('msg').style.color = '#444'
  		document.getElementById('msg').style.backgroundColor = '#CCEDDA'
  	} else {
  	  document.getElementById('msg').innerHTML = "Problem: " + page_request.statusText
  	}
  }
}