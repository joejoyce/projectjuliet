$(document).ready(function() {
 	console.log("test");
 	alert("gay");
	var myData = new Array([10, 20], [15, 10], [20, 30], [25, 10], [30, 5]);
	alert("gay1");
	var myChart = new JSChart('chartcontainer', 'line');
	alert("gay2");
	myChart.setDataArray(myData);
	alert("gay3");
	
	myChart.draw();
	
});
