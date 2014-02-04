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
		var companyName = dataObj[0].company_name;
		var companyId = dataObj[0].id;
		var priceScale = dataObj[0].price_scale;
		
		res.render('order', {
    	title: 'Order',
    	companySymbol: companySymbol,
    	companyName: companyName,
    	priceScale: priceScale,
    	companyId: companyId
    });
	});

	client.write('basic|SELECT * FROM symbol WHERE symbol="' + companySymbol + '"\n');
};
