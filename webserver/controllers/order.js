/**
 * Order controller
 */

var models = require('../models');
var order = models.Order;

/**
 * GET /orders/:symbol_index
 * Get all of the orders for a specific symbol index
 */
exports.index = function(req, res) {
  if (req.params.symbol_index) {
    res.send({
      kind: 'list',
      data: order.getOrders(req.params.symbol_index)
    });
  } else {
    res.send(400);
  }
}

/**
 * GET /orders/bids/:symbol_index
 * Get all of the bid orders for a specific symbol index
 */
exports.bids = function(req, res) {
  if (req.params.symbol_index) {
    order.getOrders(req.params.symbol_index, [{field: 'is_ask', value: 0}], function(orders) {
      res.send({
        kind: 'list',
        data: orders 
      });
    });   
  } else {
    res.send(400);
  }
}

/**
 * GET /orders/offers/:symbol_index
 * Get all of the offer orders for a specific symbol index
 */
exports.offers = function(req, res) {
  if (req.params.symbol_index) {
    order.getOrders(req.params.symbol_index, [{field: 'is_ask', value: 1}], function(orders) {
      res.send({
        kind: 'list',
        data: orders
      });
    });  
  } else {
    res.send(400);
  }
}