$(document).ready(function() {
    Highcharts.setOptions({
        global: {
            useUTC: false
        }
    });

    $('#throughput-chart').highcharts({
        chart: {
            type: 'spline',
            animation: Highcharts.svg, // don't animate in old IE
            marginRight: 10,
            events: {
                load: function() {
                    var series = this.series[0];
                    setInterval(function() {
                        $.getJSON('/api/v1/status/throughput', function(response) {
                            var x = (new Date()).getTime();
                            var y = Number(response.data);                            
                            series.addPoint([x, y], true, false);
                        });    
                    }, 1000);
                }
            }
        },
        title: {
            text: 'System Throughput'
        },
        xAxis: {
            type: 'datetime',
            tickPixelInterval: 150
        },
        yAxis: {
            title: {
                text: 'Packets Per Second'
            },
            plotLines: [{
                value: 0,
                width: 1,
                color: '#808080'
            }]
        },
        tooltip: {
            formatter: function() {
                    return '<b>'+ this.series.name +'</b><br/>'+
                    Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x) +'<br/>'+
                    Highcharts.numberFormat(this.y, 2);
            }
        },
        legend: {
            enabled: false
        },
        exporting: {
            enabled: false
        },
        series: [{
            name: 'Throughput',
            data: []
        }]
    });

 $('#latency-chart').highcharts({
        chart: {
            type: 'spline',
            animation: Highcharts.svg, // don't animate in old IE
            marginRight: 10,
            events: {
                load: function() {
                    var series = this.series[0];
                    setInterval(function() {
                        $.getJSON('/api/v1/status/latency', function(response) {
                            var x = (new Date()).getTime();
                            var y = Math.abs(Number(response.data.networkRTTime)/1000000);                            
                            series.addPoint([x, y], true, false);
                        });    
                    }, 1000);
                }
            }
        },
        title: {
            text: 'System Latency'
        },
        xAxis: {
            type: 'datetime',
            tickPixelInterval: 150
        },
        yAxis: {
            title: {
                text: 'RTT (Milliseconds)'
            },            
            plotLines: [{
                value: 0,
                width: 1,
                color: '#808080'
            }]
        },
        tooltip: {
            formatter: function() {
                    return '<b>'+ this.series.name +'</b><br/>'+
                    Highcharts.dateFormat('%Y-%m-%d %H:%M:%S', this.x) +'<br/>'+
                    Highcharts.numberFormat(this.y, 2);
            }
        },
        legend: {
            enabled: false
        },
        exporting: {
            enabled: false
        },
        series: [{
            name: 'Latency',
            color: '#FF1000',
            data: []
        }]
    });
});