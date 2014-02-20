/**
 * Home controller
 */

var models = require('../models');
var symbol = models.Symbol;

/**
 * GET /
 * Home page
 */
exports.index = function(req, res) {
	symbol.list(function(list) {
	 res.render('home', {
	    title: 'Home',
	    symbol_list: list
	  });
	}); 
};
