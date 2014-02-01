/**
 * GET /stock
 * Stock price graph for specific company
 */

exports.getStock = function(req, res) {
 var companyId = req.param("companyId");
	console.log(companyId);
  res.render('stock', {
    title: 'Stock',
    companyId: companyId
  });
};
