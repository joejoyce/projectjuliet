/**
 * GET /contact
 * Contact form page.
 */

exports.getStock = function(req, res) {
  res.render('stock', {
    title: 'Stock'
  });
};
