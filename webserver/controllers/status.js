/**
 * Status controller
 */

var models = require('../models');
var status = models.Status;
var symbol = models.Symbol;

/**
 * GET /status
 * Status page
 */
exports.index = function(req, res) {
  symbol.list(function(list) {
    res.render('status', {
      title: 'System Status',
      symbol_list: list,
      client_list: status.listClients()
    });  
  });
};
  

/**
 * GET /api/v1/status/clients
 * Get all cluster clients
 */
exports.clients = function(req, res) {
  res.send({
    kind: 'list',
    data: status.listClients()
  });
};

/**
 * GET /api/v1/status/time
 * Get the current simulation system time
 */
exports.time = function(req, res) {
  status.getTime(function(time) {
   res.send({
      kind: 'time',
      data: time
    });
  }); 
};
