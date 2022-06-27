import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotPaletteInputFilterComponent } from '../dot-palette-input-filter/dot-palette-input-filter.component';

@Component({
    selector: 'dot-palette-content-type',
    templateUrl: './dot-palette-content-type.component.html',
    styleUrls: ['./dot-palette-content-type.component.scss']
})
export class DotPaletteContentTypeComponent {
    @ViewChild('filterInput', { static: true }) filterInput: DotPaletteInputFilterComponent;

    @Input() items: DotCMSContentType[] = [];
    @Input() loading = true;

    @Output() selected = new EventEmitter<string>();
    @Output() filter = new EventEmitter<string>();

    constructor(private dotContentletEditorService: DotContentletEditorService) {}

    /**
     * Set the content Type being dragged from the Content palette to dotContentletEditorService
     *
     * @param DotCMSContentType contentType
     * @memberof DotPaletteContentTypeComponent
     */
    dragStart(contentType: DotCMSContentType): void {
        this.dotContentletEditorService.setDraggedContentType(contentType);
    }

    /**
     * Emits the Content Type variable name to show contentlets and clears
     * component's local variables
     *
     * @param string contentTypeVariable
     * @memberof DotPaletteContentTypeComponent
     */
    showContentTypesList(contentTypeVariable: string): void {
        this.filterInput.searchInput.nativeElement.value = '';
        this.selected.emit(contentTypeVariable);
    }

    /**
     * Does a filtering of the Content Types based on value from the filter component
     *
     * @param string value
     * @memberof DotPaletteContentTypeComponent
     */
    filterContentTypes(value: string): void {
        this.filter.emit(value);
    }

    /**
     * Focus the filter input
     *
     * @memberof DotPaletteContentTypeComponent
     */
    focusInputFilter() {
        this.filterInput.focus();
    }
}
