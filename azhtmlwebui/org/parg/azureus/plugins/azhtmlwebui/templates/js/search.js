function f(){document.searching.search.focus();}
	
function search() {
	
		var searchString = document.searching.search.value;
		
		if (searchString == "") {
			f()
		} else {
			if(seed){
				var s = "&search=";
			}else{
				var s = "?search=";
			}
			window.location.replace(page_url_simple + s + searchString);
		}
	
}