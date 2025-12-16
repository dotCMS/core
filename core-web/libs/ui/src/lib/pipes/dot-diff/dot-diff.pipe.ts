import HtmlDiff from 'htmldiff-js';

import { Pipe, PipeTransform } from '@angular/core';

/**
 * HtmlDiff library breaks the data attribute, so we need to remove it before.
 * @param html
 * @returns
 */
const removeDataAttr = function (html: string): string {
    return html.replace(/(<[^>]+) data=".*?"/gi, '$1');
};

@Pipe({
    name: 'dotDiff'
})
export class DotDiffPipe implements PipeTransform {
    transform(oldValue: string, newValue: string, showDiff = true): string {
        newValue = newValue || '';

        return showDiff
            ? HtmlDiff.execute(oldValue ? removeDataAttr(oldValue) : '', removeDataAttr(newValue))
            : newValue;
    }
}
