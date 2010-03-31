function shout( text ) {
	alert( text )
}
var selectedTorrentList = new Array()

function assignCategory( cat ){
	selectedTorrentList.each( function( index ) {
		//alert("assigning cat: " + cat + ' index: ' + index)
		//var hash = ( hashes[index] ) hashes[index] : index; 
		alert(index + " : " + hashes[index])
		var ajajax = new Ajax.Request(
		'index.ajax',
		{
			method: 'get',
			parameters: {cat: cat, cmd: "set" , hash: hashes[index] , date:  new Date().getTime()},
			onFailure: function(transport){
				alert("Problem: " + transport.statusText)
			},
			onSuccess: function(transport){
				var id = 'c' + index;
				resp = transport.responseText
				alert(resp + " / " + decodeURIComponent(resp))
	  			$( id ).innerHTML = decodeURIComponent(resp)
			}
		});
	})
}
function updateSelectedTorrentList( state, index ) {
	//alert( state + ' / ' + index);
	if ( state ) {
		selectedTorrentList.push( index )
	} else {
		selectedTorrentList = $A( selectedTorrentList )
		selectedTorrentList = selectedTorrentList.without( index )
	}
	//alert(selectedTorrentList)
	/*
	alert('test ' + TorrentManager.test)
	alert('torrentManager: ' + TorrentManager)
	var tor = TorrentManager.getTorrentByHash( hashes[index] )
	alert(tor.getHash())
	tor.setSelected( state )
	//updatePossibleActions()*/
}
function setCat(cat, index) {
	alert("inside set cat: " + cat)
	//createHTTPReq( cat, "set", index ) ;
	
	
}
