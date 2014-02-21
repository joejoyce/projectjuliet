$(document).ready(function() {
	refreshBids();
	refreshOffers();
});

function refreshBids() {
	$.ajax({
		url : "/api/v1/orders/bids/" + ,
		dataType : 'json',
		success : function(result) {
			var time = new Date(parseInt(result.data) * 1000);
			var hours = time.getHours();
			var minutes = time.getMinutes();
			var seconds = time.getSeconds();
			var value = ((hours < 10) ? ' ' : '') + hours + ':'
					+ ((minutes < 10) ? '0' : '') + minutes + ':'
					+ ((seconds < 10) ? '0' : '') + seconds;
			display.setValue(value);
			window.setTimeout('refreshBids()', 1000);
		}
	});
}

function refreshOffers() {
	$.ajax({
		url : "/api/v1/orders/offers/" + ,
		dataType : 'json',
		success : function(result) {
			var time = new Date(parseInt(result.data) * 1000);
			var hours = time.getHours();
			var minutes = time.getMinutes();
			var seconds = time.getSeconds();
			var value = ((hours < 10) ? ' ' : '') + hours + ':'
					+ ((minutes < 10) ? '0' : '') + minutes + ':'
					+ ((seconds < 10) ? '0' : '') + seconds;
			display.setValue(value);
			window.setTimeout('refreshOffers()', 1000);
		}
	});
}
