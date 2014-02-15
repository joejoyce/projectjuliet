var net = require('net');

/**
 * GET /stock
 * Stock price graph for specific company
 */

exports.getStockPrice = function(req, res) {
  var companySymbol = req.param("companySymbol");

	var client = net.connect(1337, 'localhost');
	client.setEncoding('utf8');

	var client2 = net.connect(1337, 'localhost');
	client2.setEncoding('utf8');


	var totalTradeData = "";
	var companyName = "";
	var priceScale = 0;
	var symbolIndex = "";

	var escapedCompanySymbol = companySymbol.replace("\"", "");
	escapedCompanySymbol = escapedCompanySymbol.replace("\'", "");
	client.write('basic|select * from symbol WHERE symbol="' + escapedCompanySymbol + '"\n');

	client.on('data', function(data) {
		var parsedCompanyData = JSON.parse(data);
		if(parsedCompanyData.length == 0) {
			res.render('stockPrice', {
		    title: 'Stock Price',
		    error: 'Symbol not found'
		  });
		  client.end();
		  return;
		}
		companyName = parsedCompanyData[0].company_name;
		priceScale = 1/Math.pow(10, parsedCompanyData[0].price_scale);
		symbolIndex = parsedCompanyData[0].symbol_id;
		client2.write('basic|select * from trade where symbol_id = "' + symbolIndex + '"\n');
	});


	client2.on('data', function(data) {
		totalTradeData += data;
	});

	client2.on('end', function() {
		console.log(totalTradeData);
		var dataObj = JSON.parse(totalTradeData);		
		
		res.render('stock', {
    	title: 'Stock Price',
    	data: dataObj,
    	dataString: totalTradeData,
    	companyName: companyName,
    	priceScale: priceScale,
    	companySymbol: companySymbol
    });
	});
};


exports.stock = function(req, res) {
  res.render('stockPrice', {
    title: 'Stock Price'
  });
};

