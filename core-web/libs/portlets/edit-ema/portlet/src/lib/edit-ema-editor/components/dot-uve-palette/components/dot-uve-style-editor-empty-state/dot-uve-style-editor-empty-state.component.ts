import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-uve-style-editor-empty-state',
    imports: [DotMessagePipe],
    templateUrl: './dot-uve-style-editor-empty-state.component.html',
    styleUrl: './dot-uve-style-editor-empty-state.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveStyleEditorEmptyStateComponent {}
