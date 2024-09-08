window.$$ = function (id) {
    return document.getElementById(id);
}

function getName(fullPath) {
    //var fullPath = document.getElementById('upload').value;
    if (fullPath) {
        var startIndex = (fullPath.indexOf('\\') >= 0 ? fullPath.lastIndexOf('\\') : fullPath.lastIndexOf('/'));
        var filename = fullPath.substring(startIndex);
        if (filename.indexOf('\\') === 0 || filename.indexOf('/') === 0) {
            filename = filename.substring(1);
        }
        filename = JSON.stringify(filename).replace(" ", "_").replace(/[^0-9a-zA-Z\\._]/gi, "");
        if (filename.length > 255) {
            filename = filename.substring(filename.length - 255);
        }
        return filename;
    }
}

function ajax(url, formData, callback) {
    $$("error").innerHTML = "";
    var xmlhttp = new XMLHttpRequest();

    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == XMLHttpRequest.DONE) { // XMLHttpRequest.DONE == 4
            if (xmlhttp.status == 200) {
                var data = JSON.parse(xmlhttp.responseText);
                callback(data);
            } else if (xmlhttp.status == 0) {
                $$("error").innerHTML = 'Server error';
            }
        }
    };

    xmlhttp.open(formData != undefined ? "POST" : "GET", url, true);
    if (formData != undefined) {
        xmlhttp.send(formData);
    } else {
        xmlhttp.send();
    }
}

window.fileSelected = function (obj) {
    var files = obj.files;
    var ar = [];
    for (var x = 0; x < files.length; x++) {
        ar.push(files[x]);
    }
    window.sendFiles(ar);
}

window.sendFiles = function (files) {
    let file = files.pop();
    if (file != null && file != undefined) {
        var fd = new FormData();
        fd.append("file", file);
        fd.append("uuid", getName(file.name));
        console.log(fd);
        ajax("/BlobUpload", fd, function (data) {
            console.log(data);
            window.sendFiles(files);
        });
    }
}

onReady(function () {
    //alert("Ready");
});