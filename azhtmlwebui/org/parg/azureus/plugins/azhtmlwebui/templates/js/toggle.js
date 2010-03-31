function search_check() {
	if (search_on == true) {
		var search_area = document.getElementById('search');
		var search_text = document.getElementById('search_text');
		search_area.style.display = "block";
		search_text.style.display = "none";
	}
}

function toggle() {
	var search = document.getElementById('search');
	if (search.style.display == "block") {
		search.style.display = "none";
	} else {
		search.style.display = "block";
	}
}

function swap() {
	var search_area = document.getElementById('search');
	var search_text = document.getElementById('search_text');
	if (search_area.style.display == "block") {
		search_area.style.display = "none";
		search_text.style.display = "block";
	} else {
		search_area.style.display = "block";
		search_text.style.display = "none";
	}
}
