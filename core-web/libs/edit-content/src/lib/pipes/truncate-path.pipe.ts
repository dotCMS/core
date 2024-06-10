import { Pipe, PipeTransform } from '@angular/core';

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
