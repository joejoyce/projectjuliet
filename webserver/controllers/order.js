var net = require('net');

/**
 * GET /order
 * Gets an order book for a specific company.
 */

//is ask => im selling this i.e. offer

exports.getOrder = function(req, res) {
	var companySymbol = req.param("companySymbol");

	var client = net.connect(1337, 'localhost');
	client.setEncoding('utf8');

	var client2 = net.connect(1337, 'localhost');
	client2.setEncoding('utf8');

	var client3 = net.connect(1337, 'localhost');
	client3.setEncoding('utf8');


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
		
		var spread = offers[0].price*priceScale - bids[0].price*priceScale;

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
};

exports.orderBook = function(req, res) {
	res.render('orderBook', {
    	title: 'Order'    	
    });
};
