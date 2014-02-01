/**
 * GET /
 * Home page.
 */

exports.index = function(req, res) {
	console.log("hello");
  res.render('home', {
    title: 'Home'
  });
};
