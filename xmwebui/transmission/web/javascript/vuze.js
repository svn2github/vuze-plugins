var vz = window.vz || {}

vz.mode = "trial"

vz.updatePrefs = function( prefs ){
	var az_mode = prefs["az-mode"];
	if ( typeof az_mode == 'undefined' ){
		vz.mode = "trial";
	}else{
		vz.mode = az_mode;
	}
}

vz.searchQuery = null;

vz.validateSearch = function(str){
    if(!str || str == "" || str == "find...") {
        return false
    }
    return true;
}
vz.executeSearch = function(){
    var search_input = $("#search_input").get(0).value
    if(! vz.validateSearch( search_input ) ) return;
    var search_url = "http://search.vuze.com/xsearch/?q=" + search_input + "&xdmv=2.4.17.1&mode=plus&goo=//" + vz.mode + "&search_source=" + encodeURIComponent(window.location.href)
    //$("#remotesearch_container").html("<iframe id='remotesearch'></iframe>")
    //$("#remotesearch").attr({src: search_url})
    if( vz.searchQuery != search_url ) {
        $("#remotesearch_container").text("")
        vz.remote = null
        vz.createRemote( search_url )
    }
    vz.searchQuery = search_url
    $("#torrent_filter_bar").hide();
    if( !isMobileDevice && transmission[Prefs._ShowInspector] ) $("#torrent_inspector").hide();
    $("#torrent_container").hide();
    $("#remotesearch_container").show();
}
vz.backFromSearch = function(){
    $("#torrent_filter_bar").show();
    if( !isMobileDevice && transmission[Prefs._ShowInspector] ) $("#torrent_inspector").show();
    $("#torrent_container").show();
    $("#remotesearch_container").hide()
}
vz.createRemote = function(remote_url){
	console.log(remote_url);
    vz.remote = new easyXDM.Rpc(/** The channel configuration */{
        local: "/easyXDM/hash.html",
        swf: "easyxdm.swf",
        remote: remote_url,
        container: document.getElementById("remotesearch_container")
    }, /** The interface configuration */ {
        remote: {
            postMessage: {},
    		noOp: {}
        },
        local: {
            alertMessage: {
                method: function(msg){
                    alert(msg);
                },
                isVoid: true
            },
            download: {
                method: function(url){
                    /*make sure call isn't made several times*/
                    if( vz.dls[url] != null && (new Date().getTime() - vz.dls[url].ts < 2000) ) return
                    vz.ui.toggleRemoteSearch()
                    transmission.setFilterMode('downloading');
                    transmission.setSortMethod( 'age' )
                    transmission.setSortDirection( 'descending' )
                    transmission.remote.addTorrentByUrl( url, {} );
                    vz.dls[url] = {
                        url: url,
                        ts: new Date().getTime()
                        }
                },
                isVoid: true
            },
            noOp: {
                method: function(){
                //alert('done')
                },
                isVoid: true
            }
        }
    });
}

vz.ui = {}

vz.ui.toggleRemoteSearch = function(){
    if( $(".toolbar-main").is(":visible") ) {
        $(".toolbar-main").hide();
        $(".toolbar-vuze").show();
        //$("#toolbar").addClass("search")
        vz.executeSearch()
        $("#search_input").focus();
    } else {
        $(".toolbar-vuze").hide();
        $(".toolbar-main").show();
        //$("#toolbar").removeClass("search");
        vz.backFromSearch();
    }
}

vz.dls = {}
vz.utils = {}

vz.utils = {
    selectOnFocus: function(){
        $("#search_input").focus(function(){
            this.select();
        });
    }
}

function isTouchDevice(){
	try{
		document.createEvent("TouchEvent");
		return true;
	}catch(e){
		return false;
	}
}

// From http://chris-barr.com/2010/05/scrolling_a_overflowauto_element_on_a_touch_screen_device/#comment-65
function touchScroll(selector) {
    if (isTouchDevice()) {
        var scrollStartPosY=0;
        var scrollStartPosX=0;
        $('body').delegate(selector, 'touchstart', function(e) {
            scrollStartPosY=this.scrollTop+e.originalEvent.touches[0].pageY;
            scrollStartPosX=this.scrollLeft+e.originalEvent.touches[0].pageX;
        });
        $('body').delegate(selector, 'touchmove', function(e) {
            if ((this.scrollTop < this.scrollHeight-this.offsetHeight &&
                this.scrollTop+e.originalEvent.touches[0].pageY < scrollStartPosY-5) ||
                (this.scrollTop != 0 && this.scrollTop+e.originalEvent.touches[0].pageY > scrollStartPosY+5))
                    e.preventDefault();
            if ((this.scrollLeft < this.scrollWidth-this.offsetWidth &&
                this.scrollLeft+e.originalEvent.touches[0].pageX < scrollStartPosX-5) ||
                (this.scrollLeft != 0 && this.scrollLeft+e.originalEvent.touches[0].pageX > scrollStartPosX+5))
                    e.preventDefault();
            this.scrollTop=scrollStartPosY-e.originalEvent.touches[0].pageY;
            this.scrollLeft=scrollStartPosX-e.originalEvent.touches[0].pageX;
        });
    }
}

function vuzeOnResize() {
    var h = ($(window).height() - 80);
	$('#remotesearch_container').height(h);
	if ($(window).width() > 900) {
    	$("#torrent_logo").show();
	} else {
    	$("#torrent_logo").hide();
	}
}

$(document).ready( function(){
	
	$(window).resize(vuzeOnResize);

    vz.utils.selectOnFocus();
    // WebKit 533.1  (Android 2.3.3) needs scrollable divs hack
    // WebKit 533.17.9  (iPhone OS 4_2_1) needs scrollable divs hack
    //
    // WebKit 534.13 can do scrollable divs
    // WebKit 534.30 (Android 4.1.2) can do scrollable divs
    // WebKit 535.19 (Chrome 18.0.1025.166) can do scrollable divs
    // Assumed: 534 added scrollable Divs!
	if ($.browser.safari) {
		if (parseInt($.browser.version, 10) < 534) {
		    touchScroll(".scrollable");
		}
	}
	
	var ua = navigator.userAgent;
	if (ua.indexOf("iPhone OS 4_") !== -1 || ua.indexOf("iPhone OS 3_") !== -1) {
		// older iPods crash on search results
		$("#toolbar-search").hide();
	}
})