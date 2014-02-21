/**
 * Order Book controller
 */

var async = require('async');

var models = require('../models');
var symbol = models.Symbol;
var order = models.Order;

/**
 * GET /order-book Order book page
 */
exports.index = function(req, res) {
	if (req.params.symbol_index) {
		async.parallel(
		{
			symbol_list: function(callback) {
				symbol.list(function(list) {
					callback(null, list);
				});
			},
			symbolD: function(callback) {
				symbol.get(req.params.symbol_index, function(symbolD) {
					callback(null, 1 / (Math.pow(10, symbolD.price_scale)));
				});
			},
			bid_list: function(callback) {
				order.getBids(req.params.symbol_index, function(bid_list) {
					callback(null, bid_list);
				});
			},
			offer_list: function(callback) {
				order.getOffers(req.params.symbol_index, function(offer_list) {
					callback(null, offer_list);
				});
			}
		},
		function(error, results) {
			if (!error) {
				res.render('orderBook', {
					title: 'Order Book',
					symbol_list: results.symbol_list,
					symbol: results.symbolD,
					bid_list: results.bid_list,
					offer_list: results.offer_list
				});
			} else {
				res.writeHead(500, {
					Location: '/'
				})
			}
		});
	} else {
		res.writeHead(302, {
			Location: '/'
		});
	}
};