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
        $("#search_input").val("find...")
        return false
    }
    return true;
}
vz.executeSearch = function(){
    var search_input = $("#search_input").get(0).value
    if(! vz.validateSearch( search_input ) ) return;
    var search_url = "http://search.vuze.com/xsearch/?q=" + search_input + "&mode=" + vz.mode + "&search_source=" + escape(window.location.href)
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
    $("#remotesearch_container").show()
}
vz.backFromSearch = function(){
    $("#torrent_filter_bar").show();
    if( !isMobileDevice && transmission[Prefs._ShowInspector] ) $("#torrent_inspector").show();
    $("#torrent_container").show();
    $("#remotesearch_container").hide()
}
vz.createRemote = function(remote_url){
    vz.remote = new easyXDM.Interface(/** The channel configuration */{
        local: "/easyXDM/hash.html",
        remote: remote_url,
        container: document.getElementById("remotesearch_container")
    }, /** The interface configuration */ {
        remote: {
            noOp: {
                isVoid: true
            }
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
                    transmission.showAllClicked()
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
    },/**The onReady handler*/ function(){
        //vz.remote.noOp();
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
$(document).ready( function(){
    vz.utils.selectOnFocus()
})