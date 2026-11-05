import { Pipe, PipeTransform, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

@Pipe({
    name: 'dm',
    pure: false
})
export class DotMessagePipe implements PipeTransform {
    private dotMessageService = inject(DotMessageService);

    private lastValue = '';
    private lastArgs: string[] = [];
    private lastVersion: string | null | undefined;
    private lastLanguage: string | null | undefined;
    private lastResult = '';

    transform(value: string, args: string[] = []): string {
        // Read on every check so bundle installation also marks OnPush consumers dirty.
        // Optional access keeps get-only service mocks compatible.
        const version = this.dotMessageService.loadedBundleVersion?.();
        const language = this.dotMessageService.loadedBundleLanguage?.();
        if (!value) {
            return '';
        }

        if (
            version !== undefined &&
            value === this.lastValue &&
            version === this.lastVersion &&
            language === this.lastLanguage &&
            args.length === this.lastArgs.length &&
            args.every((arg, index) => arg === this.lastArgs[index])
        ) {
            return this.lastResult;
        }

        this.lastResult = this.dotMessageService.get(value, ...args);
        this.lastValue = value;
        this.lastVersion = version;
        this.lastLanguage = language;
        this.lastArgs = [...args];
        return this.lastResult;
    }
}
