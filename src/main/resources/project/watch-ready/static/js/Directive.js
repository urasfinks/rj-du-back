if (!Array.prototype.includes) {
    Object.defineProperty(Array.prototype, 'includes', {
        value: function (obj) {
            var i = this.length;
            while (i--) {
                if (this[i] === obj) {
                    return true;
                }
            }
            return false;
        }
    });
}
if (!String.prototype.startsWith) {
    Object.defineProperty(String.prototype, 'startsWith', {
        value: function (search, rawPos) {
            var pos = rawPos > 0 ? rawPos | 0 : 0;
            return this.substring(pos, pos + search.length) === search;
        }
    });
}

function map(data, arguments) {
    var result = data;
    if (arguments.length % 2 != 0) {
        //Если кол-во не чётное значит есть значение по умолчанию
        result = arguments.last;
        arguments.removeLast();
    }
    for (var i = 0; i < arguments.length; i += 2) {
        if (arguments[i] == data.toString()) {
            return arguments[i + 1];
        }
    }
    return result;
}

function timeSoFar(data, arguments, ctx) {
    if (data == null || data == "") {
        return "неизвестно";
    }
    var curTimestampMillis = Date.now();
    var d = Date.now() / 1000;
    if (isInt(data)) {
        d = data * 1;
    }
    if (arguments.length > 0) {
        if (arguments[0] == "sec") {
            d *= 1000;
        }
    }
    var diffMs = curTimestampMillis - d;
    var diffDays = Math.floor(diffMs / 86400000);
    if (diffDays > 0) {
        return diffDays + "д.";
    }
    var diffHrs = Math.floor((diffMs % 86400000) / 3600000);
    if (diffHrs > 0) {
        return diffHrs + "ч.";
    }
    var diffMins = Math.floor(((diffMs % 86400000) % 3600000) / 60000);
    if (diffMins > 0) {
        return diffMins + "мин.";
    }
    var diffSeconds = Math.floor(((diffMs % 86400000) % 3600000) / 1000);
    if (diffSeconds > 0) {
        return diffSeconds + "сек.";
    }
    return "сейчас";
}

function isInt(value) {
    return !isNaN(value) &&
        parseInt(Number(value)) == value &&
        !isNaN(parseInt(value, 10));
}