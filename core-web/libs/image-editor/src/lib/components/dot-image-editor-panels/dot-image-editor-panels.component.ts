import { ChangeDetectionStrategy, Component } from '@angular/core';

import { AccordionModule } from 'primeng/accordion';

import { DotMessagePipe } from '@dotcms/ui';

import { DotImageEditorAdjustPanelComponent } from './dot-image-editor-adjust-panel/dot-image-editor-adjust-panel.component';
import { DotImageEditorFileInfoPanelComponent } from './dot-image-editor-fileinfo-panel/dot-image-editor-fileinfo-panel.component';
import { DotImageEditorHistoryPanelComponent } from './dot-image-editor-history-panel/dot-image-editor-history-panel.component';
import { DotImageEditorTransformPanelComponent } from './dot-image-editor-transform-panel/dot-image-editor-transform-panel.component';

/**
 * Side panel container of the image editor dialog. Stacks the adjust, transform,
 * file info and applied-edits sub-panels in a multi-expand PrimeNG accordion so
 * the user can keep several sections open at once. Each sub-panel is fully
 * self-contained and talks to the {@link ImageEditorStore} on its own; this
 * container only owns the accordion layout and section headers.
 */
@Component({
    selector: 'dot-image-editor-panels',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        AccordionModule,
        DotMessagePipe,
        DotImageEditorAdjustPanelComponent,
        DotImageEditorTransformPanelComponent,
        DotImageEditorFileInfoPanelComponent,
        DotImageEditorHistoryPanelComponent
    ],
    templateUrl: './dot-image-editor-panels.component.html'
})
export class DotImageEditorPanelsComponent {}
