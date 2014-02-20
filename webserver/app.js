/**
 * Module dependencies.
 */

var express = require('express');
var path = require('path');
var less = require('less-middleware');

var app = express();
app.locals.moment = require('moment');

app.set('port', 80);
app.use(express.favicon(__dirname + '/public/img/favicon.ico')); 
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'jade');
app.use(express.favicon());
app.use(express.logger('dev'));
app.use(express.cookieParser());
app.use(express.json());
app.use(express.urlencoded());
app.use(express.methodOverride());
app.use(less({ src: __dirname + '/public', compress: true }));
app.use(app.router);
app.use(express.static(path.join(__dirname, 'public'), { }));
app.use(function(req, res) {
  res.render('404', { status: 404 });
});
app.use(express.errorHandler());

/**
 * Load controllers
 */
var homeController = require('./controllers/home');
var settingsController = require('./controllers/settings');
var stockPriceController = require('./controllers/stockPrice');
var orderBookController = require('./controllers/orderBook');
var symbolController = require('./controllers/symbol');
var orderController = require('./controllers/order');
var statusController = require('./controllers/status');

/**
 * Application routes
 */
// Pages
app.get('/', homeController.index);
app.get('/stock-price/:symbol_index', stockPriceController.index);
app.get('/order-book/:symbol_index', orderBookController.index);
app.get('/status', statusController.index);
app.get('/settings', settingsController.index);

// API
app.get('/api/v1/symbol/:symbol_index', symbolController.index);
app.get('/api/v1/symbols', symbolController.list);
app.get('/api/v1/orders/:symbol_index', orderController.index);
app.get('/api/v1/orders/bids/:symbol_index', orderController.bids);
app.get('/api/v1/orders/offers/:symbol_index', orderController.offers);
app.get('/api/v1/status/clients', statusController.clients);
app.get('/api/v1/status/time', statusController.time);

app.listen(app.get('port'), function() {
  console.log("Express server listening on port %d in %s mode", app.get('port'), app.settings.env);
});
