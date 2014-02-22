var client = {};

$(document).ready(function() {
	$('#filter').fastLiveFilter('#symbols');
	$('#filter-form').on('submit', function(e) {
		var visibleSymbols = $('#symbols li:visible');
		if (visibleSymbols.length == 1) {
			window.location.href = visibleSymbols.find('a').attr('href');
		}
		e.preventDefault();
	});
	$('#brand').click(function(e) {
		window.location = "https://www.youtube.com/watch?v=Y4MnpzG5Sqc";
	});
});