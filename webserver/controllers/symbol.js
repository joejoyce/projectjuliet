/**
 * Symbol controller
 */

var models = require('../models');
var symbol = models.Symbol;

/**
 * GET /symbol/:symbol_index
 * Get the symbol of the specified symbol index
 */
exports.index = function(req, res) {
  if (req.params.symbol_index) {
    res.send({
      kind: 'symbol',
      data: symbol.get(req.params.symbol_index)
    });
  } else {
    res.send(400);
  }
}

/**
 * GET /symbols
 * Get list of all known symbols
 */
exports.list = function(req, res) {
  res.send({
    kind: 'list',
    data: symbol.list()
  });
}