import { Pipe, PipeTransform, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

@Pipe({
    name: 'dm',
    pure: true
})
export class DotMessagePipe implements PipeTransform {
    private dotMessageService = inject(DotMessageService);

    transform(value: string, args: string[] = []): string {
        return value ? this.dotMessageService.get(value, ...args) : '';
    }
}
