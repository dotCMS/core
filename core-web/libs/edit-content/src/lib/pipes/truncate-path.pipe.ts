import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'truncatePath',
    standalone: true
})
export class TruncatePathPipe implements PipeTransform {
    transform(value: string): string {
        const split = value.split('/').filter((item) => item !== '');
        if (split.length > 0) {
            return split.pop();
        }

        return value;
    }
}
