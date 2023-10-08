function team(socketData) {
    console.log(socketData);
    $("#teamUndefined").html(getListPersonGroup(socketData, "undefined"));
    $("#teamRed").html(getListPersonGroup(socketData, "red"));
    $("#teamBlue").html(getListPersonGroup(socketData, "blue"));
}

function getListPersonGroup(socketData, team) {
    var result = [];
    var listPerson = getListPerson(socketData);
    listPerson.sort(function (a, b) {
        return a["role"] === "captain" ? -1 : 1;
    });
    for (var i = 0; i < listPerson.length; i++) {
        var curTeam = ["red", "blue"].includes(listPerson[i]["team"]) ? listPerson[i]["team"] : "undefined";
        if (curTeam === team) {
            var isCaptain = listPerson[i]["role"] === "captain" ? "Капитан " : "";
            var name = listPerson[i]["name"];
            if (listPerson[i]["static"] != undefined && listPerson[i]["static"] === true) {
                name = "[" + name + "]";
            }
            result.push({
                "label": isCaptain + name
            });
        }
    }
    var html = "";
    for (var key in result) {
        html += "<div class='p1'>" + result[key].label + "</div>";
    }
    return html;
}

function getListPerson(socketData) {
    var listPerson = [];
    for (var key in socketData) {
        if (key.startsWith("user")) {
            socketData[key]["id"] = key.substring(4);
            listPerson.push(socketData[key]);
        }
    }
    return listPerson;
}