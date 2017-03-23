export class StringFormat {
    formatMessage(): string {
        let s = arguments[0];
        if (s) {
            for (let i = 0; i < arguments.length - 1; i++) {
                let reg = new RegExp('\\{' + i + '\\}', 'gm');
                s = s.replace(reg, arguments[i + 1]);
            }
            return s;
        }
    }
}