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
<<<<<<< HEAD
    console.dir(dataObj);
=======
console.dir(dataObj);
>>>>>>> ae88efc677d6bfaf79fd3530a45661282cb7db57
    res.render('status', {
      title: 'Status',
      data: dataObj
    });
    client.end();
  });
};


exports.getTime = function(req, res) {
  var client = net.connect(1337, 'localhost');
  client.setEncoding('utf8');

  client.write('status|time\n');

  client.on('data', function(time) {
    console.log(time);
    res.end(time);
    client.end();
  });
};
