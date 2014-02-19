/**
 * Symbol model
 */

var net = require('net');

/**
 * Get the symbol of the specified symbol index
 * symbolIndex  the symbol index of the specified symbol
 */
exports.get = function(symbolIndex) {

  var symbolIndex = parseInt(symbolIndex);
  /*
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('basic|SELECT * FROM symbol WHERE symbol_id = ' + symbolIndex
             + ' ORDER BY symbol.symbol\n');

  var symbol = '';
  client.on('data', function(data) {
    symbol = JSON.parse(data);
    client.end();
  });
  */

  var symbol = '';
  switch(symbolIndex) {
    case 1: symbol = {id: 1, symbol: 'AAPL', company_name: 'Apple Inc.', price_scale: 0.0001}; break;
    case 2: symbol = {id: 2, symbol: 'MSFT', company_name: 'Microsoft Corporation', price_scale: 0.0001}; break;
    case 3: symbol = {id: 3, symbol: 'GOOG', company_name: 'Google Inc.', price_scale: 0.0001}; break;
    default: symbol = {id: 0, symbol: 'IDUNNO', company_name: 'Mystery Inc.', price_scale: 0.0001}
  }

  return symbol;
}

/**
 * List all symbols
 */
exports.list = function() {

  /*var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('basic|SELECT * FROM symbol\n');

  var symbolList = '';
  client.on('data', function(data) {
    symbolList = JSON.parse(data);
    client.end();
  });

  return symbolList;
  */
  return [
          {kind: 'symbol', id:1,symbol:'AAPL',company_name:'Apple Inc.'},
          {kind: 'symbol', id:2,symbol:'MSFT',company_name:'Microsoft Corporation'},
          {kind: 'symbol', id:3,symbol:'GOOG',company_name:'Google'}
        ];
}