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
    symbol.get(req.params.symbol_index, function(symbolD) {
      symbolD.priceScale = 1/(Math.pow(10, symbolD.priceScale)); 
      order.getOrders(req.params.symbol_index, [{field: 'is_ask', value: 0}], function(bid_list) {
        order.getOrders(req.params.symbol_index, [{field: 'is_ask', value: 1}], function(offer_list) {
          symbol.list(function(list) {
            res.render('orderBook', {
              title: 'Order Book',
              symbol_list: list,
              symbol: symbolD,
              bid_list: bid_list,
              offer_list: offer_list
            });
          });         
        });
      });
    });   
  } else {
    res.writeHead(302, { Location: '/' });
  }
};