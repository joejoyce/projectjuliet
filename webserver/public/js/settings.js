$(document).ready(function() {
	$('#pause').click(function() {
		$.ajax({
		    url: "/api/v1/settings/pause",
		    type: 'GET',
		    cache: false
		});
	});

	$('#restart').click(function() {
		$.ajax({
		    url: '/api/v1/settings/restart',
		    type: 'GET',
		    cache: false
		});
	});

	$('#set').click(function() {
		$.ajax({
		    url: "/api/v1/settings/setskip/" + $('#skip').val(),
		    type: 'GET',
		    cache: false
		});
	});
});