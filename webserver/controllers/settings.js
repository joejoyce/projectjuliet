/**
 * Settings controller
 */

var models = require('../models');
var symbol = models.Symbol;
var settings = models.Settings;

/**
 * GET /settings
 * System settings page
 */
exports.index = function(req, res) {
	symbol.list(function(list) {
		settings.getSkip(function(skip) {
			res.render('settings', {
		    title: 'Settings',
		    symbol_list: list,
		    skip: skip
		  });
		});	  
	});
};

exports.pause = function(req, res) {
	settings.pause();
};

exports.restart = function(req, res) {
  settings.restart();
};

exports.setskip = function(req, res) {
	settings.setSkip(req.params.skip);
};