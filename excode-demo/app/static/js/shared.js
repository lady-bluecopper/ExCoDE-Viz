let headers = {
    'Accept': 'application/json',
    'Content-Type': 'text/plain',
};

function loadConfigurations() {
    $.ajax({
        method: 'GET',
        headers: headers,
        url: '/loadConfigurations',
        success: function (results) {
            let parsedData = JSON.parse(results);
            Object.keys(parsedData).forEach(function (key) {
                let html =
                    '<li class="conf-item" id="conf-element">' +
                    '<button type="button" id="delete_' + key + '" class="btn-floating"><i class="material-icons">delete_outline</i></button>' +
                    '<a class="waves-effect waves-light side-conf" href="/getConfiguration?name=' + key + '">' +
                    key +
                    '</a>' +
                    '</li>';
                $('.sidenav li:eq(' + ($('.sidenav ul li').length - 2) + ')').after(html);
                $('#delete_' + key).click(function() {
                    deleteConfiguration(key);
                });
            });
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to load the configurations!'});
        }
    });
}

function deleteConfiguration(name) {
    $.ajax({
        method: 'POST',
        headers: headers,
        url: '/deleteConfiguration?name=' + name,
        success: function (bool) {
            if (bool) {
                $('.conf-item').remove();
                loadConfigurations(); 
            }
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            M.toast({html: 'Unable to delete configuration ' + name + '!'});
        }
    });   
}