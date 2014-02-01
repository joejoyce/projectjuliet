/**
 * Module dependencies.
 */

var express = require('express');
var path = require('path');
var less = require('less-middleware');

var app = express();

/**
 * Load controllers.
 */

var homeController = require('./controllers/home');
var stockController = require('./controllers/stock');
var orderController = require('./controllers/order');
var statusController = require('./controllers/status');
var settingsController = require('./controllers/settings');



app.set('port', 3000);
app.use(express.favicon(__dirname + '/public/images/favicon.ico')); 
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
 * Application routes.
 */

app.get('/', homeController.index);
app.get('/order/:companyId', orderController.getOrder);
app.get('/stock/:companyId', stockController.getStock);
app.get('/status', statusController.getStatus);
app.get('/settings', settingsController.getSettings);


app.listen(app.get('port'), function() {
  console.log("Express server listening on port %d in %s mode", app.get('port'), app.settings.env);
});
