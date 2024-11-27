import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

import { TOOLBAR_VARIANTS } from '../../../shared/enums';
import { UVEStore } from '../../../store/dot-uve.store';
import { DotEmaBookmarksComponent } from '../dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from '../dot-ema-info-display/dot-ema-info-display.component';

@Component({
    selector: 'dot-uve-toolbar',
    standalone: true,
    imports: [ButtonModule, ToolbarModule, DotEmaBookmarksComponent, DotEmaInfoDisplayComponent],
    templateUrl: './dot-uve-toolbar.component.html',
    styleUrl: './dot-uve-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveToolbarComponent {
    #store = inject(UVEStore);

    readonly $toolbar = this.#store.$uveToolbar;

    TOOLBAR_VARIANTS = TOOLBAR_VARIANTS;

    togglePreviewMode(preview: boolean) {
        this.#store.togglePreviewMode(preview);
    }
}
