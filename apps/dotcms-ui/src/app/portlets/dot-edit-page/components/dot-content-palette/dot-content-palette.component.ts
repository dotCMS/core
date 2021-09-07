import { Component, Input, OnDestroy } from '@angular/core';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { Subject } from 'rxjs';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';

@Component({
    selector: 'dot-content-palette',
    templateUrl: './dot-content-palette.component.html',
    styleUrls: ['./dot-content-palette.component.scss']
})
export class DotContentPaletteComponent implements OnDestroy {
    @Input() items: DotCMSContentType[] = [];
    filter: string;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(private dotContentletEditorService: DotContentletEditorService) {}

    /**
     * Notify the dragging element to the service, and finally to the edit iframe.
     *
     * @param DotCMSContentType contentType
     * @memberof DotContentPaletteComponent
     */
    dragStart(contentType: DotCMSContentType): void {
        this.dotContentletEditorService.setDraggedContentType(contentType);
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}
