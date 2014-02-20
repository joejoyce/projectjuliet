$(document).ready(function() {
	var cData = $('meta[name=cData]').attr("content");
	console.log(cData);
 	cData = $.parseJSON(cData);

 	$('#candle').highcharts('StockChart', {
		rangeSelector : {
			selected : 1
		},

		title : {
			text : symbol + ' Stock Price'
		},

		series : [{
			type : 'candlestick',
			name : symbol + ' Stock Price',
			data : cData,
			dataGrouping : {
				units : [
					['Hour', // unit name
					[1] // allowed multiples
				], [
					'month', 
					[1, 2, 3, 4, 6]]
				]
			}
		}]
	});	
});