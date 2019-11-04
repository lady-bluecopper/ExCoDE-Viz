function createChart1(name) {
    $.ajax({
        type: "GET",
        url: '/getChartData?chart=1&name=' + name,
        processData: false,
        contentType: false,
        cache: false,
        timeout: 600000,
        success: function (csv_string) {
            var data = d3.csv.parse(csv_string, function(d) {
                return d;
            });
            let table = d3.select('#general_stats').append('table');
            table.attr("class", "responsive-table highlight centered");
            table.attr("style", "overflow-x: scroll;");
            let thead = table.append('thead');
            let tbody = table.append('tbody');

            thead.append("tr")
                .selectAll("th")
                .data(d3.keys(data[0])).enter()
                .append("th")
                .text(function(d) { return d; });

            var rows = tbody.selectAll('tr')
                .data(data)
                .enter()
                .append('tr');
            var cells = rows.selectAll('td')
                .data(function (d) {
                    return d3.keys(data[0]).map(function (k) {
                        return { value: d[k], column: k};
                    });
                })
                .enter()
                .append('td')
                .text(function (d) { return d.value; });
            console.log("SUCCESS: Statistic 1");
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to load the dataset ' + dbName});
            $('#loading-dataset').hide();
            return false;
        }
    });
    return true;
}

function createChart2(name) {
    $.ajax({
        type: "GET",
        url: '/getChartData?chart=2&name=' + name,
        processData: false,
        contentType: false,
        cache: false,
        timeout: 600000,
        success: function (csv_string) {
            var data = d3.csv.parse(csv_string, function(d) {
                return d;
            });
            let table = d3.select('#snap_stats').append('table');
            table.attr("class", "responsive-table highlight centered");
            table.attr("style", "overflow-x: scroll;");
            let thead = table.append('thead');
            let tbody = table.append('tbody');

            thead.append("tr")
                .selectAll("th")
                .data(d3.keys(data[0])).enter()
                .append("th")
                .text(function(d) { return d; });

            var rows = tbody.selectAll('tr')
                .data(data)
                .enter()
                .append('tr');
            var cells = rows.selectAll('td')
                .data(function (d) {
                    return d3.keys(data[0]).map(function (k) {
                        return { value: d[k], column: k};
                    });
                })
                .enter()
                .append('td')
                .text(function (d) { return d.value; });
            console.log("SUCCESS: Statistic 2");
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to load the dataset ' + dbName});
            $('#loading-dataset').hide();
            return false;
        }
    });
    return true;
}

