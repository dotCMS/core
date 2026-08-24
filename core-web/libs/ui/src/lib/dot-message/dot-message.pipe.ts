import { Pipe, PipeTransform, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

@Pipe({
    name: 'dm',
    pure: true
})
export class DotMessagePipe implements PipeTransform {
    private dotMessageService = inject(DotMessageService);

    // Nullable: the body has always returned '' for a missing key, and callers pass optional
    // labels straight through (`vm.disabledTooltipLabel | dm`). Only the signature disagreed.
    transform(value: string | null | undefined, args: string[] = []): string {
        return value ? this.dotMessageService.get(value, ...args) : '';
    }
}
