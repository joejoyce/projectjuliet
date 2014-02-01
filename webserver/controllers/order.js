/**
 * GET /order
 * Gets an order book for a specific company.
 */

exports.getOrder = function(req, res) {
	var companyId = req.param("companyId");
	console.log(companyId);
  res.render('order', {
    title: 'Order',
    companyId: companyId
  });
};
