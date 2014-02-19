/**
 * Settings controller
 */

var models = require('../models');
var symbol = models.Symbol;

/**
 * GET /settings
 * System settings page
 */
exports.index = function(req, res) {
  res.render('settings', {
    title: 'Settings',
    symbol_list: symbol.list()
  });
};
