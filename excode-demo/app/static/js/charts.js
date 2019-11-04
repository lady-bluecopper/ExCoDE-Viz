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
            table.attr("id", "graph-summary-table");
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
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to load the dataset ' + dbName});
            $('#loading-dataset').hide();
        }
    });
}

function generate_chart_in_graph(data) {
    var margin = {top: 10, right: 10, bottom: 10, left: 10},
        width = $("#results-card").parent().width() - margin.left - margin.right,
        height = $("#results-card").parent().width() - margin.top - margin.bottom;

    var focus_node = null, highlight_node = null;
    var highlight_color = "#45474d";
    var highlight_color_selected = "#722525";
    var in_sub_color = "#AEB4A9";
    var not_in_sub_color = "#FFFFFF";
    var highlight_trans = 0.1;
    var linkDistance = 100;
    var gradual_colors = ["#2b2640", "#564d80", "#777099", "#9994b2", "#bbb7cc", "#dddbe5"];

    var force = d3.layout.force()
        .nodes(data.nodes)
        .links(data.edges)
        .size([width, height])
        .linkDistance([linkDistance])
        .charge([-300])
        .linkStrength(0.1)
        .theta(0.1)
        .gravity(0.08)
        .start();

    var zoom = d3.behavior.zoom()
        .on("zoom", zoomed);

    var drag = d3.behavior.drag()
        .origin(function(d) { return d; })
        .on("dragstart", dragstarted)
        .on("drag", dragged)
        .on("dragend", dragended);

    var graph = d3.select("#results-card")
        .append("svg")
        .attr({"width":width,
               "height":height,
               "style":"background-color:#E8EEF2"})
        .append("g")
        .call(zoom);

    var rect = graph.append("rect")
            .attr("width", width)
            .attr("height", height)
            .style("fill", "none")
            .style("pointer-events", "all");

    var container = graph.append("g");

    var edgepaths = container
        .append("g")
        .selectAll(".edgepath")
        .data(data.edges)
        .enter()
        .append("path")
        .attr({"d": function(d) { return 'M ' + d.source.x + ' ' + d.source.y + ' L ' + d.target.x + ' ' + d.target.y },
               "id": function(d,i) { return 'edgepath' + d.source.name + "-" + d.target.name },
               "class": "edgepath"})
        .style("stroke", function(e) {
            if ('sids' in e) {
                return in_sub_color;
            }
            return not_in_sub_color;
        });

    var nodes = container
        .append("g")
        .selectAll("circle")
        .data(data.nodes)
        .enter()
        .append("circle")
        .attr("r", 8)
        .attr("id", function(d) { return 'node' + d.name })
        .style("fill", function(d,i){
            if ('sids' in d) {
                var sids = d.sids.split(",")
                if (sids.length > 1) {
                    return "#000000";
                }
                var id = parseInt(sids[0].charAt(0)) - 1;
                return gradual_colors[id];
            } 
            return not_in_sub_color;
        })
        .style("stroke", function(d){
            if ('sids' in d) {
                return in_sub_color;
            }
            return not_in_sub_color;
        })
        .call(drag);

    nodes.on("dblclick.zoom", function(d) { 
        var dcx = (window.innerWidth / 2 - d.x * zoom.scale());
        var dcy = (window.innerHeight / 2 - d.y * zoom.scale());
        zoom.translate([dcx,dcy]);
        g.attr("transform", "translate("+ dcx + "," + dcy  + ")scale(" + zoom.scale() + ")");
    });

    var nodelabels = container
        .selectAll(".nodelabel") 
        .data(data.nodes)
        .enter()
        .append("text")
        .attr({"x": function(d){ return d.x },
              "dx": 10,
              "y": function(d){ return d.y },
              "dy": ".60em",
              "class": "nodelabel"
        })
        .text(function(d){ return d.name });

    var legendHeight = data.densities.length * 18 + 10;
    var legendWidth = 80;
    var nodeLegendWidth = 140;
    var nodeLegendHeight = 40;

    var legend = graph
        .append("g")
        .attr("transform", "translate (" + (width - legendWidth - 10) + "," + (height - legendHeight - 24) + ")")
        .attr("class", "legend");
    
    legend.selectAll("text")
        .data(['Density'])
        .enter()
        .append("text")
        .attr("class", "legend-title")
        .attr("x", 18)
        .attr("y", 8)
        .text(function(d) { return d });

    var lb = legend.append("rect")
        .attr("transform", "translate (0,14)")
        .attr("class", "legend-box")
        .attr("width", legendWidth)
        .attr("height", legendHeight)
        .attr('rx', 5)
        .attr('ry', 5);

    var li = legend.append("g")
        .attr("transform", "translate (8,5)")
        .attr("class", "legend-items");

    li.selectAll("rect")
        .data(data.densities)
        .enter()
        .append("rect")
        .attr("y", function(d, i) { return legendHeight - 10 - (i * 18);})
        .attr("width", 10)
        .attr("height", 10)
        .style("fill", function(d) { return gradual_colors[parseInt(d.id) - 1] });

    li.selectAll("text")
        .data(data.densities)
        .enter()
        .append("text")
        .attr("x", 15)
        .attr("y", function(d, i) { return legendHeight - 10 - (i * 18) + 9; })
        .text(function(d) {
                if (d.id == "6") {
                    return "<= " + d.value; 
                } 
                return d.value;
            })
        .style('stroke', 'black');

    var node_legend = graph
        .append("g")
        .attr("class", "mouse-over-effects")
        .attr("class", "legend")
        .attr("transform", "translate (" + (width - nodeLegendWidth - 10) + "," + (10) + ")");

    var node_legend_rect = node_legend.append("rect")
        .attr("width", nodeLegendWidth)
        .attr("height", nodeLegendHeight)
        .attr("rx", 5)
        .attr("ry", 5);

    node_legend.append("text")
        .attr("x", 15)
        .attr("y", 15)
        .attr("stroke", "black")
        .text("Hover over a node.");

    var linkedByIndex = {};
    data.edges.forEach(function(d) {
        linkedByIndex[d.source.index + "," + d.target.index] = 1;
    });

    function isConnected(a, b) {
        return linkedByIndex[a.index + "," + b.index] || linkedByIndex[b.index + "," + a.index] || a.index == b.index;
    }

    nodes
        .on("mouseover", function(d){ highlight(d); })
        .on("mousedown", function(d) { dragging(d); })
        .on("mouseout", function(d){ nohighlight(); });

    d3.select(window).on("mouseup", function() {
        if (focus_node !== null) {
            focus_node = null;
            nodes.style("opacity", 1);
            nodelabels.style("opacity", 1);
            edgepaths.style("opacity", 1);
        }
    
        if (highlight_node === null) {
            nohighlight();
        }
    });

    function highlight(d) {
        if (focus_node !== null) {
            d = focus_node;
        }
        highlight_node = d;

        let sids = ('sids' in d) ? d.sids.split(",") : [];
        let start = 15;
        if (sids.length > 2) {
            node_legend_rect.attr("height", nodeLegendHeight + 16 * (sids.length - 1));   
        }
        if (sids.length > 0) {
            var current_text = node_legend.select("text")
                .text("")
                .append("tspan")
                .attr("x", 15)
                .attr("y", start)
                .text("Node ID : " + d.name)
                .append("tspan")
                .attr("x", 15)
                .attr("y", start += 16)
                .text("Appears in : " + (parseInt(sids[0].substring(1)) + 1));

            for (var i = 1; i < sids.length; i++) {
                if (sids[i] !== sids[0]) {
                    current_text.append("tspan")
                        .attr("x", 86)
                        .attr("y", start += 16)
                        .text(parseInt(sids[i].substring(1)) + 1);
                }
            }
        } else {
            node_legend.select("text")
                .text("")
                .append("tspan")
                .attr("x", 15)
                .attr("y", start)
                .text("Node ID : " + d.name);
        }

        nodes.style("stroke", function(o) { 
                var n = '#node' + o.name;
                var bool = ($(n).css('stroke') === "rgb(229, 75, 75)");
                if (isConnected(d, o)) {
                    if (bool) {
                        return highlight_color_selected;
                    }
                    return highlight_color;
                } else if (bool) {
                    return "#E54B4B";
                }
                if ('sids' in o) {
                    return in_sub_color;
                }
                return not_in_sub_color;
            })
            .style("stroke-width", function(o) { return isConnected(d, o) ? "3px" : "1.5px"; });
        nodelabels
            .style("font-weight", function(o) { return isConnected(d, o) ? "bold" : "normal";});
        edgepaths
            .style("stroke", function(o) { 
                var path = '#edgepath' + o.source.name + '-' + o.target.name;
                var bool = ($(path).css('stroke') === "rgb(229, 75, 75)");
                if (o.source.index == d.index || o.target.index == d.index) {
                    if (bool) {
                        return highlight_color_selected;
                    }
                    return highlight_color;
                } else if (bool) {
                    return "#E54B4B";
                } else if ('sids' in o) {
                    return in_sub_color;
                }
                return not_in_sub_color;
            })
            .style("stroke-width", function(o) { return o.source.index == d.index || o.target.index == d.index ? "3px" : "1.5px"; });
    }

    function dragging(d) {
        focus_node = d;
        set_focus(d);
        if (highlight_node === null) {
            highlight(d);
        }
    }

    function set_focus(d) {   
        nodes.style("opacity", function(o) { return isConnected(d, o) ? 1 : highlight_trans; });
        nodelabels.style("opacity", function(o) { return isConnected(d, o) ? 1 : highlight_trans; });
        edgepaths.style("opacity", function(o) { return o.source.index == d.index || o.target.index == d.index ? 1 : highlight_trans; });     
    }

    function nohighlight() {
        highlight_node = null;
        if (focus_node === null) {

            node_legend_rect.attr("height", nodeLegendHeight);
            node_legend.select("text")
                .text("Hover over a node.");

            nodes
                .style("fill", function(d,i){
                    if ('sids' in d) {
                        var sids = d.sids.split(",")
                        if (sids.length > 1) {
                            return '#000000';
                        }
                        var id = parseInt(sids[0].charAt(0)) - 1;
                        return gradual_colors[id]; 
                    } 
                    return not_in_sub_color;
                })
                .style("stroke", function(o) {
                    var n = '#node' + o.name;
                    if (($(n).css('stroke') === "rgb(229, 75, 75)") || ($(n).css('stroke') === "rgb(114, 37, 37)")) {
                        return "#E54B4B";
                    } else if ('sids' in o) {
                        return in_sub_color;
                    }
                    return not_in_sub_color;
                })
                .style("stroke-width", "1.5px");
            nodelabels
                .style("font-weight", "normal");
            edgepaths
                .style("stroke", function(o) {
                    var path = '#edgepath' + o.source.name + '-' + o.target.name;
                    if (($(path).css('stroke') === "rgb(229, 75, 75)") || ($(path).css('stroke') === "rgb(114, 37, 37)")) {
                        return "#E54B4B";
                    } else if ('sids' in o) {
                        return in_sub_color;
                    }
                    return not_in_sub_color;
                })
                .style("stroke-width", "1.5px");
        }
    }

    function zoomed() {
        container.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
    }

    function dragstarted(d) {
        d3.event.sourceEvent.stopPropagation();
        d3.select(this).classed("dragging", true);
        force.start();
    }

    function dragged(d) {
        d3.select(this).attr("cx", d.x = d3.event.x).attr("cy", d.y = d3.event.y);
    }

    function dragended(d) {
        d3.select(this).classed("dragging", false);
    }

    force.on("tick", function(){
        nodes.attr({"cx": function(d){ return d.x },
                    "cy": function(d){ return d.y }
        });
        nodelabels.attr({"x": function(d) { return d.x },
                         "dx": 10,
                         "y": function(d) { return d.y },
                         "dy": ".60em"
        });
        edgepaths.attr('d', function(d) { 
            var path = 'M ' + d.source.x + ' ' + d.source.y + ' L ' + d.target.x + ' ' + d.target.y;
            return path;
        });       
    });

    container.selectAll("circle")
        .on("click", function(node) {
            if ('sids' in node) {
                $('#btn-subgraph-exploration').prop("disabled", false);
                nodes.style("stroke", function(other) {
                    var sids = node.sids.split(",");
                    if ('sids' in other) {
                        var other_sids = other.sids.split(",");
                        var retained = other_sids.filter(function(sid) { return sids.includes(sid) });
                        return (retained.length > 0) ? "#E54B4B" : in_sub_color;    
                    }
                    return in_sub_color;
                });
                edgepaths.style("stroke", function(edge) {
                    var sids = node.sids.split(",");
                    if ('sids' in edge) {
                        var edge_sids = edge.sids.split(",");
                        var retained = edge_sids.filter(function(sid) { return sids.includes(sid) });
                        return (retained.length > 0) ? "#E54B4B" : in_sub_color;    
                    }
                    return not_in_sub_color;
                });
            }
        });

    return true;
}

