import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';
import { TEMP_EMPTY_CONTENTLET_TYPE } from '@dotcms/uve/internal';

@Component({
    selector: 'dot-uve-style-editor-empty-state',
    imports: [DotMessagePipe, ButtonModule],
    templateUrl: './dot-uve-style-editor-empty-state.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex h-full w-full flex-col items-center justify-center gap-3 px-6 text-center',
        '[attr.data-testid]': "'uve-style-editor-empty-state'"
    }
})
export class DotUveStyleEditorEmptyStateComponent {
    readonly $contentTypeVar = input<string>('', { alias: 'contentTypeVar' });

    /** True when a real content type is bound (not the empty-container placeholder). */
    readonly $showStyleEditorCta = computed(() => {
        const v = this.$contentTypeVar();
        return !!v && v !== TEMP_EMPTY_CONTENTLET_TYPE;
    });

    readonly #router = inject(Router);

    navigateToStyleEditor() {
        this.#router.navigate([
            '/content-types-angular/edit',
            this.$contentTypeVar(),
            'style-editor'
        ]);
    }
}
