var uuid = location.href.split("#")[1];
var protocol = location.protocol.toLowerCase() == "https:" ? "wss" : "ws";
var url = protocol + "://" + location.host.toString() + "/socket/" + uuid;

function error(data) {
    alert(data);
}

function add(data) {
    try {
        show(data["gameState"]);
        window[data["gameState"]](data);
    } catch (e) {
        alert(e.stack);
    }
}

function show(id) {
    var list = ["team", "word", "run", "finish"];
    for (var k in list) {
        if (id === list[k]) {
            $("#" + list[k]).show();
        } else {
            $("#" + list[k]).hide();
        }
    }
}

var toOpen = undefined;

function reconnect() {
    try {
        clearTimeout(toOpen);
    } catch (e) {
    }
    toOpen = setTimeout(function () {
        connect();
    }, 4000);
}

function connect() {
    var socket = new WebSocket(url);
    socket.onopen = function (e) {
        socket.send(JSON.stringify({
            request: {
                handler: "SUBSCRIBE",
                uuid_data: uuid
            }
        }));
        getData();
    };
    socket.onmessage = function (event) {
        getData();
    };
    socket.onclose = function (e) {
        console.log('Socket is closed. Reconnect will be attempted in 1 second.', e.reason);
        reconnect();
    };

    socket.onerror = function (err) {
        socket.close();
        reconnect();
    };
}

function getData() {
    try {
        $.ajax({
            url: '/Data',
            method: 'post',
            contentType: "application/json",
            dataType: 'json',
            data: JSON.stringify({uuid: uuid}),
            success: function (data) {
                add(JSON.parse(data.data.data));
            },
            error: function (data) {
                error(JSON.parse(data.responseText).description);
            }
        });
    } catch (e) {
        error(e.toString());
    }
}

try {
    connect();
} catch (e) {
    error(e);
}