function generate_chart(data) {
    var margin = {top: 10, right: 10, bottom: 10, left: 10},
        width = $("#results-card").parent().width() - margin.left - margin.right,
        height = $("#results-card").parent().width() - margin.top - margin.bottom;

    var focus_node = null, highlight_node = null;
    var highlight_color = "#45474d";
    var highlight_color_selected = "#722525";
    var highlight_trans = 0.1;
    var linkDistance = 100;
    var gradual_colors = ["#2b2640", "#564d80", "#777099", "#9994b2", "#bbb7cc", "#dddbe5"];

    var force = d3.layout.force()
        .nodes(data.nodes)
        .links(data.edges)
        .size([width, height])
        .linkDistance([linkDistance])
        .charge([-300])
        .linkStrength(0.1)
        .theta(0.1)
        .gravity(0.08)
        .start();

    var zoom = d3.behavior.zoom()
        .on("zoom", zoomed);

    var drag = d3.behavior.drag()
        .origin(function(d) { return d; })
        .on("dragstart", dragstarted)
        .on("drag", dragged)
        .on("dragend", dragended);

    var graph = d3.select("#results-card")
        .append("svg")
        .attr({"width":width,
               "height":height,
               "style":"background-color:#E8EEF2"})
        .append("g")
        .call(zoom);

    var rect = graph.append("rect")
            .attr("width", width)
            .attr("height", height)
            .style("fill", "none")
            .style("pointer-events", "all");

    var container = graph.append("g");

    var edgepaths = container
        .append("g")
        .selectAll(".edgepath")
        .data(data.edges)
        .enter()
        .append("path")
        .attr({"d": function(d) { return 'M ' + d.source.x + ' ' + d.source.y + ' L ' + d.target.x + ' ' + d.target.y },
               "id": function(d,i) { return 'edgepath' + d.source.name + "-" + d.target.name },
               "class": "edgepath"});

    var nodes = container
        .append("g")
        .selectAll("circle")
        .data(data.nodes)
        .enter()
        .append("circle")
        .attr("r", 8)
        .attr("id", function(d) { return 'node' + d.name })
        .style("fill", function(d,i){
            var sids = d.sids.split(",")
            if (sids.length > 1) {
                return "#FFFFFF";
            }
            var id = parseInt(sids[0].charAt(0)) - 1;
            return gradual_colors[id];}
        )
        .call(drag);

    nodes.on("dblclick.zoom", function(d) { 
        var dcx = (window.innerWidth / 2 - d.x * zoom.scale());
        var dcy = (window.innerHeight / 2 - d.y * zoom.scale());
        zoom.translate([dcx,dcy]);
        g.attr("transform", "translate("+ dcx + "," + dcy  + ")scale(" + zoom.scale() + ")");
    });

    var nodelabels = container
        .selectAll(".nodelabel") 
        .data(data.nodes)
        .enter()
        .append("text")
        .attr({"x": function(d){ return d.x },
              "dx": 10,
              "y": function(d){ return d.y },
              "dy": ".60em",
              "class": "nodelabel"
        })
        .text(function(d){ return d.name });

    var legendHeight = data.densities.length * 18 + 10;
    var legendWidth = 80;
    var nodeLegendWidth = 140;
    var nodeLegendHeight = 40;

    var legend = graph
        .append("g")
        .attr("transform", "translate (" + (width - legendWidth - 10) + "," + (height - legendHeight - 24) + ")")
        .attr("class", "legend");
    
    legend.selectAll("text")
        .data(['Density'])
        .enter()
        .append("text")
        .attr("class", "legend-title")
        .attr("x", 18)
        .attr("y", 8)
        .text(function(d) { return d });

    var lb = legend.append("rect")
        .attr("transform", "translate (0,14)")
        .attr("class", "legend-box")
        .attr("width", legendWidth)
        .attr("height", legendHeight)
        .attr('rx', 5)
        .attr('ry', 5);

    var li = legend.append("g")
        .attr("transform", "translate (8,5)")
        .attr("class", "legend-items");

    li.selectAll("rect")
        .data(data.densities)
        .enter()
        .append("rect")
        .attr("y", function(d, i) { return legendHeight - 10 - (i * 18) })
        .attr("width", 10)
        .attr("height", 10)
        .style("fill", function(d) { return gradual_colors[parseInt(d.id) - 1] });

    li.selectAll("text")
        .data(data.densities)
        .enter()
        .append("text")
        .attr("x", 15)
        .attr("y", function(d, i) { return legendHeight - 10 - (i * 18) + 9 })
        .text(function(d) {
                if (d.id == "6") {
                    return "<= " + d.value; 
                } 
                return d.value;
            })
        .style('stroke', 'black');

    var node_legend = graph
        .append("g")
        .attr("class", "mouse-over-effects")
        .attr("class", "legend")
        .attr("transform", "translate (" + (width - nodeLegendWidth - 10) + "," + (10) + ")");

    var node_legend_rect = node_legend.append("rect")
        .attr("width", nodeLegendWidth)
        .attr("height", nodeLegendHeight)
        .attr("rx", 5)
        .attr("ry", 5);

    node_legend.append("text")
        .attr("x", 15)
        .attr("y", 15)
        .attr("stroke", "black")
        .text("Hover over a node.");

    var linkedByIndex = {};
    data.edges.forEach(function(d) {
        linkedByIndex[d.source.index + "," + d.target.index] = 1;
    });

    function isConnected(a, b) {
        return linkedByIndex[a.index + "," + b.index] || linkedByIndex[b.index + "," + a.index] || a.index == b.index;
    }

    nodes
        .on("mouseover", function(d){ highlight(d); })
        .on("mousedown", function(d) { dragging(d); })
        .on("mouseout", function(d){ nohighlight(); });

    d3.select(window).on("mouseup", function() {
        if (focus_node !== null) {
            focus_node = null;
            nodes.style("opacity", 1);
            nodelabels.style("opacity", 1);
            edgepaths.style("opacity", 1);
        }
    
        if (highlight_node === null) {
            nohighlight();
        }
    });

    function highlight(d) {
        if (focus_node !== null) {
            d = focus_node;
        }
        highlight_node = d;

        let sids = d.sids.split(",");
        let start = 15;
        if (sids.length > 2) {
            node_legend_rect.attr("height", nodeLegendHeight + 16 * (sids.length - 1));   
        }
        var current_text = node_legend.select("text")
            .text("")
            .append("tspan")
            .attr("x", 15)
            .attr("y", start)
            .text("Node ID : " + d.name)
            .append("tspan")
            .attr("x", 15)
            .attr("y", start += 16)
            .text("Appears in : " + (parseInt(sids[0].substring(1)) + 1));

        for (var i = 1; i < sids.length; i++) {
            if (sids[i] !== sids[0]) {
                current_text.append("tspan")
                    .attr("x", 86)
                    .attr("y", start += 16)
                    .text(parseInt(sids[i].substring(1)) + 1);
            }
        }

        nodes
            .style("stroke", function(o) { 
                var n = '#node' + o.name;
                var bool = ($(n).css('stroke') === "rgb(229, 75, 75)");
                if (isConnected(d, o)) {
                    if (bool) {
                        return highlight_color_selected;
                    }
                    return highlight_color;
                } else if (bool) {
                    return "#E54B4B";
                }
                return "#AEB4A9";
            })
            .style("stroke-width", function(o) { return isConnected(d, o) ? "3px" : "1.5px"; });
        nodelabels
            .style("font-weight", function(o) { return isConnected(d, o) ? "bold" : "normal";});
        edgepaths
            .style("stroke", function(o) { 
                var path = '#edgepath' + o.source.name + '-' + o.target.name;
                var bool = ($(path).css('stroke') === "rgb(229, 75, 75)");
                if (o.source.index == d.index || o.target.index == d.index) {
                    if (bool) {
                        return highlight_color_selected;
                    }
                    return highlight_color;
                } else if (bool) {
                        return "#E54B4B";
                }
                return "#AEB4A9";
            })
            .style("stroke-width", function(o) { return o.source.index == d.index || o.target.index == d.index ? "3px" : "1.5px"; });
    }

    function dragging(d) {
        focus_node = d;
        set_focus(d);
        if (highlight_node === null) {
            highlight(d);
        }
    }

    function set_focus(d) {   
        nodes.style("opacity", function(o) { return isConnected(d, o) ? 1 : highlight_trans; });
        nodelabels.style("opacity", function(o) { return isConnected(d, o) ? 1 : highlight_trans; });
        edgepaths.style("opacity", function(o) { return o.source.index == d.index || o.target.index == d.index ? 1 : highlight_trans; });     
    }

    function nohighlight() {
        highlight_node = null;
        if (focus_node === null) {

            node_legend_rect.attr("height", nodeLegendHeight);
            node_legend.select("text")
                .text("Hover over a node.");

            nodes
                .style("fill", function(d,i){
                    var sids = d.sids.split(",")
                    if (sids.length > 1) {
                        return "#FFFFFF";
                    }
                    var id = parseInt(sids[0].charAt(0)) - 1;
                    return gradual_colors[id]; })
                .style("stroke", function(o) {
                    var n = '#node' + o.name;
                    if (($(n).css('stroke') === "rgb(229, 75, 75)") || ($(n).css('stroke') === "rgb(114, 37, 37)")) {
                        return "#E54B4B";
                    } else {
                        return "#AEB4A9";
                    }
                })
                .style("stroke-width", "1.5px");
            nodelabels
                .style("font-weight", "normal");
            edgepaths
                .style("stroke", function(o) {
                    var path = '#edgepath' + o.source.name + '-' + o.target.name;
                    if (($(path).css('stroke') === "rgb(229, 75, 75)") || ($(path).css('stroke') === "rgb(114, 37, 37)")) {
                        return "#E54B4B";
                    } else {
                        return "#AEB4A9";    
                    }
                })
                .style("stroke-width", "1.5px");
        }
    }

    function zoomed() {
        container.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
    }

    function dragstarted(d) {
        d3.event.sourceEvent.stopPropagation();
        d3.select(this).classed("dragging", true);
        force.start();
    }

    function dragged(d) {
        d3.select(this).attr("cx", d.x = d3.event.x).attr("cy", d.y = d3.event.y);
    }

    function dragended(d) {
        d3.select(this).classed("dragging", false);
    }

    force.on("tick", function(){
        nodes.attr({"cx": function(d){ return d.x },
                    "cy": function(d){ return d.y }
        });
        nodelabels.attr({"x": function(d) { return d.x },
                         "dx": 10,
                         "y": function(d) { return d.y },
                         "dy": ".60em"
        });
        edgepaths.attr('d', function(d) { 
            var path = 'M ' + d.source.x + ' ' + d.source.y + ' L ' + d.target.x + ' ' + d.target.y;
            return path;
        });       
    });

    container.selectAll("circle")
        .on("click", function(node) {
            $('#btn-subgraph-exploration').prop("disabled", false);
            nodes.style("stroke", function(other) {
                var sids = node.sids.split(",");
                var other_sids = other.sids.split(",");
                var retained = other_sids.filter(function(sid) { return sids.includes(sid) });
                return (retained.length > 0) ? "#E54B4B" : "#AEB4A9";
            });
            edgepaths.style("stroke", function(edge) {
                var sids = node.sids.split(",");
                var edge_sids = edge.sids.split(",");
                var retained = edge_sids.filter(function(sid) { return sids.includes(sid) });
                return (retained.length > 0) ? "#E54B4B" : "#AEB4A9";
            });
        });

    return true;
}

