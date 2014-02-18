$(document).ready(function() {
 	var stockData = $('meta[name=stock-data]').attr("content");
 	stockData = $.parseJSON(stockData);
 	var priceScale = Number($('meta[name=price-scale]').attr("content"));
 	var symbol = $('meta[name=symbol]').attr("content");
 	var chartData = [];
 	$(stockData).each(function(i, data) {
 		chartData.push([data.offered_s*1000,data.price*priceScale]);
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
