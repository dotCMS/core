import { Injectable } from "@angular/core";
@Injectable()
export class StringFormat {
    public formatMessage(s: string, ...args: string[]): string {
        if (s) {
            for (let i = 0; i < args.length - 1; i++) {
                const reg = new RegExp('\\{' + i + '\\}', 'gm');
                s = s.replace(reg, arguments[i]);
            }
            return s;
        }
    }
}
