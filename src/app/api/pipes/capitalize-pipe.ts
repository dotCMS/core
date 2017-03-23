/**
 * Created by oswaldogallango on 7/21/16.
 */
import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
    name : 'capitalize',
})

/**
 * Put in uppercase le first character of the string
 */
export class CapitalizePipe implements PipeTransform{

    transform(value: string, args: Array<any>): string {

        if (value === undefined || value === '') {
            value = '';
        } else  {
            value = value.trim().charAt(0).toLocaleUpperCase() + value.trim().substring(1);
        }

        return value;
    }

}
