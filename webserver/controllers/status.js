var net = require('net');

/**
 * GET /status
 * System status page
 */

exports.getStatus = function(req, res) {
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');

  client.write('status|listclients\n');

  client.on('data', function(data) {
    var dataObj = JSON.parse(data);
console.dir(dataObj);
    res.render('status', {
      title: 'Status',
      data: dataObj
    });
    client.end();
  });
};
