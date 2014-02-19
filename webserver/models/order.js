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
exports.getOrders = function(symbolIndex, criteria) {
/*
  var symbolIndex = parseInt(symbolIndex);
  
  // Construct WHERE clause
  var where = '';
  if (criteria) {
    criteria.forEach(function(criterion, index) {
      where += ' AND ' + criterion.field + ' = ' + criterion.value;
    });
  }

  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('basic|SELECT * FROM order_book WHERE symbol_id = ' + symbolIndex
             + where + ' ORDER BY price LIMIT 25\n');

  var orderList = '';
  client.on('data', function(data) {
    orderList = JSON.parse(data);
    client.end();
  });

  return orderList;
*/
  var symbolList = '';
  if (criteria[0].value == 1) {
    symbolList = [
      {order_id: 1, symbol_id: 1, price: 345, volume: 1000, is_ask: 1,
       placed_s: 1, placed_seq_num: 1, updated_s: 1, updated_seq_num: 1},
      {order_id: 2, symbol_id: 4, price: 754, volume: 100, is_ask: 1,
        placed_s: 2, placed_seq_num: 2, updated_s: 2, updated_seq_num: 2}
    ];
  } else {
    symbolList = [
      {order_id: 34, symbol_id: 1, price: 7654, volume: 1345, is_ask: 0,
       placed_s: 1, placed_seq_num: 2, updated_s: 3, updated_seq_num: 4},
      {order_id: 43, symbol_id: 4, price: 367, volume: 5432, is_ask: 0,
        placed_s: 1, placed_seq_num: 2, updated_s: 3, updated_seq_num: 4}
    ];
  }
  return symbolList;
}



/*
  
  var offerOrderBookData = "";
  var bidOrderBookData = "";
  
  var companyName = "";
  var priceScale = 0;
  var symbolIndex = "";

  var escapedCompanySymbol = companySymbol.replace("\"", "");
  escapedCompanySymbol = escapedCompanySymbol.replace("\'", "");
  client.write('basic|select * from symbol WHERE symbol="' + escapedCompanySymbol + '"\n');

  client.on('data', function(data) {
    var parsedCompanyData = JSON.parse(data);
    if(parsedCompanyData.length == 0) {
      res.render('orderBook', {
        title: 'Order',
        error: "Symbol not found"
      });
      client.end();
      return;
    }
    companyName = parsedCompanyData[0].company_name;
    priceScale = 1/Math.pow(10, parsedCompanyData[0].price_scale);
    symbolIndex = parsedCompanyData[0].symbol_id;
    client2.write('basic|select * from order_book where symbol_id = "' + symbolIndex + '" AND is_ask=1 ORDER BY price LIMIT 25\n');
    client.end();
  });

  client2.on('data', function(data) {
    offerOrderBookData += data;
  });

  client2.on('end', function() {
    client3.write('basic|select * from order_book where symbol_id = "' + symbolIndex + '" AND is_ask=0 ORDER BY price DESC LIMIT 25\n');
    client2.end();
  });

  client3.on('data', function(data) {
    bidOrderBookData += data;
  });

  client3.on('end', function() {
    var offers = JSON.parse(offerOrderBookData);
    var bids = JSON.parse(bidOrderBookData);
    var spread = 0;
    
    if(offers[0] && bids[0])
      spread = offers[0].price*priceScale - bids[0].price*priceScale;

    res.render('order', {
      title: 'Order',
      offers: offers,
      bids: bids,
      companyName: companyName,
      priceScale: priceScale,
      companySymbol: companySymbol,
      spread: spread
    });

    client3.end();
  });
}
*/