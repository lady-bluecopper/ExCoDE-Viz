function createJSON(data, name) {
    let config = {};
    config.Name = name;
    let subgraph = {};
    let jsonArray = [];
    data.each(function(id) {
        var ids = data[id].id.substring(8).split("-");
        item = {};
        item["source"] = ids[0];
        item["target"] = ids[1];
        jsonArray.push(item);
    });
    subgraph.edges = jsonArray;
    config.Subgraph = subgraph;
    return config;
}

function generate_subgraph_card(string) {
    var data = JSON.parse(string);
    var htmlStringNodes = {};
    var htmlStringEdges = {};
    var densities = {};
    var subgraphDensity = {};
    var nodes = '<h6 left-align><i>Nodes</i> : ';
    var edges = '<h6 left-align><i>Edges</i> : ';
    var density = '<h6 left-align><i>Density</i> : ';
    var subgraph = '<div class="row"><p style="font-weight: bold;">Subgraph ';
    
    data.nodes.forEach(function(node) {
        if ('sids' in node) {
            var sids = node.sids.split(',');
            sids.forEach(function(s) {
                var sid = s.substring(1);
                if (!(sid in htmlStringNodes)) {
                    htmlStringNodes[sid] = nodes;
                    subgraphDensity[sid] = s.charAt(0);
                }
                htmlStringNodes[sid] += (node.name + ',');
            });
        }
    });

    data.edges.forEach(function(edge) {
        if ('sids' in edge) {
            var sids = edge.sids.split(',');
            sids.forEach(function(s) {
                var sid = s.substring(1);
                if (!(sid in htmlStringEdges)) {
                    htmlStringEdges[sid] = edges;
                }
                htmlStringEdges[sid] += ('(' + edge.source + "-" + edge.target + "),");
            });
        }
    });

    data.densities.forEach(function(pair) {
        densities[pair.id] = pair.value;
    });

    for (var key in htmlStringNodes) {
        var toAppend = '';
        toAppend += (subgraph + (parseInt(key) + 1) + '</p>');
        toAppend += (htmlStringNodes[key].slice(0, -1) + '</h6>');
        toAppend += (htmlStringEdges[key].slice(0, -1) + '</h6>');
        toAppend += (density + densities[subgraphDensity[key]] + '</h6>');
        toAppend += '</div>';
        $('#subgraph-summary').append(toAppend);
    }
    return true;
}

$(document).ready(function () {
    $.ajaxSetup({ cache: false });

    let name = $('#conf_name').text().trim().toLowerCase();
    let dataset = $('#dataset_path_name').text().trim();

    loadConfigurations();
    $('#results-card').hide();
    $('#btn-subgraph-exploration').hide();
    $('#more-subgraphs').hide();

    createChart1(dataset);

    $('#loading-results').show();

    $.ajax({
        method: 'GET',
        headers: headers,
        url: '/getResults?name=' + name,
        success: function (results) {
            if(results == "") {
                $('#loading-results').hide();
                $('#no-result').show();
            }
            else {
                var data = JSON.parse(results);
                var completed;
                if ('size' in data) {
                    completed = generate_chart_in_graph(data);
                } else {
                    completed = generate_chart(data);    
                }
                if (completed) {
                    $('#loading-results').hide();
                    $('#results-card').show();
                    $('#btn-subgraph-exploration').show();
                } 
                if (generate_subgraph_card(results)) {
                    $('#more-subgraphs').show();    
                }
                $("html, body").animate({ scrollTop: $(document).height()}, 1000);
            }
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to load the results of ' + name});
            $('#loading-results').hide();
            return;
        }
    });

    $('#btn-subgraph-exploration').click(function (event) {
        $('#loading-results').show();
        $('#explore_subgraph').hide();
        $('#no-explode-result').hide();
        $('#snaps_tabs').empty();
        $('#snaps_graphs').empty();

        var data = $('.edgepath').filter(function() {
            return $(this).css('stroke') === "rgb(229, 75, 75)";
        });
        var subgraph = createJSON(data, name);
        console.log(JSON.stringify(subgraph));

        $.ajax({
            method: 'POST',
            headers: headers,
            url: '/exploreSubgraph?name=' + name,
            data: JSON.stringify(subgraph),
            timeout: 600000,
            success: function (result) {
                generateExplodedView();
                $("html, body").animate({ scrollTop: $(document).height()}, 1000);
            },
            error: function (XMLHttpRequest, textStatus, errorThrown) {
                M.toast({html: 'Unable to load the exploded view'});
                $('#loading-results').hide();
            }
        });
    });

    $('.card-title').click(() => {
        $('.subgraph-card').css('width', ($("#results-card").parent().width() - 20))
        $('.subgraph-card').css('height', ($("#results-card").parent().width() / 2 - 20));
        $('.subgraph-card').css('background-color', 'white');
        $('#btn-subgraph-exploration').hide();
    });

    $('.shrink').click(() => {
        $('.subgraph-card').css('width', 'auto');
        $('.subgraph-card').css('height', 'auto');
        $('.subgraph-card').css('background-color', '#874000');
        $('#btn-subgraph-exploration').show();
    });
});    