// Snapshot - Count 
function createChart5(name) {
    $.ajax({
        type: "GET",
        url: '/getChartData?chart=5&name=' + name,
        processData: false,
        contentType: false,
        cache: false,
        timeout: 600000,
        success: function (csv_string) {
            var data = d3.csv.parse(csv_string, function(d) {
                return d;
            });
            data.forEach(function(d) {
                d.Snapshot = parseInt(d.Snapshot);
                d.Count = parseInt(d.Count);
            });
            var margin = {top: 20, right: 80, bottom: 50, left: 70},
                width = $('#num_snap').width() - margin.left - margin.right,
                height = 300 - margin.top - margin.bottom;

            var numTicks = Math.ceil(data.length / 10);
            var ticks = [];
            data.forEach(function(e) {
                if (parseInt(e.Snapshot) % numTicks == 0) {
                    ticks.push(e.Snapshot);
                }
            });
            var x = d3.scale.linear().range([0, width]);
            var xAxis = d3.svg.axis().scale(x).orient("bottom").tickValues(ticks);
            x.domain(d3.extent(data, function(d) { return d.Snapshot; }));

            var y = d3.scale.linear().range([height, 0]);
            var yAxis = d3.svg.axis().scale(y).orient("left").innerTickSize(-width).outerTickSize(0).tickPadding(10);
            y.domain([d3.min(data, function(d) { return d.Count; }), d3.max(data, function(d) { return d.Count; })]);

            var valueline = d3.svg.line()
                .x(function(d) { return x(d.Snapshot); })
                .y(function(d) { return y(d.Count); });

            var chart = d3.select("#num_edges").append("svg")
                        .attr("width", width + margin.left + margin.right)
                        .attr("height", height + margin.top + margin.bottom)
                        .append("g")
                        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            chart.append("path")  
                .attr("class", "line")
                .attr("class", "chart-path")
                .attr("d", valueline(data));

            chart.append("g")     
                .attr("class", "x axis")
                .attr("transform", "translate(0," + height + ")")
                .call(xAxis)
                .append("text")
                .attr('x', width / 2)
                .attr("y", margin.bottom - 5)
                .style("text-anchor", "middle")
                .text("Snapshot");

            chart.append("g")     
                .attr("class", "y axis")
                .call(yAxis)
                .append("text")
                .attr("transform", "rotate(-90)")
                .attr('x', - height / 2)
                .attr("y", - margin.left + 20)
                .style("text-anchor", "middle")
                .text("Count");

            var focus = chart.append("g")
                .attr("class", "mouse-over-effects")
                .style("display", "none");

            focus.append("circle")
                .attr("r", 5)
                .style("stroke", "#752626")
                .style("fill", "none")
                .style("stroke-width", "2px");

            focus.append("rect")
                .attr("width", width)
                .attr("height", height)
                .attr('fill', 'none')
                .attr('pointer-events', 'all')
                .attr("x", 0)
                .attr("y", 0);

            focus.append("text")
                .attr("x", 10)
                .attr("y", 3)
                .style("stroke", "black");

            chart.append("rect")
                .attr("width", width)
                .attr("height", height)
                .attr('pointer-events', 'all')
                .on("mouseover", function() { focus.style("display", null); })
                .on("mouseout", function() { focus.style("display", "none"); })
                .on("mousemove", function() {
                    var mouse = d3.mouse(this);
                    bisect = d3.bisector(function(d) { return d.Snapshot; }).right;
                    x0 = x.invert(mouse[0]);
                    i = bisect(data, x0);
                    if (i == 0) {
                        d = data[i];
                    } else {
                        d0 = data[i - 1];
                        d1 = data[i];
                        d = x0 - d0.Snapshot > d1.Snapshot - x0 ? d1 : d0;    
                    }
                    focus.select("text").text(d.Count);
                    focus.attr("transform", "translate(" + x(d.Snapshot) + "," + y(d.Count) +")");
                });

            console.log("SUCCESS: Statistic 5");
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to load the dataset ' + dbName});
            $('#loading-dataset').hide();
            return false;
        }
    });
    return true;
}

// Degree - Count
function createChart3(name) {
    $.ajax({
        type: "GET",
        url: '/getChartData?chart=3&name=' + name,
        processData: false,
        contentType: false,
        cache: false,
        timeout: 600000,
        success: function (csv_string) {
            var data = d3.csv.parse(csv_string, function(d) {
                return d;
            });
            data.forEach(function(d) {
                d.Degree = parseInt(d.Degree);
                d.Count = parseInt(d.Count);
            });
            var margin = {top: 20, right: 80, bottom: 50, left: 70},
                width = $('#degrees').width() - margin.left - margin.right,
                height = 300 - margin.top - margin.bottom;

            var minX = d3.min(data, function(d) { return d.Degree });
            var maxX = d3.max(data, function(d) { return d.Degree });
            var numTicks = Math.ceil((maxX - minX) / 10);

            var ticks = [];
            for (var i = minX; i < maxX; i += numTicks) {
                ticks.push(i);
            }
            var point_color = "#E54B4B";
            var out_of_range_point = "#491818";

            var xValue = function(d) { return d.Degree },
                xScale = d3.scale.linear().range([0, width]),
                xMap = function(d) { return xScale(xValue(d)) },
                xAxis = d3.svg.axis().scale(xScale).orient("bottom").tickValues(ticks);
                xScale.domain([minX, maxX]);
            var yValue = function(d) { return d.Count },
                yScale = d3.scale.linear().range([height, 0]),
                yMap = function(d) { 
                    if (height + yScale(d.Count) < 0) {
                        return 0;
                    }
                    return yScale(yValue(d));
                },
                yAxis = d3.svg.axis().scale(yScale).orient("left").innerTickSize(-width).outerTickSize(0).tickPadding(10);
                yScale.domain([d3.min(data, function(d) { return d.Count }), d3.max(data, function(d) { return d.Count })]);
            var svg = d3.select('#degrees').append("svg")
                            .attr("width", width + margin.left + margin.right)
                            .attr("height", height + margin.top + margin.bottom)
                            .append("g")
                            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
            var focus = svg
                .append("g")
                .attr("class", "mouse-over-effects")
                .style("display", "none")
                .attr("transform", "translate (" + (width - 20) + "," + (10) + ")");
            var focus_area = focus
                .append("rect")
                .attr("width", 10)
                .attr("height", 10)
                .attr('fill', 'none');
            focus.append("text")
                .attr("x", 5)
                .attr("y", 5)
                .attr("stroke", "black")
                .text("");

            svg.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + height + ")")
                .call(xAxis)
                .append("text")
                .attr('x', width / 2)
                .attr("y", margin.bottom - 5)
                .style("text-anchor", "middle")
                .text("Degree");
            svg.append("g")
                .attr("class", "y axis")
                .call(yAxis)
                .append("text")
                .attr("transform", "rotate(-90)")
                .attr('x', - height / 2)
                .attr("y", - margin.left + 20)
                .style("text-anchor", "middle")
                .text("Count");
            svg.selectAll(".dot")
                .data(data)
                .enter()
                .append("circle")
                .attr("class", "dot")
                .attr("r", 3)
                .attr("cx", xMap)
                .attr("cy", yMap)
                .style("fill", function(d) {
                    if (height + yScale(d.Count) < 0) {
                        return out_of_range_point;
                    }
                    return point_color;
                })
                .style("stroke", "none") 
                .on("mouseover", function(d) {
                    focus.style("display", null);
                    focus.select("text")
                        .text("")
                        .text("(" + xValue(d) + ", " + yValue(d) + ")");
                    if (height + yScale(d.Count) < 0) {
                        focus.attr("transform", "translate(" + (xScale(d.Degree) - 5) + ",-10)");
                    } else {
                        focus.attr("transform", "translate(" + (xScale(d.Degree) - 5) + "," + (yScale(d.Count) - 10) +")");
                    }
                })
                .on("mouseout", function(d) { focus.style("display", "none") });
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to load the dataset ' + dbName});
            $('#loading-dataset').hide();
            return false;
        }
    });
    return true;
}

