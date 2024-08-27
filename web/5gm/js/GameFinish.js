function finish(data){
    console.log(data);
    $("#desc").text(data.finishDescription);
    var newStateData = {};
    newStateData["user"] = getUsers(data);
    newStateData["gridWord"] = getGridWord(data);
    calculateScore(data, newStateData);
    $("#scoreRedFinish").text(newStateData.scoreRed);
    $("#allRedFinish").text(newStateData.allRed);
    $("#scoreBlueFinish").text(newStateData.scoreBlue);
    $("#allBlueFinish").text(newStateData.allBlue);
    $("#usersRedFinish").text(newStateData.user.red);
    $("#usersBlueFinish").text(newStateData.user.blue);
    $("#gridWordFinish").html(newStateData.gridWord);
}