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
    status.getStatus(function(status) {
      res.render('status', {
        title: 'System Status',
        symbol_list: list,
        client_list: status.clients,
        throughput: status.throughput,
        loadAv: status.loadAv,
        total_orders: status.total_orders,
        total_trades: status.total_trades,
        total_symbols: status.total_symbols
      });  
    });    
  });
};
  

/**
 * GET /api/v1/status/clients
 * Get all cluster clients
 */
exports.clients = function(req, res) {
  status.getClients(function(clients) {
   res.send({
      clients: clients
    });
  }); 
};

/**
 * GET /api/v1/status/time
 * Get the current system time
 */
exports.time = function(req, res) {
  status.getTime(function(time) {
   res.send({
      kind: 'time',
      data: time
    });
  }); 
};


/**
 * GET /api/v1/status/throughput
 * Get the current packet throughput
 */
exports.throughput = function(req, res) {
  status.getThroughput(function(throughput) {
    res.send({
      data: throughput
    });
  });
};

/**
 * GET /api/v1/status/latency
 * Get the current system latency
 */
exports.latency = function(req, res) {
  status.getLatency(function(latency) {
    res.send({
      data: latency
    });
  });
};
