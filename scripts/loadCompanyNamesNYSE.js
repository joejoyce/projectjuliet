var mysql = require('mysql');
var fs = require('fs');

var connection = mysql.createConnection({
	host : 'localhost',
	user : 'root',
	password : 'rootword'
});

var stream = fs.createReadStream('../data/companylistNYSE.csv');

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
	var elements = line.split(",");

	var symbol = elements[0];
	var name = elements[1];
	
	connection.query('USE juliet', function (err) {
		var q = 'UPDATE symbol SET company_name=' + name + ' WHERE symbol=' + symbol;
		connection.query(q, function (err) {
			console.log("Inserted: " + name);
		});
	});
}

readLines(stream, processLine);
