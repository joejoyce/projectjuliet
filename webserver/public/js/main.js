$(document).ready(function() {
 	var stockData = $('meta[name=stock-data]').attr("content");
 	stockData = $.parseJSON(stockData);
 	var priceScale = Number($('meta[name=price-scale]').attr("content"));
 	var symbol = $('meta[name=symbol]').attr("content");
 	var chartData = [];
 	var x = 0;
 	$(stockData).each(function(i, data) {
 		chartData.push(["Volume: " + data.volume,data.price*priceScale]);
 		x += 1;
 	});
 	$('#chartcontainer').highcharts('StockChart', {
			rangeSelector : {
				selected : 1
			},

			title : {
				text : symbol + ' Stock Price'
			},
			
			series : [{
				name : symbol,
				data : chartData,
				tooltip: {
					valueDecimals: 2
				}
			}]
	});
});
