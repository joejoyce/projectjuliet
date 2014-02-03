var net = require('net');

/**
 * GET /order
 * Gets an order book for a specific company.
 */

exports.getOrder = function(req, res) {
	var companyId = req.param("companyId");
	var client = net.connect(1337, 'localhost');
	client.setEncoding('utf8');
	
	client.on('data', function(data) {
		console.log("Got data from server: " + data);
		res.render('order', {
    	title: 'Order',
    	companyId: companyId,
    	response: data
  	});
	});

	client.write(companyId + "\n");
};
