var mysql = require('mysql');
var fs = require('fs');

var connection = mysql.createConnection({
	host : 'localhost',
	user : 'root',
	password : 'rootword'
});


connection.query('USE juliet', function (err) {
	if (err) throw err;
	connection.query('select symbol_id from trade group by symbol_id order by count(symbol_id) desc limit 2000', function (err, data) {
    console.log(data.length);
    var arr = [];
    data.forEach(function(d) {
      arr.push(d.symbol_id);
    });
    connection.query('select * from symbol where symbol_id in ('+arr.join(',')+')', function (err, stocks) {
  		if (err) throw err;
      fs.writeFile("./stocks.txt", JSON.stringify(stocks), function(err) {
        if(err) {
            console.log(err);
        } else {
            console.log("The file was saved!");
        }
      }); 
  	});
  });
});

