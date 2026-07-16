import { Pipe, PipeTransform } from '@angular/core';

/**
 * Mock of DotMessagePipe
 * use only with tests
 * @override DotMessagePipe pipe
 */
@Pipe({ name: 'dm' })
export class DotMessagePipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}
