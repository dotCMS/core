import { ChangeDetectionStrategy, Component, inject, computed } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../store/dot-uve.store';
@Component({
    selector: 'dot-uve-page-version-not-found',
    imports: [DotMessagePipe],
    templateUrl: './dot-uve-page-version-not-found.component.html',
    styleUrl: './dot-uve-page-version-not-found.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePageVersionNotFoundComponent {
    readonly #store = inject(UVEStore);

    readonly $info = computed(() => {
        const errorCode = this.#store.errorCode();

        if (errorCode === 404) {
            return {
                icon: 'pi-stopwatch',
                title: 'uve.editor.error.404.title',
                description: 'uve.editor.error.404.description'
            };
        }

        return {
            icon: 'pi-exclamation-triangle',
            title: 'uve.editor.error.default.title',
            description: 'uve.editor.error.default.description'
        };
    });
}
