import { ChangeDetectionStrategy, Component, inject, computed } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../store/dot-uve.store';
@Component({
    selector: 'dot-dot-uve-error',
    standalone: true,
    imports: [DotMessagePipe],
    templateUrl: './dot-uve-error.component.html',
    styleUrl: './dot-uve-error.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveErrorComponent {
    readonly #store = inject(UVEStore);

    readonly $info = computed(() => {
        const errorCode = this.#store.errorCode();

        if (errorCode === 404) {
            return {
                icon: 'pi-search',
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