function createChart4(name) {
    $.ajax({
        type: "GET",
        url: '/getChartData?chart=4&name=' + name,
        processData: false,
        contentType: false,
        cache: false,
        timeout: 600000,
        success: function (csv_string) {
            var data = d3.csv.parse(csv_string, function(d) {
                return d;
            });
            data.forEach(function(d) {
                d.Existence = parseInt(d.Existence);
                d.Count = parseInt(d.Count);
            });
            var margin = {top: 20, right: 80, bottom: 50, left: 70},
                width = $('#num_snap').width() - margin.left - margin.right,
                height = 300 - margin.top - margin.bottom;

            var minX = d3.min(data, function(d) { return d.Existence });
            var maxX = d3.max(data, function(d) { return d.Existence });
            var numTicks = Math.ceil((maxX - minX) / 10);

            var ticks = [];
            for (var i = minX; i < maxX; i += numTicks) {
                ticks.push(i);
            }
            
            var point_color = "#E54B4B";
            var out_of_range_point = "#491818";

            var xValue = function(d) { return d.Existence },
                xScale = d3.scale.linear().range([0, width]),
                xMap = function(d) { return xScale(xValue(d)) },
                xAxis = d3.svg.axis().scale(xScale).orient("bottom").tickValues(ticks);
                xScale.domain([d3.min(data, function(d) { return d.Existence }), d3.max(data, function(d) { return d.Existence })]);
            var yValue = function(d) { return d.Count },
                yScale = d3.scale.linear().range([height, 0]),
                yMap = function(d) { 
                    if (height + yScale(d.Count) < 0) {
                        return 0;
                    }
                    return yScale(yValue(d));
                },
                yAxis = d3.svg.axis().scale(yScale).orient("left").innerTickSize(-width).outerTickSize(0).tickPadding(10);
                yScale.domain([d3.min(data, function(d) { return d.Count }), d3.max(data, function(d) { return d.Count })]);
            
            var svg = d3.select('#num_snap').append("svg")
                            .attr("width", width + margin.left + margin.right)
                            .attr("height", height + margin.top + margin.bottom)
                            .append("g")
                            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
            var focus = svg
                .append("g")
                .attr("class", "mouse-over-effects")
                .style("display", "none")
                .attr("transform", "translate (" + (width - 20) + "," + (10) + ")");
            var focus_area = focus
                .append("rect")
                .attr("width", 10)
                .attr("height", 10)
                .attr('fill', 'none');
            focus.append("text")
                .attr("x", 5)
                .attr("y", 5)
                .attr("stroke", "black")
                .text("");

            svg.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + height + ")")
                .call(xAxis)
                .append("text")
                .attr('x', width / 2)
                .attr("y", margin.bottom - 5)
                .style("text-anchor", "middle")
                .text("Existence Count");
            svg.append("g")
                .attr("class", "y axis")
                .call(yAxis)
                .append("text")
                .attr("transform", "rotate(-90)")
                .attr('x', - height / 2)
                .attr("y", - margin.left + 20)
                .style("text-anchor", "middle")
                .text("Count");

            svg.selectAll(".dot")
                .data(data)
                .enter()
                .append("circle")
                .attr("class", "dot")
                .attr("r", 3)
                .attr("cx", xMap)
                .attr("cy", yMap)
                .style("fill", function(d) {
                    if (height + yScale(d.Count) < 0) {
                        return out_of_range_point;
                    }
                    return point_color;
                })
                .style("stroke", "none") 
                .on("mouseover", function(d) {
                    focus.style("display", null);
                    focus.select("text")
                        .text("")
                        .text("(" + xValue(d) + ", " + yValue(d) + ")");
                    if (height + yScale(d.Count) < 0) {
                        focus.attr("transform", "translate(" + (xScale(d.Existence) - 5) + ",-10)");
                    } else {
                        focus.attr("transform", "translate(" + (xScale(d.Existence) - 5) + "," + (yScale(d.Count) - 10) +")");
                    }
                })
                .on("mouseout", function(d) { focus.style("display", "none") });

            console.log("SUCCESS: Statistic 4");
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to load the dataset ' + dbName});
            $('#loading-dataset').hide();
            return false;
        }
    });
    return true;
}

