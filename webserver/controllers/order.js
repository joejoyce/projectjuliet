var net = require('net');

/**
 * GET /order
 * Gets an order book for a specific company.
 */

exports.getOrder = function(req, res) {
	var companySymbol = req.param("companySymbol");
	var client = net.connect(1337, 'localhost');
	client.setEncoding('utf8');
	
	client.on('data', function(data) {
		console.log("Got data from server: " + data);

		var dataObj = JSON.parse(data);
		var companyName = dataObj.company_name;
		var companyId = dataObj.id;
		var priceScale = dataObj.price_scale;
		
		res.render('order', {
    	title: 'Order',
    	companySymbol: companySymbol,
    	companyName: companyName,
    	priceScale: priceScale,
    	companyId: companyId
    });
	});

	client.write('basic|SELECT * FROM symbol WHERE symbol="' + companySymbol + '"');
};

exports.orderBook = function(req, res) {
	res.render('orderBook', {
    	title: 'Order'    	
    });
};
