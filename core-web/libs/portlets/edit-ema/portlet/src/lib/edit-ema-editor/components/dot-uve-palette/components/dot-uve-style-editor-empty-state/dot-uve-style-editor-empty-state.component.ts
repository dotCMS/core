import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-uve-style-editor-empty-state',
    imports: [DotMessagePipe, ButtonModule],
    templateUrl: './dot-uve-style-editor-empty-state.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex h-full w-full flex-col items-center justify-center gap-4 px-6 py-10 text-center',
        '[attr.data-testid]': "'uve-style-editor-empty-state'"
    }
})
export class DotUveStyleEditorEmptyStateComponent {
    readonly $contentTypeVar = input<string>('', { alias: 'contentTypeVar' });

    readonly #router = inject(Router);

    navigateToStyleEditor() {
        this.#router.navigate([
            '/content-types-angular/edit',
            this.$contentTypeVar(),
            'style-editor'
        ]);
    }
}
