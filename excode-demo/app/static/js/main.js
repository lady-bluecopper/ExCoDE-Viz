function createDataDescriptor(data) {
    let dataset = {};
    dataset.Name = $("#dataset-dd").val();
    if($('#local').prop('checked') === true) {
        dataset.Local = "true";
        dataset.Path = $('input[type=file]')[0].files[0].name;
    } else {
        dataset.Local = "false";
        dataset.Path = $('#dataset-list').find(":selected").text();
    }
    dataset.Properties = {};
    dataset.Properties.Weighted = "false";
    dataset.Properties.Labeled = "false";
    return dataset;
}

function createConfiguration() {
    let data = {};
    data.Name = $('#config-dd').val();
    data.Dataset = createDataDescriptor();
    data.Task = {};
    data.Task.Correlation = $('#correlationSlider').val();
    data.Task.Density = $('#densitySlider').val();
    if($('#minimum').prop('checked') === false) {
        data.Task.DensityFunction = "Average";
    } else {
        data.Task.DensityFunction = "Minimum";
    }
    data.Task.EdgesPerSnapshot = $('#snapSlider').val();
    if ($('#sizeButton').prop('checked') === false) {
        data.Task.MaxSize = $('#sizeSlider').val();    
    }
    else {
        data.Task.MaxSize = $('#sizeButton').val();   
    }
    data.Task.Epsilon = $('#simSlider').val();
    return data;
}

function saveConfiguration() {
    let data = createConfiguration();
    console.log(data);
    $.ajax({
        method: 'POST',
        headers: headers,
        url: '/saveConfiguration',
        data: JSON.stringify(data),
        success: function (bool) {
            console.log('Data: ' + data);
            if (bool == "true") {
                let config = $('#config-dd').val().trim();
                let html =
                    '<li class="conf-item" id="conf-element">' +
                        '<button type="button" id="delete_' + config + '" class="btn-floating">' +
                            '<i class="material-icons">delete_outline</i>' +
                        '</button>' +
                        '<a class="waves-effect waves-light side-conf" href="/getConfiguration?name=' + config + '">' + config + '</a>' +
                    '</li>';
                $('.sidenav li:eq(' + ($('.sidenav ul li').length - 2) + ')').after(html);
                $('#delete_' + config).click(function() {
                    deleteConfiguration(config);
                });
            }
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to save the configuration ' + data.Name});
        }
    });
}

function saveDatasetName(name) {
    $.ajax({
        method: 'POST',
        headers: headers,
        url: '/saveDatasetName?name=' + name,

        success: function (response) {
            console.log(response);
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to save dataset name ' + data.Name});
        }
    });
}

function createCharts(dbFileName) {
    return (createChart1(dbFileName) && createChart2(dbFileName) && createChart3(dbFileName) && createChart4(dbFileName) && createChart5(dbFileName) && createChart6(dbFileName));
}

function catchFormEvents() {
    // ----
    $("#config-dd").on('input', function() {
        $("#config-error").hide();
    });
    $("#dataset-dd").on('input', function() {
        $("#dataset-error").hide();
    });
    $("#dataset-list").on('change', function() {
        $("#path-error").hide();
    });
    $("#dataset-button").on('click', function() {
        $("#path-error").hide();
    });
    // ----
    $('#cor').text($('#correlationSlider').val());
    $('#den').text($('#densitySlider').val());
    $('#snap').text($('#snapSlider').val());
    if ($('#sizeButton').prop('checked')) {
        $('#size').text('10000');
    } else {
        $('#size').text($('#sizeSlider').val());
    }
    $('#sim').text($('#simSlider').val());
    // ----
    $('input[type=radio][name=fileGroup]').on('change', function() {
        if ($(this).val() == "local") {
            $('#local-field').show();
            $('#remote-field').hide();
        } else {
            $('#local-field').hide();
            $('#remote-field').show();
        }
    });
    $('#correlationSlider').on('input', function() {
        $('#cor').text($(this).val());
    });

    $('#sizeButton').on('change', function() {
        $('#size').text($(this).val());
    });

    $('#densitySlider').on('input', function() {
        $('#den').text($(this).val());
    });

    $('#snapSlider').on('input', function() {
        $('#snap').text($(this).val());
    });

    $('#sizeSlider').on('input', function() {
        $('#size').text($(this).val());
        $('#sizeButton').prop('checked', false);
    });

    $('#sizeButton').on('change', function() {
        $('#size').text($(this).val());
    });

    $('#simSlider').on('input', function() {
        $('#sim').text($(this).val());
    });
}

$(document).ready(function () {
    $.ajaxSetup({ cache: false });
    $('select').formSelect();
    $('.tooltipped').tooltip();
    $('.materialboxed').materialbox();
    $('#loading-dataset').hide();
    $('#dataset-load').hide();
    $('#btn-save-configuration').prop("disabled", true);
    $('#btn-execute-configuration').prop("disabled", true);
    $('#loadFirst').show();
    catchFormEvents();
    // -----
    $('#btn-dataset-select').click(function (event) {
        $('#dataset-load').hide();
        $('#general_stats').empty();
        $('#snap_stats').empty();
        $('#degrees-multi').empty();
        $('#num_edges').empty();
        $('#degrees').empty();
        $('#num_snap').empty();

        var error = false;
        if ($("#config-dd").val() == "") {
            $("#config-error").show();
            error = true;
        }
        if ($("#dataset-dd").val() == "") {
            $("#dataset-error").show();
            error = true;
        }
        if ($("#path-dd").val() == "") {
            $("#path-error").show();
            error = true;
        }
        if (error) {
            return false;
        }
        event.preventDefault();
        $('#loading-dataset').show();
        var dbFileName = ""
        if ($("#local").prop("checked") === true) {
            dbFileName = $('input[type=file]')[0].files[0].name;
            if ($("#" + dbFileName).length == 0 && dbFileName.length > 0) {
                $("#dataset-list").append($("<option></option>").attr("value", dbFileName).attr("id", "dataset-" + dbFileName).text(dbFileName))
                saveDatasetName(dbFileName);
            }
            $('select').formSelect();
        } else {
            dbFileName = $('#dataset-list').find(":selected").text();
        }

        let form = $('#dataset-select')[0];
        let data = new FormData(form);

        $.ajax({
            type: "POST",
            enctype: 'multipart/form-data',
            url: '/statistics',
            data: data,
            processData: false,
            contentType: false,
            cache: false,
            timeout: 600000,
            success: function (data) {
                console.log("Statistics: ", data);
                if (data == "Done") {
                    if (createCharts(dbFileName)) {
                        $('#loading-dataset').hide();
                        $('#dataset-load').show();
                        $('#btn-save-configuration').prop("disabled", false);
                        $('#btn-execute-configuration').prop("disabled", false);
                        $('#loadFirst').hide();    
                    }    
                } else {
                    M.toast({html: data});
                    $('#loading-dataset').hide();
                    return false;
                }
                
            },
            error: function (XMLHttpRequest, textStatus, errorThrown) {
                M.toast({html: 'Unable to load dataset ' + $('#dataset-dd').val()});
                $('#loading-dataset').hide();
                return false;
            }
        });

        return true;
    });

    $('#btn-save-configuration').click(function (event) {
        event.preventDefault();
        saveConfiguration();
    });

    loadConfigurations();
});