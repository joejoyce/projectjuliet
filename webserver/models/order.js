/**
 * Order model
 */

var net = require('net');

/**
 * List all of the orders for a given symbol index
 * 
 * symbolIndex  the index of the specified symbol (this is NOT the same as the stock symbol)
 * criteria     an array of criteria that order records must satisfy
 */
exports.getOffers = function(symbolIndex, callback) {
  var symbolIndex = parseInt(symbolIndex);
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('basic|SELECT * FROM order_book WHERE symbol_id = ' +symbolIndex+ ' AND is_ask=1 AND deleted=0 AND added=1 ORDER BY price LIMIT 25\n');

  var offers = '';

  client.on('data', function(data) {
    offers += data;
  });

  client.on('end', function() {
    offers = JSON.parse(offers);
    client.end();
    callback(offers);
  });
};

exports.getBids = function(symbolIndex, callback) {
  var symbolIndex = parseInt(symbolIndex);
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('basic|SELECT * FROM order_book WHERE symbol_id = ' +symbolIndex+ ' AND is_ask=0 AND deleted=0 AND added=1 ORDER BY price DESC LIMIT 25\n');

  var bids = '';

  client.on('data', function(data) {
    bids += data;
  });

  client.on('end', function() {
    bids = JSON.parse(bids);
    client.end();
    callback(bids);
  });
};