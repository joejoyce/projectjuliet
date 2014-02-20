/**
 * Trade controller
 */

var models = require('../models');
var trade = models.Trade;

/**
 * GET /trades/:symbol_index
 * Gets all of the trades for a specific symbol index
 */

exports.index = function(req, res) {
  if (req.params.symbol_index) {
    trade.getTrades(req.params.symbol_index, function(trades) {
		  res.send({
		    kind: 'list',
		    data: trades
		  });
    });   
  } else {
    res.send(400);
  }
};