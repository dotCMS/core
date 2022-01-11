import { Pipe, PipeTransform } from '@angular/core';
import HtmlDiff from 'htmldiff-js';

@Pipe({
    name: 'dotDiff'
})
export class DotDiffPipe implements PipeTransform {
    transform(oldValue: string, newValue: string, showDiff = true): string {
        return showDiff ? HtmlDiff.execute(oldValue, newValue) : newValue;
    }
}
