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
      symbolD.price_scale = 1/(Math.pow(10, symbolD.price_scale)); 
      order.getBids(req.params.symbol_index, function(bid_list) {
        order.getOffers(req.params.symbol_index, function(offer_list) {
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