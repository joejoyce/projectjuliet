$(document).ready(function() {
  var stockData = $('meta[name=stock-data]').attr("content");
  stockData = $.parseJSON(stockData);
  var priceScale = Number($('meta[name=price-scale]').attr("content"));
  var symbol = $('meta[name=symbol]').attr("content");
  var chartData = [];
  
  stockData.sort(function(a, b) {
    return a.offered_s - b.offered_s;
  });

  stockData.forEach(function(data) {
    chartData.push([(data.offered_s-18000)*1000,data.price*priceScale]);
  });
  
  $('#stock-graph').highcharts('StockChart', {
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