$(document).ready(function() {
	var cData = $('meta[name=cData]').attr("content");
	cData = $.parseJSON(cData);
        cData.sort(function(a,b) {
		return a.time - b.time;
});
 	var symbol = $('meta[name=symbol]').attr("content");

 	var volume = [];
 	var ohlc = [];

 	for (i = 0; i < cData.length; i++) {
		ohlc.push([			
			Number(cData[i].time*1000), 
			Number(cData[i].open.toFixed(2)), 
			Number(cData[i].high.toFixed(2)), 
			Number(cData[i].low.toFixed(2)), 
			Number(cData[i].close.toFixed(2))
		]);
		
		volume.push([
			Number(cData[i].time*1000),
			Number(cData[i].volume) 
		]);
	}

	var groupingUnits = [[
		'week',                        
		[1]                             
	], [
		'month',
		[1, 2, 3, 4, 6]
	]];


 	$('#candle-graph').highcharts('StockChart', {
		rangeSelector : {
			selected : 1
		},

		title : {
			text : symbol + ' Stock Price'
		},

		yAxis: [{
		        title: {
		            text: 'OHLC'
		        },
		        height: 200,
		        lineWidth: 2
		    }, {
		        title: {
		            text: 'Volume'
		        },
		        top: 300,
		        height: 100,
		        offset: 0,
		        lineWidth: 2
		    }],
		    
		series: [{
				type: 'candlestick',
      	name: symbol,
      	data: ohlc
	  	}, {
		  	type: 'column',
		    name: 'Volume',
		    data: volume,
		    yAxis: 1
		}]
	});	
});