// Snapshot - Degree
function createChart6(name) {
    $.ajax({
        type: "GET",
        url: '/getChartData?chart=6&name=' + name,
        processData: false,
        contentType: false,
        cache: false,
        timeout: 600000,
        success: function (csv_string) {
            var data = d3.csv.parse(csv_string, function(d) {
                    return d;
                });
                data.forEach(function(d) {
                    d.Snapshot = parseInt(d.Snapshot);
                    d.Max = parseInt(d.Max);
                    d.Avg = parseFloat(d.Avg);
                    d.Min = parseInt(d.Min);
                });
                console.log($('#num_snap').width());
                var margin = {top: 20, right: 80, bottom: 50, left: 60},
                    width = $('#num_snap').width() - margin.left - margin.right,
                    height = 300 - margin.top - margin.bottom;

                var numTicks = parseInt(data.length / 10);
                var ticks = [];
                data.forEach(function(e) {
                    if (parseInt(e.Snapshot) % numTicks == 0) {
                        ticks.push(e.Snapshot);
                    }
                });

                var x = d3.scale.linear().range([0, width]);
                var xAxis = d3.svg.axis().scale(x).orient("bottom").tickValues(ticks);
                x.domain(d3.extent(data, function(d) { return d.Snapshot; }));

                var y = d3.scale.linear().range([height, 0]);
                var yAxis = d3.svg.axis().scale(y).orient("left").innerTickSize(-width).outerTickSize(0).tickPadding(10);
                y.domain([d3.min(data, function(d) { return d.Min; }), d3.max(data, function(d) { return d.Max; })]);

                var chart = d3.select("#degrees-multi").append("svg")
                            .attr("width", width + margin.left + margin.right)
                            .attr("height", height + margin.top + margin.bottom)
                            .append("g")
                            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");


                var color = d3.scale.category10();
                color.domain(d3.keys(data[0]).filter(function(key) { return key !== "Snapshot"; }));

                var functions = color.domain().map(function(name) {
                    return {
                        name: name,
                        values: data.map(function(d) {
                            return {
                                snap: d.Snapshot,
                                val: +d[name]
                            };
                        })
                    };
                });

                var valueline = d3.svg.line()
                    .x(function(d) { return x(d.snap); })
                    .y(function(d) { return y(d.val); });

                chart.append("g")     
                    .attr("class", "x axis")
                    .attr("transform", "translate(0," + height + ")")
                    .call(xAxis)
                    .append("text")
                    .attr('x', width / 2)
                    .attr("y", margin.bottom - 5)
                    .style("text-anchor", "middle")
                    .text("Snapshot");

                chart.append("g")     
                    .attr("class", "y axis")
                    .call(yAxis)
                    .append("text")
                    .attr("transform", "rotate(-90)")
                    .attr('x', - height / 2)
                    .attr("y", - margin.left + 20)
                    .style("text-anchor", "middle")
                    .text("Degree");

                var func = chart.selectAll(".func")
                    .data(functions)
                    .enter().append("g")
                    .attr("class", "func");

                func.append("path")
                    .attr("class", "line")
                    .attr("class", "chart-path")
                    .attr("d", function(d) {
                        return valueline(d.values);
                    })
                    .style("stroke", function(d) {
                        return color(d.name);
                    });

                var legendHeight = functions.length * 18 + 10;
                var legendWidth = 55;

                var legend = chart
                    .append('g')
                    .attr("transform", "translate (5,0)")
                    .attr('class', 'legend');

                var lb = legend.append('rect')
                    .attr("class", "legend-box")
                    .attr("width", legendWidth)
                    .attr("height", legendHeight)
                    .attr('rx', 5)
                    .attr('ry', 5);

                var li = legend.append("g")
                    .attr("transform", "translate (8,5)")
                    .attr("class", "legend-items");

                li.selectAll("rect")
                    .data(functions)
                    .enter()
                    .append("rect")
                    .attr("y", function(d, i) { return 5 + (i * 18) })
                    .attr("width", 10)
                    .attr("height", 10)
                    .style("fill", function(d) { return color(d.name) });

                li.selectAll("text")
                    .data(functions)
                    .enter()
                    .append("text")
                    .attr("x", 15)
                    .attr("y", function(d, i) { return 5 + (i * 18) + 9 })
                    .text(function(d) { return d.name })
                    .style('stroke', 'black');

                var mouseG = chart.append("g")
                    .attr("class", "mouse-over-effects");

                mouseG.append("path")
                    .attr("class", "mouse-line")
                    .style("stroke", "black")
                    .style("stroke-width", "1px")
                    .style("opacity", "0");

                var mousePerLine = mouseG.selectAll('.mouse-per-line')
                    .data(functions)
                    .enter()
                    .append("g")
                    .attr("class", "mouse-per-line");

                mousePerLine.append("circle")
                    .attr("r", 5)
                    .style("stroke", function(d) {
                        return color(d.name);
                    })
                    .style("fill", "none")
                    .style("stroke-width", "2px")
                    .style("opacity", "0");

                mousePerLine.append("text")
                    .attr("transform", "translate(10,3)")
                    .style("stroke", "black");

                mouseG.append('rect')
                    .attr('width', width) 
                    .attr('height', height)
                    .attr('fill', 'none')
                    .attr('pointer-events', 'all')
                    .on('mouseout', function() {
                        d3.select(".mouse-line")
                          .style("opacity", "0");
                        d3.selectAll(".mouse-per-line circle")
                          .style("opacity", "0");
                        d3.selectAll(".mouse-per-line text")
                          .style("opacity", "0");
                    })
                    .on('mouseover', function() { 
                        d3.select(".mouse-line")
                          .style("opacity", "1");
                        d3.selectAll(".mouse-per-line circle")
                          .style("opacity", "1");
                        d3.selectAll(".mouse-per-line text")
                          .style("opacity", "1");
                    })
                    .on('mousemove', function() { 
                        var mouse = d3.mouse(this);
                        d3.select(".mouse-line")
                            .attr("d", function() {
                                var d = "M" + mouse[0] + "," + height;
                                d += " " + mouse[0] + "," + 0;
                                return d;
                        });

                        d3.selectAll(".mouse-per-line")
                            .attr("transform", function(d, i) {
                                var x0 = x.invert(mouse[0]),
                                bisect = d3.bisector(function(d) { return d.snap; }).right;
                                idx = bisect(d.values, x0);
                                if (idx == 0) {
                                    d = functions[i].values[idx];
                                }
                                else {
                                    d0 = functions[i].values[idx - 1];
                                    d1 = functions[i].values[idx];
                                    d = x0 - d0.snap > d1.snap - x0 ? d1 : d0;     
                                }
                                d3.select(this).select('text').text(d.val);
                                return "translate(" + x(d.snap) + "," + y(d.val) +")";

                        });

                    });

            console.log("SUCCESS: Statistic 6");
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to load the dataset ' + dbName});
            $('#loading-dataset').hide();
            return false;
        }
    });
    return true;
}