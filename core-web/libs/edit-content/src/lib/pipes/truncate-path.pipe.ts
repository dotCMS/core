import { Pipe, PipeTransform } from '@angular/core';

/**
 * This pipe is used to truncate the path to the last folder name
 *
 * @export
 * @class TruncatePathPipe
 * @implements {PipeTransform}
 */
@Pipe({
    name: 'truncatePath',
    standalone: true,
    pure: true
})
export class TruncatePathPipe implements PipeTransform {
    transform(value: string): string {
        const split = value.split('/').filter((item) => item !== '');

        return split.pop();
    }
}
