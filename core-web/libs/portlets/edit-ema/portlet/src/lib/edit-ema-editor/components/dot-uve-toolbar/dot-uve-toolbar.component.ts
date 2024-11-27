import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../store/dot-uve.store';
import { DotEmaBookmarksComponent } from "../dot-ema-bookmarks/dot-ema-bookmarks.component";

@Component({
    selector: 'dot-uve-toolbar',
    standalone: true,
    imports: [ButtonModule, DotMessagePipe, ToolbarModule, DotEmaBookmarksComponent],
    templateUrl: './dot-uve-toolbar.component.html',
    styleUrl: './dot-uve-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveToolbarComponent {
    #store = inject(UVEStore);

    readonly $toolbarProps = this.#store.$toolbar;

    togglePreviewMode(preview: boolean) {
        this.#store.togglePreviewMode(preview);
    }
}
