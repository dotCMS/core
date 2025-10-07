import { Component, EventEmitter, Input, Output, ViewChild, inject } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotContentletEditorService } from '../../../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotPaletteInputFilterComponent } from '../dot-palette-input-filter/dot-palette-input-filter.component';

@Component({
    selector: 'dot-palette-contentlets',
    templateUrl: './dot-palette-contentlets.component.html',
    styleUrls: ['./dot-palette-contentlets.component.scss'],
    standalone: false
})
export class DotPaletteContentletsComponent {
    private dotContentletEditorService = inject(DotContentletEditorService);

    @Input() items: DotCMSContentlet[];
    @Input() loading: boolean;
    @Input() totalRecords: number;

    @Output() back = new EventEmitter();
    @Output() filter = new EventEmitter<string>();
    @Output() paginate = new EventEmitter<LazyLoadEvent>();

    itemsPerPage = 25;

    @ViewChild('inputFilter') inputFilter: DotPaletteInputFilterComponent;

    /**
     * Loads data with a specific page
     *
     * @param LazyLoadEvent event
     * @memberof DotPaletteContentletsComponent
     */
    onPaginate(event: LazyLoadEvent): void {
        this.paginate.emit(event);
    }

    /**
     * Clear component and emit back
     *
     * @memberof DotPaletteContentletsComponent
     */
    backHandler(): void {
        this.back.emit();
    }

    /**
     * Set the contentlet being dragged from the Content palette to dotContentletEditorService
     *
     * @param DotCMSContentType contentType
     * @memberof DotPaletteContentletsComponent
     */
    dragStart(contentType: DotCMSContentlet): void {
        this.dotContentletEditorService.setDraggedContentType(contentType);
    }

    /**
     * Does the string formatting in order to do a filtering of the Contentlets,
     * finally call the loadData() to request the data
     *
     * @param string value
     * @memberof DotPaletteContentletsComponent
     */
    filterContentlets(value: string): void {
        value = value.trim();
        this.filter.emit(value);
    }

    /**
     * Focus the input filter
     *
     * @memberof DotPaletteContentletsComponent
     */
    focusInputFilter(): void {
        this.inputFilter.value = '';
        this.inputFilter.focus();
    }
}
