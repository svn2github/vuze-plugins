var vz = window.vz || {}

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
	var search_url = "http://search.vuze.com/xsearch/?q=" + search_input + "&search_source=" + escape(window.location.href)
		//$("#remotesearch_container").html("<iframe id='remotesearch'></iframe>")
		//$("#remotesearch").attr({src: search_url})
	if( vz.searchQuery != search_url ) {
		$("#remotesearch_container").text("")
		vz.remote = null
		vz.createRemote( search_url )
	}
	vz.searchQuery = search_url
	$("#torrent_filter_bar").hide();
	if( !iPhone && transmission[Prefs._ShowInspector] ) $("#torrent_inspector").hide();
	$("#torrent_container").hide();
	$("#remotesearch_container").show()
}
vz.backFromSearch = function(){
	$("#torrent_filter_bar").show();
	if( !iPhone && transmission[Prefs._ShowInspector] ) $("#torrent_inspector").show();
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
            		vz.dls[url] = {url: url, ts: new Date().getTime()}
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
	if( $("#open").is(":visible") ) {
		$("#open").hide();$("#remove").hide();$("#pause_selected").hide();$("#resume_selected").hide();
		$("#pause_all").hide();$("#resume_all").hide();
		$("#t_bar").addClass("search")
		$("#search_li").show()
		$("#search_btn").show()
		vz.executeSearch()
		$("#search_input").focus()
	} else {
		$("#open").show();$("#remove").show();$("#pause_selected").show();$("#resume_selected").show();
		$("#pause_all").show();$("#resume_all").show();
		$("#t_bar").removeClass("search")
		$("#search_li").hide()
		$("#search_btn").hide()
		vz.backFromSearch()
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