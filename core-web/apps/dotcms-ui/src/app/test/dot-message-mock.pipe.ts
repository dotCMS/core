import { Pipe, PipeTransform } from '@angular/core';

/**
 * Mock of DotMessagePipe
 * use only with tests
 * @override DotMessagePipeModule pipe
 */
@Pipe({ name: 'dm', standalone: true })
export class DotMessagePipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}
