/* The content of this file is in the public domain. */

if (!Array.indexOf) {
    Array.prototype.indexOf = function (obj, start) {
        if (start === undefined) {
            start = 0;
        }
        for (var i = start; i < this.length; i++) {
            if (this[i] === obj) {
                return i;
            }
        }
        return -1;
    };
}

var onEdit;
var onReady;

function acceptJSON(xhr) {
    xhr.setRequestHeader('Accept', 'application/json');
}

var konserver = {
    get: function (uri, success, error) {
        $.ajax({
            type: 'GET',
            url: uri,
            beforeSend: acceptJSON,
            dataType: 'json',
            success: success,
            error: error
        });
    },
    del: function (uri, success, error) {
        $.ajax({
            type: 'POST',
            url: uri,
            data: {_method: 'DELETE'},
            beforeSend: acceptJSON,
            dataType: 'text',
            success: success,
            error: error
        });
    }
};

$(document).ready(function () {
    
    $("#edit-panel").hide();
    
    $("#edit-link").click(function (e) {
        $("form")[0].reset();
        $("#show-panel").hide();
        if (onEdit) {
            onEdit();
        } else {
            $("#edit-panel").show();
        }
        e.preventDefault();
    });
    
    $("#cancel-link").click(function (e) {
        $("#edit-panel").hide();
        $("#show-panel").show();
        e.preventDefault();
    });
    
    if (onReady) {
        onReady();
    }
    
});

