/**
 * Trade model
 */

var net = require('net');

/**
 * List all of the trades for a given symbol index
 * 
 * symbolIndex  the index of the specified symbol (this is NOT the same as the stock symbol)
 * criteria     an array of criteria that trade records must satisfy
 */
exports.getTrades = function(symbolIndex, callback) {
  var symbolIndex = parseInt(symbolIndex);
  var tradeList = '';
  
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('basic|SELECT * FROM trade WHERE symbol_id = ' +symbolIndex+ '\n');

  client.on('data', function(data) {
    tradeList += data;
  });

  client.on('end', function() {    
    tradeList = JSON.parse(tradeList);
    client.end();
    callback(tradeList);
  });
};

exports.getCandle = function(symbolIndex, priceScale, callback) {
  var symbolIndex = parseInt(symbolIndex);
  var cData = '';
  
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('cluster|candlestick '+symbolIndex+' 600\n');

  client.on('data', function(data) {
    cData += data;
  });

  client.on('end', function() {    
    cData = JSON.parse(cData);
    client.end();
    var candles = [];
    cData.forEach(function(candle) {
      if(candle.open == 0) return true;

      candle.start *= priceScale;
      candle.open *= priceScale;
      candle.close *= priceScale;
      candle.high *= priceScale;
      candle.low *= priceScale;
      candles.push(candle);
    });

    callback(candles);
  });
};

exports.getMovingAverage = function(symbolIndex, priceScale, callback) {
  var symbolIndex = parseInt(symbolIndex);
  var mData = '';
  
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('cluster|movingAverage '+symbolIndex+'\n');

  client.on('data', function(data) {
    mData += data;
  });

  client.on('end', function() {    
    mData = JSON.parse(mData);
    client.end();
    var averageData = [];
    mData.forEach(function(series) {
                var averages = [];
                series.data.forEach(function(average) {
                    average.average *= priceScale;
                    averages.push(average);
                });
            averageData.push({name:series.name, data:averages});
            });

    callback(averageData);
  });
};