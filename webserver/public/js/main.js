$(document).ready(function() {
	$('#filter').fastLiveFilter('#symbols');
	$('#filter-form').on('submit', function() {
		var visibleSymbols = $('#symbols li:visible');
		if (visibleSymbols.length == 1) {
			window.location.href = visibleSymbols.find('a').attr('href');
		}
	});
	$('#brand').click(function(e) {
		window.location = "http://www.downloadmoreram.com";
	});
});