import {Injectable} from '@angular/core';
/**
 * Encapsulate string utils methods.
 */
@Injectable()
export class StringUtils {
    constructor() {}
    /**
     * Get from text, the line number (indexLine), null if it does not exists.
     * @param text
     * @param indexLine
     * @returns {string}
     */
    getLine (text: string, indexLine: number): string {
        let line: string = null;

        if (text) {
            let lines = text.split('\n');
            line = lines && lines.length > indexLine ? lines[indexLine] : null;
        }

        return line;
    } // getLine.

} // E:O:F:StringUtils.