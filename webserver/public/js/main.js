$(document).ready(function() {
	// Event handlers
	$('#filter').on('keyup', function() {
		var input = $(this).val().toUpperCase();
	  if (input == '') {
	    $('#symbols li').show();
	  } else {
	    $('#symbols li').hide();
	    $('#symbols li[data-symbol^="'+input+'"]').show();
	  }
	});
});