$(document).ready(function() {
	var cData = $('meta[name=cData]').attr("content");
	cData = $.parseJSON(cData);
 	var symbol = $('meta[name=symbol]').attr("content");

 	var volume = [];
 	var ohlc = [];

 	for (i = 0; i < cData.length; i++) {
 		console.log(cData[i].time*1000);
		ohlc.push([			
			Number(cData[i].time*1000), 
			Number(cData[i].open), 
			Number(cData[i].high), 
			Number(cData[i].low), 
			Number(cData[i].close)
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