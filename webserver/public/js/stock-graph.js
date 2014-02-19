$(document).ready(function() {
  var stockData = JSON.parse($('meta[name=stock-data]').attr("content"));
  var priceScale = Number($('meta[name=price-scale]').attr("content"));
  var symbol = $('meta[name=symbol]').attr("content");
  var chartData = [];
  
  $(stockData).each(function(i, data) {
    chartData.push(["Volume: " + data.volume, data.price * priceScale]);
  });
  
  $('#chart-container').highcharts('StockChart', {
    rangeSelector: {
      selected: 1
    },
    series: [{
      name: symbol,
      data: chartData,
      tooltip: {
        valueDecimals: 2
      }
    }]
  });
});