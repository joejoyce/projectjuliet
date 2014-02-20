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
};

/**
 * List all symbols
 */

var listCache = [];

exports.list = function(callback) {
  if(listCache.length != 0) {
    callback(listCache);
    return;
  }
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
    listCache = output;
    callback(output);
  });
};