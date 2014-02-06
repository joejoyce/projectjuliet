var mysql = require('mysql');
var fs = require('fs');

var connection = mysql.createConnection({
	host : 'localhost',
	user : 'root',
	password : 'rootword'
});

var stream = fs.createReadStream('../data/ARCASymbolMapping.txt');

function readLines(input, func) {
	var remaining = '';

  input.on('data', function(data) {
    remaining += data;
    var index = remaining.indexOf('\n');
    var last  = 0;
    while (index > -1) {
      var line = remaining.substring(last, index);
      last = index + 1;
      func(line);
      index = remaining.indexOf('\n', last);
    }

    remaining = remaining.substring(last);
  });

  input.on('end', function() {
    if (remaining.length > 0) {
      func(remaining);
    }
  });
}

function processLine(line) {
	var elements = line.split("|");
	
	//console.log("Symbol: " + elements[1] + ", symbol_id: " + elements[2] + ", price_scale: " + elements[7]);
	
	var symbol = elements[1];
	var id = elements[2];
	var price_scale = elements[7];

	connection.query('USE Juliet', function (err) {
		if (err) throw err;
		connection.query('INSERT INTO symbol VALUES (' + id + ',"' + symbol + '",' + '"",' + price_scale + ',0)', function (err) {
			if (err) throw err;
			console.log("Inserted: " + symbol);
		});
	});
}

readLines(stream, processLine);
