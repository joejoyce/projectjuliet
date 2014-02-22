/**
 * Stock Price controller
 */

var models = require('../models');
var symbol = models.Symbol;
var trade = models.Trade;

/**
 * GET /stock-price/:symbol_index
 * Stock price page
 */
exports.index = function(req, res) {
  if (req.params.symbol_index) {
    symbol.get(req.params.symbol_index, function(symbolData) {    
      trade.getTrades(req.params.symbol_index, function(trades) {
        symbolData.price_scale = 1/(Math.pow(10,symbolData.price_scale));
        trade.getCandle(req.params.symbol_index, symbolData.price_scale, function(cData) {
          symbol.list(function(list) { 
            symbol.getStats(req.params.symbol_index, function(stats) {
              res.render('stockPrice', {
                title: 'Stock Price',
                symbol_list: list,
                symbol: symbolData,
                trade_list: trades,
                cData: cData,
                stats: stats
              });
            });
            
          });  
        });        
      });           
    });    
  } else {
    res.writeHead(302, { Location: '/' });
  }
};
