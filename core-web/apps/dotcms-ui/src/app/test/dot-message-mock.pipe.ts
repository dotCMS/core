import { Pipe, PipeTransform } from '@angular/core';

/**
 * Mock of DotMessagePipe
 * use only with tests
 */
@Pipe({ name: 'dm', standalone: true })
export class DotMessageMockPipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}
