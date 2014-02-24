/**
 * Status model
 */

var net = require('net');

/**
 * List all clients in the cluster
 */
exports.getStatus = function(callback) {  
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('status|GabeN\n');
  
  var status = '';
  client.on('data', function(data) {
    status += data;
  });

  client.on('end', function() {
    status = JSON.parse(status);
    client.end();
    callback(status);
  });
};

/**
 * Get the current simulation system time
 */
exports.getTime = function(callback) {
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('status|time\n');

  client.on('data', function(data) {
    client.end();
    callback(data);
  });
};

exports.getLatency = function(callback) {
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('status|latency\n');

  client.on('data', function(data) {
    data = JSON.parse(data);
    client.end();
    callback(data);
  });
};

exports.getThroughput = function(callback) {
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('status|throughput\n');

  client.on('data', function(data) {
    data = JSON.parse(data);
    client.end();
    callback(data.data);
  });
};