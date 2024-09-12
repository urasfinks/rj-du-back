window.$$ = function (id) {
    return document.getElementById(id);
}

function lsChange(obj) {
    localStorage.setItem(obj.id, obj.value);
}

function getName(fullPath) {
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
    window.selectedFiles = ar;
    window.reloadSelectedFiles();

    //
}
window.selectedFiles = [];
window.reloadSelectedFiles = function () {
    var template = $$("template").innerHTML;
    $$("selected-files").innerHTML = "";
    for (var i = 0; i < window.selectedFiles.length; i++) {
        let block = document.createElement("div");
        block.innerHTML = template;
        block.getElementsByClassName("uuid")[0].value = getName(window.selectedFiles[i].name).toLowerCase();
        block.getElementsByClassName("idx")[0].idx = i;
        block.getElementsByClassName("file-index")[0].innerHTML = i + "";
        var ar = ["key", "meta", "lazy_sync"];
        for (var j = 0; j < ar.length; j++) {
            block.getElementsByClassName(ar[j])[0].value = $$(ar[j]).value;
        }
        $$("selected-files").appendChild(block);
    }
    $$("btn-load-to-server").style.display = window.selectedFiles.length == 0 ? "none" : "inline";
}
window.removeSelectedFile = function (idx) {
    window.selectedFiles.splice(idx, 1);
    window.reloadSelectedFiles();
}

window.sendFiles = function () {
    var list = $$("selected-files").childNodes;
    var ar = [];
    for (var x = 0; x < list.length; x++) {
        ar.push(list[x]);
    }
    window.handleQueue(ar);
}
window.handleQueue = function (ar) {
    let block = ar.pop();
    if (block != null && block != undefined) {
        var fd = new FormData();
        var idx = block.getElementsByClassName("file-index")[0].innerHTML * 1;
        fd.append("file", window.selectedFiles[idx]);
        var field = ["uuid", "key", "meta", "lazy_sync"];
        for (var i = 0; i < field.length; i++) {
            var val = block.getElementsByClassName(field[i])[0].value;
            if (val != undefined && val.trim() != "") {
                fd.append(field[i], val);
            }
        }
        ajax("/BlobUpload", fd, function (data) {
            window.removeSelectedFile(idx);
            if (data.status == undefined || data.status != true) {
                alert(JSON.stringify(data));
            }
            window.handleQueue(ar);
        });
    } else {
        alert("Готово");
    }
}

onReady(function () {
    var ar = ["key", "meta", "lazy_sync"];
    for (var i = 0; i < ar.length; i++) {
        var lastKey = localStorage.getItem(ar[i]);
        if (lastKey !== undefined) {
            document.getElementById(ar[i]).value = lastKey;
        }
    }
});