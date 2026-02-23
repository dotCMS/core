import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-uve-style-editor-empty-state',
    imports: [DotMessagePipe],
    templateUrl: './dot-uve-style-editor-empty-state.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex h-full w-full flex-col items-center justify-center gap-4 px-6 py-10 text-center'
    }
})
export class DotUveStyleEditorEmptyStateComponent {}
