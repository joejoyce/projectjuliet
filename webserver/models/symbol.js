/**
 * Symbol model
 */

var net = require('net');

/**
 * Get the symbol of the specified symbol index
 * symbolIndex  the symbol index of the specified symbol
 */
exports.get = function(symbolIndex, callback) {
  var symbolIndex = parseInt(symbolIndex);
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('basic|SELECT * FROM symbol WHERE symbol_id = ' +symbolIndex+ ' ORDER BY symbol\n');

  var symbol = '';
  client.on('data', function(data) {
    symbol = JSON.parse(data);
    client.end();
    callback(symbol[0]);
  });
  
  /*var symbol = '';
  switch(symbolIndex) {
    case 1: symbol = {id: 1, symbol: 'AAPL', company_name: 'Apple Inc.', price_scale: 0.0001}; break;
    case 2: symbol = {id: 2, symbol: 'MSFT', company_name: 'Microsoft Corporation', price_scale: 0.0001}; break;
    case 3: symbol = {id: 3, symbol: 'GOOG', company_name: 'Google Inc.', price_scale: 0.0001}; break;
    default: symbol = {id: 0, symbol: 'IDUNNO', company_name: 'Mystery Inc.', price_scale: 0.0001}
  }

  return symbol;*/
};

/**
 * List all symbols
 */
exports.list = function(callback) {
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('basic|SELECT * FROM symbol\n');

  var symbolList = '';

  client.on('data', function(data) {
    symbolList += data;  
  });

  client.on('end', function() {
    var data = JSON.parse(symbolList);

    var output = [];
    data.forEach(function(row) {
      row.kind = 'symbol';
      output.push(row);
    });
    client.end();
    callback(output);
  });

  /* return [
          {kind: 'symbol', id:1,symbol:'AAPL',company_name:'Apple Inc.'},
          {kind: 'symbol', id:2,symbol:'MSFT',company_name:'Microsoft Corporation'},
          {kind: 'symbol', id:3,symbol:'GOOG',company_name:'Google'}
        ];*/
};