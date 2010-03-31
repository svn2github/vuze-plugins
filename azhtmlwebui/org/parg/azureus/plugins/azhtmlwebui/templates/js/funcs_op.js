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
					$('msg').innerHTML = transport.responseText
			  		$('msg').style.color = '#444'
			  		$('msg').style.backgroundColor = '#FDFA47'
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
	if(isChecked) {
		$(id).disabled=false;
	} else {
		$(id).disabled=true;
	}
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
	if(!$("comp_tab").checked) $('tab_comp').hide()
})