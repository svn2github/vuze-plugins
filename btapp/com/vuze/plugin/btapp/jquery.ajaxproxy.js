// NOTE: btapp sdk ships with a .ajax extender (xhr.js), but (older?) versions
//       do not properly handle "callback=?"
//window.falcon = {
//		get_xhr_prefix : function() {
//			return 'http://' + location.hostname + ":" + location.port + '/ajaxProxy';
//		}
//};

// Ours is much simpler and probably makes real javascript devs scream in horror,
// but it appears to work
jQuery(document).ready(function(){
	if (window.vuzeAjaxProxyInstalled) {
		return;
	}
	
	jQuery.ajax = (function(_ajax){
    
    var ajaxProxy = 'http://' + location.hostname + ":" + location.port + '/ajaxProxy/'; 
    
    return function(o) {
        var url = o.url;
            
        if (o.url.indexOf(ajaxProxy) !== 0) {
        	// no escaping the url, otherwise jqeuery won't handle "callback=?"
        	// Luckily, the full URL as we declare it gets passed directly in the
        	// http header, and Vuze parses it all out.
        	o.url = ajaxProxy + url;
        }
        return _ajax.apply(this, arguments);
        
    };
    
	})(jQuery.ajax);
	window.vuzeAjaxProxyInstalled = true;
});
