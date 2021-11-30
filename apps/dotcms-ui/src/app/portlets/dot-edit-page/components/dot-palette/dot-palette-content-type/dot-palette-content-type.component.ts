import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotPaletteInputFilterComponent } from '../dot-palette-input-filter/dot-palette-input-filter.component';

@Component({
    selector: 'dot-palette-content-type',
    templateUrl: './dot-palette-content-type.component.html',
    styleUrls: ['./dot-palette-content-type.component.scss']
})
export class DotPaletteContentTypeComponent implements OnChanges {
    @ViewChild('filterInput', { static: true })
    filterInput: DotPaletteInputFilterComponent;
    @Input() items: DotCMSContentType[] = [];
    @Output() show = new EventEmitter<string>();
    itemsFiltered: DotCMSContentType[];

    constructor(private dotContentletEditorService: DotContentletEditorService) {}

    ngOnChanges(changes: SimpleChanges) {
        if (!changes?.items?.firstChange && changes?.items?.currentValue) {
            this.itemsFiltered = [...this.items];
        }
    }

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
        this.itemsFiltered = [...this.items];
        this.show.emit(contentTypeVariable);
    }

    /**
     * Does a filtering of the Content Types based on value from the filter component
     *
     * @param string value
     * @memberof DotPaletteContentTypeComponent
     */
    filterContentTypes(value: string): void {
        this.itemsFiltered = this.items.filter((item) =>
            item.name.toLowerCase().includes(value.toLowerCase())
        );
    }
}
