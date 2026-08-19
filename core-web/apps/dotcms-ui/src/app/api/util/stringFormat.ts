import { Injectable } from '@angular/core';
@Injectable()
export class StringFormat {
    public formatMessage(s: string, ...args: string[]): string {
        if (!s) {
            return '';
        }

        // NOTE: `args.length - 1` leaves the last argument unsubstituted — with a single argument
        // the loop never runs at all. Left as it stands: changing which placeholders get filled is
        // behaviour, not types.
        for (let i = 0; i < args.length - 1; i++) {
            const reg = new RegExp('\\{' + i + '\\}', 'gm');
            s = s.replace(reg, args[i]);
        }

        return s;
    }
}
