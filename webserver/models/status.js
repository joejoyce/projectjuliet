/**
 * Status model
 */

var net = require('net');

/**
 * List all clients in the cluster
 */
exports.listClients = function() {
  /*
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');
  client.write('status|listclients\n');

  var clientList = '';
  client.on('data', function(data) {
    clientList = JSON.parse(data);
    client.end();
  
  });
  return clientList;
  */
  return [
    { name: '127.0.0.1', totalPackets: 100, currentPackets: 10 },
    { name: '127.0.0.2', totalPackets: 101, currentPackets: 11 },
    { name: '127.0.0.3', totalPackets: 102, currentPackets: 12 },
    { name: '127.0.0.4', totalPackets: 103, currentPackets: 13 }
  ];
}

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
}