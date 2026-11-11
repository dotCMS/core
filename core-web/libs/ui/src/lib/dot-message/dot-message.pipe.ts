import { Pipe, PipeTransform, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';

@Pipe({
    name: 'dm',
    pure: false
})
export class DotMessagePipe implements PipeTransform {
    private dotMessageService = inject(DotMessageService);

    private lastValue = '';
    private lastArgs: string | string[] = [];
    private lastVersion: string | null | undefined;
    private lastLanguage: string | null | undefined;
    private lastResult = '';
    private lastRevision: number | undefined;

    transform(value: string, args: string | string[] = []): string {
        // Read on every check so bundle installation also marks OnPush consumers dirty.
        // Optional access keeps get-only service mocks compatible.
        const version = this.dotMessageService.loadedBundleVersion?.();
        const language = this.dotMessageService.loadedBundleLanguage?.();
        const revision = this.dotMessageService.loadedBundleRevision?.();
        if (!value) {
            return '';
        }

        if (
            version !== undefined &&
            value === this.lastValue &&
            version === this.lastVersion &&
            language === this.lastLanguage &&
            revision === this.lastRevision &&
            (typeof args === 'string'
                ? args === this.lastArgs
                : Array.isArray(this.lastArgs) &&
                  args.length === this.lastArgs.length &&
                  args.every((arg, index) => arg === this.lastArgs[index]))
        ) {
            return this.lastResult;
        }

        this.lastResult = this.dotMessageService.get(value, ...args);
        this.lastValue = value;
        this.lastVersion = version;
        this.lastLanguage = language;
        this.lastRevision = revision;
        this.lastArgs = typeof args === 'string' ? args : [...args];
        return this.lastResult;
    }
}
