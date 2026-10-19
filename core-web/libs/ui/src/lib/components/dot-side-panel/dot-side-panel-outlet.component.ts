import { Component, inject } from '@angular/core';

import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { DotSidePanelContentDirective } from './dot-side-panel-content.directive';
import { DotSidePanelComponent } from './dot-side-panel.component';

import { DotSidePanelService } from '../../services/dot-side-panel/dot-side-panel.service';

/**
 * Where every side panel renders: one drawer per panel `DotSidePanelService` has open, in open
 * order, so the last one opened is in front. Mounted once, at the root of the admin app.
 *
 * Each drawer's close button, Escape and mask click go to the panel's
 * `DotSidePanelRef.requestClose()`, so the content's guard runs first.
 */
@Component({
    selector: 'dot-side-panel-outlet',
    imports: [DotSidePanelComponent, DotSidePanelContentDirective, ProgressSpinnerModule],
    templateUrl: './dot-side-panel-outlet.component.html'
})
export class DotSidePanelOutletComponent {
    protected readonly $panels = inject(DotSidePanelService).panels;
}
