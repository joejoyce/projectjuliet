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
exports.getTrades = function(symbolIndex, criteria) {
/*
  var symbolIndex = parseInt(symbolIndex);
  var tradeList = '';
  
  // Construct WHERE clause
  var where = '';
  if (criteria) {
    criteria.forEach(function(criterion, index) {
      where += ' AND ' + criterion.field + ' = ' + criterion.value;
    });
  }

  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('basic|SELECT * FROM trade WHERE symbol_id = ' + symbolIndex
             + where + '\n');

  client.on('data', function(data) {
    tradeList = JSON.parse(data);
  });

  client.on('end', function() {
    client.end();
  });

  return tradeList;
*/
  return [
    {trade_id: 1, symbol_id: 1, price: 678, volume: 300,
     offered_s: 1, offered_seq_num: 1, completed_s: 1, completed_seq_num:1},
    {trade_id: 2, symbol_id: 1, price: 345, volume: 500,
     offered_s: 2, offered_seq_num: 2, completed_s: 2, completed_seq_num:2}
  ]
}