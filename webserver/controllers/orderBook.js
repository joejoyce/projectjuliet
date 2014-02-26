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
					if(!symbolD) {
       			callback({error: "error"}, null);
       			return;
      		}   
					symbolD.price_scale = 1 / (Math.pow(10, symbolD.price_scale));
					callback(null, symbolD);
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
			},
			statistics: function(callback) {
				symbol.getStats(req.params.symbol_index, function(stats) {
					callback(null, stats);
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
					offer_list: results.offer_list,
					stats: results.statistics
				});
			} else {
				res.redirect('/');
			}
		});
	} else {
		res.redirect('/');
	}
};