import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-uve-style-editor-empty-state',
    imports: [DotMessagePipe, RouterLink],
    templateUrl: './dot-uve-style-editor-empty-state.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex h-full w-full flex-col items-center justify-center gap-4 px-6 py-10 text-center'
    }
})
export class DotUveStyleEditorEmptyStateComponent {
    readonly $contentTypeVar = input<string>('', { alias: 'contentTypeVar' });
}
