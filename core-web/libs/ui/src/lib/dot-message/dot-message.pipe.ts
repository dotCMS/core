import { Pipe, PipeTransform } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

@Pipe({
    name: 'dm',
    standalone: true,
    pure: true
})
export class DotMessagePipe implements PipeTransform {
    constructor(private dotMessageService: DotMessageService) {}

    transform(value: string, args: string[] = []): string {
        return value ? this.dotMessageService.get(value, ...args) : '';
    }
}
