$(document).ready(function() {
 	var myData = new Array([10, 20], [15, 10], [20, 30], [25, 10], [30, 5]);
	var myChart = new JSChart('chartcontainer', 'line');
	myChart.setDataArray(myData);
	myChart.draw();	
});
