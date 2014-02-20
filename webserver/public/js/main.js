$(document).ready(function() {
	$('#filter').fastLiveFilter('#symbols');
	$('#filter-form').on('submit', function{
		var visibleSymbols = $('#symbols li:visible');
		if (visibleSymbols.length == 1) {
			window.location.href = "/order-book/" + visibleSymbols.data('id');
		}
	});
});