function generateSnapGraph(data, id, name) {
    var margin = {top: 10, right: 10, bottom: 10, left: 10},
        width = $("#results-card").parent().width() - margin.left - margin.right,
        height = $("#results-card").parent().width() * 0.6 - margin.top - margin.bottom;

    var focus_node = null, highlight_node = null;
    var highlight_color = "#45474d";
    var highlight_trans = 0.1;
    var linkDistance = 90;
    var colors = d3.scale.category10();
    var nodeLegendWidth = 200;
    var nodeLegendHeight = 60;

    var nodeColors = new Set();
    data.nodes.forEach(function(n) {
        if (n.label) {
            nodeColors.add(n.label.split("+")[0]);
        }
    });
    var nodeColorArray = Array.from(nodeColors);

    var force = d3.layout.force()
        .nodes(data.nodes)
        .links(data.edges)
        .size([width, height])
        .linkDistance([linkDistance])
        .charge([-300])
        .theta(0.1)
        .gravity(0.08)
        .start();

    var zoom = d3.behavior.zoom()
        .on("zoom", zoomed);

    var drag = d3.behavior.drag()
        .origin(function(d) { return d; })
        .on("dragstart", dragstarted)
        .on("drag", dragged)
        .on("dragend", dragended);

    var graph = d3.select("#snap_" + id)
        .append("svg")
        .attr({"width":width,
               "height":height,
               "style":"background-color:#E8EEF2"})
        .append("g")
        .call(zoom);

    var rect = graph.append("rect")
            .attr("width", width)
            .attr("height", height)
            .style("fill", "none")
            .style("pointer-events", "all");

    var container = graph.append("g");

    var edgepaths = container
        .append("g")
        .selectAll(".edgepath")
        .data(data.edges)
        .enter()
        .append("path")
        .attr({"d": function(d) { return 'M ' + d.source.x + ' ' + d.source.y + ' L ' + d.target.x + ' ' + d.target.y },
               "id": function(d,i) { return 'edgepath' + d.source.name + "-" + d.target.name },
               "class": "edgepath"});

    var nodes = container
        .append("g")
        .selectAll("circle")
        .data(data.nodes)
        .enter()
        .append("circle")
        .attr("r", 8)
        .attr("id", function(d) { return 'node' + d.name })
        .style("fill", function(d,i){ 
            if (d.label) {
                return colors(nodeColorArray.indexOf(d.label.split("+")[0]))
            }
            return colors(0)
        })
        .call(drag);

    nodes.on("dblclick.zoom", function(d) { 
        var dcx = (window.innerWidth / 2 - d.x * zoom.scale());
        var dcy = (window.innerHeight / 2 - d.y * zoom.scale());
        zoom.translate([dcx,dcy]);
        g.attr("transform", "translate("+ dcx + "," + dcy  + ")scale(" + zoom.scale() + ")");
    });

    var nodelabels = container
        .selectAll(".nodelabel") 
        .data(data.nodes)
        .enter()
        .append("text")
        .attr({"x": function(d){ return d.x },
              "dx": 10,
              "y": function(d){ return d.y },
              "dy": ".60em",
              "class": "nodelabel"
        })
        .text(function(d){ 
            if (d.label && d.label != "") {
                var l = d.label.split("+")[1];
                if (l != "no name") {
                    return l
                }
            }
            return d.name
        });

    if (nodeColorArray.length > 0) {
        var legendHeight = nodeColorArray.length * 18 + 10;
        var legendWidth = 80;
        var label_legend = graph
            .append("g")
            .attr("transform", "translate (" + (width - legendWidth - 10) + "," + (height - legendHeight - 24) + ")")
            .attr("class", "legend");
        label_legend.selectAll("text")
            .data(['Labels'])
            .enter()
            .append("text")
            .attr("class", "legend-title")
            .attr("x", 18)
            .attr("y", 8)
            .text(function(d) { return d });

        var lb = label_legend.append("rect")
            .attr("transform", "translate (0,14)")
            .attr("class", "legend-box")
            .attr("width", legendWidth)
            .attr("height", legendHeight)
            .attr('rx', 5)
            .attr('ry', 5);

        var li = label_legend.append("g")
            .attr("transform", "translate (8,5)")
            .attr("class", "legend-items");

    li.selectAll("rect")
        .data(nodeColorArray)
        .enter()
        .append("rect")
        .attr("y", function(d, i) { return legendHeight - 10 - (i * 18);})
        .attr("width", 10)
        .attr("height", 10)
        .style("fill", function(d) { return colors(nodeColorArray.indexOf(d))});

        li.selectAll("text")
            .data(nodeColorArray)
            .enter()
            .append("text")
            .attr("x", 15)
            .attr("y", function(d, i) { return legendHeight - 10 - (i * 18) + 9; })
            .text(function(d) { return d })
            .style('stroke', 'black');
    }
    
    var node_legend = graph
        .append("g")
        .attr("class", "mouse-over-effects")
        .attr("class", "legend")
        .attr("transform", "translate (" + (width - nodeLegendWidth - 10) + "," + (10) + ")");

    var node_legend_rect = node_legend.append("rect")
        .attr("width", nodeLegendWidth)
        .attr("height", nodeLegendHeight)
        .attr("rx", 5)
        .attr("ry", 5);

    node_legend.append("text")
        .attr("stroke", "black")
        .append("tspan")
        .attr("x", 15)
        .attr("y", 15)
        .text("Snapshot : " + name)
        .append("tspan")
        .attr("x", 15)
        .attr("y", 31)
        .text("Average Degree : " + data.density)
        .append("tspan")
        .attr("x", 15)
        .attr("y", 47)
        .text("Num Edges : " + data.edges.length);

    var linkedByIndex = {};
    data.edges.forEach(function(d) {
        linkedByIndex[d.source.index + "," + d.target.index] = 1;
    });

    function isConnected(a, b) {
        return linkedByIndex[a.index + "," + b.index] || linkedByIndex[b.index + "," + a.index] || a.index == b.index;
    }

    nodes
        .on("mouseover", function(d){ highlight(d); })
        .on("mousedown", function(d) { dragging(d); })
        .on("mouseout", function(d){ nohighlight(); });

    $('svg').on("mouseup", function() {
        if (focus_node !== null) {
            focus_node = null;
            nodes.style("opacity", 1);
            nodelabels.style("opacity", 1);
            edgepaths.style("opacity", 1);
        }
    
        if (highlight_node === null) {
            nohighlight();
        }
    });

    function highlight(d) {
        if (focus_node !== null) {
            d = focus_node;
        }
        highlight_node = d;

        nodes
            .style("stroke", function(o) { return isConnected(d, o) ? highlight_color : "#AEB4A9"; })
            .style("stroke-width", function(o) { return isConnected(d, o) ? "3px" : "1.5px"; });
        nodelabels
            .style("font-weight", function(o) { return isConnected(d, o) ? "bold" : "normal";});
        edgepaths
            .style("stroke", function(o) { 
                return (o.source.index == d.index || o.target.index == d.index) ? highlight_color : "#AEB4A9";
            })
            .style("stroke-width", function(o) { return o.source.index == d.index || o.target.index == d.index ? "3px" : "1.5px"; });
    }

    function dragging(d) {
        focus_node = d;
        set_focus(d);
        if (highlight_node === null) {
            highlight(d);
        }
    }

    function set_focus(d) {   
        nodes.style("opacity", function(o) { return isConnected(d, o) ? 1 : highlight_trans; });
        nodelabels.style("opacity", function(o) { return isConnected(d, o) ? 1 : highlight_trans; });
        edgepaths.style("opacity", function(o) { return o.source.index == d.index || o.target.index == d.index ? 1 : highlight_trans; });     
    }

    function nohighlight() {
        highlight_node = null;
        if (focus_node === null) {
            nodes
                .style("fill", function(n) {
                    if (n.label) {
                        return colors(nodeColorArray.indexOf(n.label.split("+")[0]))
                    }
                    return colors(0)
                })
                .style("stroke", "#AEB4A9")
                .style("stroke-width", "1.5px");
            nodelabels
                .style("font-weight", "normal");
            edgepaths
                .style("stroke", "#AEB4A9")
                .style("stroke-width", "1.5px");
        }
    }

    function zoomed() {
        container.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
    }

    function dragstarted(d) {
        d3.event.sourceEvent.stopPropagation();
        d3.select(this).classed("dragging", true);
        force.start();
    }

    function dragged(d) {
        d3.select(this).attr("cx", d.x = d3.event.x).attr("cy", d.y = d3.event.y);
    }

    function dragended(d) {
        d3.select(this).classed("dragging", false);
    }

    force.on("tick", function(){
        nodes.attr({"cx": function(d){ return d.x },
                    "cy": function(d){ return d.y }
        });
        nodelabels.attr({"x": function(d) { return d.x },
                         "dx": 10,
                         "y": function(d) { return d.y },
                         "dy": ".60em"
        });
        edgepaths.attr('d', function(d) { 
            var path = 'M ' + d.source.x + ' ' + d.source.y + ' L ' + d.target.x + ' ' + d.target.y;
            return path;
        });       
    });

    return true;
}

