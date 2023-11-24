function word(socketData){
    var newStateData = {};
    newStateData["gridWord"] = getGridWord(socketData);
    $("#genWord").html(newStateData.gridWord);
}