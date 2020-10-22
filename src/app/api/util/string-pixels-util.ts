import * as _ from 'lodash';
import { Injectable } from '@angular/core';
@Injectable()
export class StringPixels {
    private static readonly characterSize = 7;
    private static readonly arrowDropdownComponentSize = 32;

    /**
     * Returns an estimate of the width in pixels that may have the longer
     * text from a collection, based on a character constant
     * @param Array<string> textValues The text to be measure.
     *
     */
    public static getDropdownWidth(textValues: Array<string>): string {
        const maxText = _.maxBy(textValues, (text: string) => text.length).length;
        const maxWidth =
            StringPixels.characterSize * maxText > 108 ? 108 : StringPixels.characterSize * maxText;
        return `${maxWidth + StringPixels.arrowDropdownComponentSize}px`;
    }
}
