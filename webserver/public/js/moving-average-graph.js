$(document).ready(function() {
	var mData = $('meta[name=mData]').attr("content");
	mData = $.parseJSON(mData);
    mData.forEach(function(data) {
        data.data.sort(function(a,b) {
            return a.time - b.time;
        });    
    });
    
 	var symbol = $('meta[name=symbol]').attr("content");

 	var volume = [];
 	var chartData = [];

 	for(i = 0; i < mData.length; i++) {
        var seriesData = [];
        for (j = 0; j < mData[i].data.length; j++) {
    		seriesData.push([			
    			Number((mData[i].data[j].time-18000)*1000), 
    			Number(mData[i].data[j].average.toFixed(2))
    		]);
        }
        chartData.push({name:mData[i].name, data:seriesData});
	}

 	$('#moving-average-graph').highcharts('StockChart', {
            chart: {},
            rangeSelector: {
                selected: 1
            },
            title: {
                text: symbol + ' Stock Price Moving Averages'
            },
            series : chartData
	});	
}); 