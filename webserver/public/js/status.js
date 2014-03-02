$(document).ready(function() {
    setInterval(function() {
        $.getJSON('/api/v1/status/clients', function(clients) {
            clients.clients.forEach(function(client) {
                var inTable = false;
                $('.clients tr:gt(0)').each(function(i, data) {
                    if($(data.children[0]).text() == client.name) {
                        inTable = true;
                        $(data.children[1]).text(client.totalPackets);
                        $(data.children[2]).text(client.currentPackets);
                        if(client.currentPackets > 200) {
                            $(data.children[0]).removeClass("posc");
                            $(data.children[1]).removeClass("posc");
                            $(data.children[2]).removeClass("posc");
                            $(data.children[0]).addClass("negc");
                            $(data.children[1]).addClass("negc");
                            $(data.children[2]).addClass("negc");
                        }
                        else {
                            $(data.children[0]).removeClass("negc");
                            $(data.children[1]).removeClass("negc");
                            $(data.children[2]).removeClass("negc");
                            $(data.children[0]).addClass("posc");
                            $(data.children[1]).addClass("posc");
                            $(data.children[2]).addClass("posc");
                        }
                    }
                });
                if(!inTable) {
                    // A new client, cool!
                    // Lets add it to the table
                    if(client.currentPackets > 200) {
                        $('.clients tr:last').after('<tr><td class="negc">'+client.name+'</td><td class="negc">'+client.totalPackets+'</td><td class="negc">'+client.currentPackets+'</td></tr>');
                    } else {
                        $('.clients tr:last').after('<tr><td class="posc">'+client.name+'</td><td class="posc">'+client.totalPackets+'</td><td class="posc">'+client.currentPackets+'</td></tr>');
                    }
                }
            });
            if($('.clients tr:gt(0)').length != clients.clients.length) {
                // A client has disconnected!
                // Lets remove him from the table
                var names = [];
                $('.clients tr:gt(0)').each(function(i, data) {
                    names.push($(data.children[0]).text());
                });

                clients.clients.forEach(function(client) {
                    var index = names.indexOf(client.name);
                    names.splice(index, 1);
                });
                // Anything left in names now needs to go:
                $('.clients tr:gt(0)').each(function(i, data) {
                    if(names.indexOf($(data.children[0]).text()) != -1) {
                        $(data).remove();
                    }
                });
            }
        });  
    }, 1000);


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

 			                if(series.data.length > 30) {
                                var graph = series.graph;
                                var area = series.area;
                                var currentShift = (graph && graph.shift) || 0;

                                Highcharts.each([graph, area, series.graphNeg, series.areaNeg], function (shape) {
                                    if (shape) {
                                        shape.shift = currentShift + 1;
                                    }
                                });
                              series.data[0].remove(false, false);
                            }

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
                            var y = Math.abs(Number(response.data.databaseRTTime)/1000000);                            
                             if(series.data.length > 30) {
                                var graph = series.graph;
                                var area = series.area;
                                var currentShift = (graph && graph.shift) || 0;

                                Highcharts.each([graph, area, series.graphNeg, series.areaNeg], function (shape) {
                                    if (shape) {
                                        shape.shift = currentShift + 1;
                                    }
                                });
                                series.data[0].remove(false, false);
                            }

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
                text: 'Database RTT (Milliseconds)'
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
