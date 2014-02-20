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

	var client3 = net.connect(1337, 'localhost');
	client3.setEncoding('utf8');


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
			client3.write('cluster|candlestick '+symbolIndex+' 900\n');
	});

	var cData = "";

	client3.on('data', function(data) {
		cData += data;
	});

	client3.on('end', function() {
		var priceJSON = JSON.parse(totalTradeData);
		console.log(cData);
		var cJSON = JSON.parse(cData);
		var candles = [];
		cJSON.forEach(function(candle) {
			if(candle.open == 0) return true;
			candle.start *= priceScale;
			candle.open *= priceScale;
			candle.close *= priceScale;
			candle.high *= priceScale;
			candle.low *= priceScale;
			candles.push(candle);
		});
	  console.log(candles);
		res.render('stock', {
    	title: 'Stock Price',
    	data: priceJSON,
    	dataString: totalTradeData,
    	companyName: companyName,
    	priceScale: priceScale,
    	companySymbol: companySymbol,
    	cData: JSON.stringify(candles)
    });
	});
};


exports.stock = function(req, res) {
  res.render('stockPrice', {
    title: 'Stock Price'
  });
};

