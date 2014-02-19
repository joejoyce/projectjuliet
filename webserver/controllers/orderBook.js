/**
 * Order Book controller
 */

var models = require('../models');
var symbol = models.Symbol;
var order = models.Order;

/**
 * GET /order-book
 * Order book page
 */
exports.index = function(req, res) {
  if (req.params.symbol_index) {
    res.render('orderBook', {
      title: 'Order Book',
      symbol_list: symbol.list(),
      symbol: symbol.get(req.params.symbol_index),
      bid_list: order.getOrders(req.params.symbol_index, [{field: 'is_ask', value: 0}]),
      offer_list: order.getOrders(req.params.symbol_index, [{field: 'is_ask', value: 1}])
    });
  } else {
    res.writeHead(302, { Location: '/' });
  }
};