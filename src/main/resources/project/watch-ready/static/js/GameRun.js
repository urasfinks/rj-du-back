function run(data) {
    console.log(data);
    $("#runTeam").text(map(data.runTeam, ["blue", "СИНИХ", "red", "КРАСНЫХ"]));
    $("#runTeam").addClass("bg-" + data.runTeam);
    curTime = data.session;
    renderSession();
    var newStateData = {};
    newStateData["user"] = getUsers(data);
    newStateData["gridWord"] = getGridWord(data);
    calculateScore(data, newStateData);
    $("#scoreRed").text(newStateData.scoreRed);
    $("#allRed").text(newStateData.allRed);
    $("#scoreBlue").text(newStateData.scoreBlue);
    $("#allBlue").text(newStateData.allBlue);
    $("#usersRed").text(newStateData.user.red);
    $("#usersBlue").text(newStateData.user.blue);
    $("#gridWord").html(newStateData.gridWord);
}

var toSessionTime = undefined, curTime = 0;

function renderSession() {
    try {
        clearTimeout(toSessionTime);
    } catch (e) {
    }
    $("#session").text(timeSoFar(curTime, ["sec"]));
    toSessionTime = setTimeout(function () {
        renderSession();
    }, 1000);
}

function getGridWord(socketData) {
    var listCard = [];
    for (var key in socketData) {
        if (key.startsWith("card")) {
            socketData[key]["index"] = key.split("card")[1];
            listCard.push(socketData[key]);
        }
    }
    var canBePressed = false;
    var isCapt = false;

    listCard.sort(function (a, b) {
        return a["index"] - b["index"];
    });
    var counter = 0;
    var column = [];
    var matrix = {col: 4, row: 7};
    var colorCard = {
        "red": "#e64331",
        "blue": "#4695ef",
        "neutral": "#efd9b9",
        "die": "#000000"
    };
    var colorText = {
        "red": "#ffffff",
        "blue": "#ffffff",
        "neutral": "#000000",
        "die": "#ffffff"
    };
    var mapCount = {};
    var mapCountMy = {};
    for (var key in socketData) {
        if (key.startsWith("tapCard_")) {
            var curKey = "i" + key.split("tapCard_")[1].split("_")[0];
            var amI = key.split("tapCard_")[1].split("_")[1];
            if (mapCount[curKey] === undefined) {
                mapCount[curKey] = 1;
            } else {
                mapCount[curKey]++;
            }
        }
    }
    for (var c = 0; c < matrix.row; c++) {
        var row = [];
        for (var i = 0; i < matrix.col; i++) {
            var curIndex = counter++;
            var cardData = listCard[curIndex];
            var onTap = null;
            if (cardData != undefined) {
                var tapCount = mapCount["i" + curIndex] != undefined ? mapCount["i" + curIndex] : null;
                if (cardData["selected"] != null) {
                    tapCount = null;
                }
                //Если карточка выбрана - показываем её реальный цвет
                var curColorCard = (isCapt || cardData["selected"] != null) ? colorCard[cardData["team"]] : "#efd9b9";
                var curColorText = (isCapt || cardData["selected"] != null) ? colorText[cardData["team"]] : "#000000";
                var decoration = "none";
                var curWeightText = "bold";
                if (cardData["selected"] != null) {
                    curWeightText = "normal";
                    curColorText = "#777";
                    curColorCard = "#333";
                    if (cardData["selected"] !== cardData["team"]) {
                        decoration = "lineThrough";
                    }
                    if(cardData["team"] == "die"){
                        curColorText = "#e64331";
                    }
                }

                row.push({
                    curColorCard: curColorCard,
                    label: cardData["label"].toUpperCase(),
                    decoration: decoration,
                    curWeightText: curWeightText,
                    curColorText: curColorText,
                    tapCount: tapCount,
                    selected: cardData["selected"],
                    cardTeam: cardData["team"]
                });
            }
        }
        column.push(row);
    }
    var resultTable = "<table class='gridWord'>";
    for (var i = 0; i < column.length; i++) {
        resultTable += "<tr>";
        for (var j = 0; j < column[i].length; j++) {
            resultTable += "<td style='width: 25%;'>";

            var complexClass = "card";
            complexClass += " decoration-" + column[i][j].decoration;
            complexClass += " weight-" + column[i][j].curWeightText;

            var complexStyle = "";
            complexStyle += " background-color: " + column[i][j].curColorCard + ";";
            complexStyle += " color: " + column[i][j].curColorText + ";";

            resultTable += "<div class='" + complexClass + "' style='" + complexStyle + "'>";
            resultTable += column[i][j].label;

            if (column[i][j].tapCount != null) {
                resultTable += "<div class='tapCount'>" + column[i][j].tapCount + "</div>";
            } else if (column[i][j].selected != null) {
                resultTable += "<div class='selected bg-" + column[i][j].cardTeam + "'></div>";
            }

            resultTable += "</div>";
            resultTable += "</td>";
        }
        resultTable += "</tr>";
    }
    console.log(column);
    return resultTable + "<table>";
}

function calculateScore(socketData, newStateData) {
    var blue = 0, red = 0, allBlue = 0, allRed = 0;
    for (var key in socketData) {
        if (key.startsWith("card")) {
            if (socketData[key]["team"] === "red") {
                allRed++;
            }
            if (socketData[key]["team"] === "blue") {
                allBlue++;
            }
            if (socketData[key]["selected"] != undefined && socketData[key]["selected"] != null) {
                if (socketData[key]["team"] === "blue") {
                    blue++;
                }
                if (socketData[key]["team"] === "red") {
                    red++;
                }
            }
        }
    }
    console.log("Score: blue: " + blue + "; allBlue: " + allBlue + "; red: " + red + "; allRed: " + allRed);
    newStateData["scoreRed"] = red;
    newStateData["scoreBlue"] = blue;
    newStateData["allBlue"] = allBlue;
    newStateData["allRed"] = allRed;
}

function getUsers(socketData) {
    var reds = [], blues = [];
    for (var key in socketData) {
        if (key.startsWith("user") && socketData[key]["role"] === "captain") {
            if (socketData[key]["team"] === "red") {
                reds.push(socketData[key]["name"] + " (Капитан)");
            } else if (socketData[key]["team"] === "blue") {
                blues.push(socketData[key]["name"] + " (Капитан)");
            }
        }
    }
    for (var key in socketData) {
        if (key.startsWith("user") && socketData[key]["role"] !== "captain") {
            if (socketData[key]["team"] === "red") {
                reds.push(socketData[key]["name"]);
            } else if (socketData[key]["team"] === "blue") {
                blues.push(socketData[key]["name"]);
            }
        }
    }
    return {
        "red": reds.join(", "),
        "blue": blues.join(", "),
    }
}