/**
 * Stock Price controller
 */

var models = require('../models');
var symbol = models.Symbol;
var trade = models.Trade;

/**
 * GET /stock-price/:symbol_index
 * Stock price page
 */
exports.index = function(req, res) {
  if (req.params.symbol_index) {
    res.render('stockPrice', {
      title: 'Stock Price',
      symbol_list: symbol.list(),
      symbol: symbol.get(req.params.symbol_index),
      trade_list: trade.getTrades(req.params.symbol_index, null)
    });
  } else {
    res.writeHead(302, { Location: '/' });
  }
};
