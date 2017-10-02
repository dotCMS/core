/**
 * Created by fmontes on 7/28/16.
 */
import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
    name : 'lowercase',
})

/**
 * Put in lowercase the received string
 */
export class LowercasePipe implements PipeTransform {

    transform(value: string, args: Array<any>): string {
        return value === undefined || value === '' ? '' : value.toLowerCase();
    }

}
