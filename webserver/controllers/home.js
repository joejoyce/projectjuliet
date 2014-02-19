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
  res.render('home', {
    title: 'Home',
    symbol_list: symbol.list()
  });
};
