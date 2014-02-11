var net = require('net');

/**
 * GET /order
 * Gets an order book for a specific company.
 */

exports.getOrder = function(req, res) {
	var companySymbol = req.param("companySymbol");

	var client = net.connect(1337, 'localhost');
	client.setEncoding('utf8');

	var client2 = net.connect(1337, 'localhost');
	client2.setEncoding('utf8');


	var totalOrderBookData = "";
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
		client2.write('basic|select * from order_book where symbol_id = "' + symbolIndex + '"ORDER BY order_id DESC LIMIT 100\n');
	});


	client2.on('data', function(data) {
		totalOrderBookData += data;
	});

	client2.on('end', function() {
		var dataObj = JSON.parse(totalOrderBookData);		
		
		res.render('order', {
    	title: 'Order',
    	data: dataObj,
    	companyName: companyName,
    	priceScale: priceScale,
    	companySymbol: companySymbol
    });
	});
};

exports.orderBook = function(req, res) {
	res.render('orderBook', {
    	title: 'Order'    	
    });
};