function generateExplodedView() {
    let width = 0;
    $.ajax({
        type: "GET",
        url: '/getExplodedData',
        processData: false,
        contentType: false,
        cache: false,
        timeout: 600000,
        success: function (snapshots) {
            if(snapshots == "") {
                $('#loading-results').hide();
                $('#no-explode-result').show();
            } else {
                var data = JSON.parse(snapshots);
                data.forEach(function(entry) {
                    $('#snaps_tabs').append(
                        '<li class="tab"><a id="tab_' + entry.number + '" href="#snap_' + entry.number + '">Snap ' + entry.number + '</a></li>'
                    );
                    $('#snaps_graphs').append(
                        '<div style="display: none;" id="snap_' + entry.number + '">' +
                        '</div>'
                    );  
                    if (entry.label) {
                        width = generateSnapGraph(entry.subgraph, entry.number, entry.label);    
                    } else {
                        width = generateSnapGraph(entry.subgraph, entry.number, entry.number); 
                    }
                });
                $('#loading-results').hide();
                $('#snaps_tabs').attr({"style": "width: " + width + "px"});
                $('#explore_subgraph').show();
                try {
                    $('.tabs').tabs();    
                } catch (e) {}
                $('#snap_' + data[0].number).attr({"class": "active", "style": "display: block"});
                $('#tab_' + data[0].number).attr({"class": "active"});
                $("html, body").animate({ scrollTop: $(document).height()}, 1000);
            }
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to get the subgraph data'});
            return false;
        }
    });
    return true;
}