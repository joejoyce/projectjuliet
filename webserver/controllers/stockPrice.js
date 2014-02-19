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
  console.log("helloscott");
  if (req.params.symbol_index) {
    console.log("param: " + req.params.symbol_index);
    symbol.get(req.params.symbol_index, function(symbolData) {      
      symbol.list(function(list) {
       res.render('stockPrice', {
          title: 'Stock Price',
          symbol_list: list,
          symbol: symbolData,
          trade_list: trade.getTrades(req.params.symbol_index, null)
        });
      });     
    });    
  } else {
    res.writeHead(302, { Location: '/' });
  }
};
