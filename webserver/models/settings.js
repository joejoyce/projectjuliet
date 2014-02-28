var net = require('net');

exports.pause = function() {
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('config|pause\n');
};

exports.restart = function() {
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('config|restart\n');
  client.end();
  console.log("sent!");
};

exports.setSkip = function(skip) {
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('config|set data.rate='+skip+'\n');
  client.end()
};


exports.getSkip = function(callback) {
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('config|get data.rate\n');
  client.on('data', function(data) {
  	data = JSON.parse(data);
        client.end();
  	callback(data.skip);
  });
};

