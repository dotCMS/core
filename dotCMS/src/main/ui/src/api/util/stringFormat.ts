export class StringFormat {
    formatMessage() {
        var s = arguments[0];
        if (s) {
            for (var i = 0; i < arguments.length - 1; i++) {
                var reg = new RegExp('\\{' + i + '\\}', 'gm');
                s = s.replace(reg, arguments[i + 1]);
            }
            return s;
        }
    }
}