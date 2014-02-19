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
    res.send({
      kind: 'list',
      data: trade.getTrades(req.params.symbol_index)
    });
  } else {
    res.send(400);
  }
}