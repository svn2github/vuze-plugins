function f(){document.searching.search.focus();}
	
function search() {
	
		var searchString = document.searching.search.value;
		
		if (searchString == "") {
			f()
		} else {
			/*
			new Ajax.Request(
				'index.ajax',
				{
				method: 'get'
				parameters: {act: 'search', term: searchString, state: state, date: new Date().getTime() }
				onFailure: function(transport){
				}
				onSuccess: function(transport){
					
				}
			});
			*/
			if(seed){
				var s = "&search=";
			}else{
				var s = "?search=";
			}
			window.location.replace(page_url_simple + s + searchString);
		}
	
}