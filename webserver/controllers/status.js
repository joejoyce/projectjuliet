/**
 * GET /status
 * System status page
 */

exports.getStatus = function(req, res) {
  res.render('status', {
    title: 'Status'
  });
};
