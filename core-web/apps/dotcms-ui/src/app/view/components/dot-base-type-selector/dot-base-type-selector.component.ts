import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { SelectChangeEvent, SelectModule } from 'primeng/select';

import { map, take } from 'rxjs/operators';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { StructureTypeView } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-base-type-selector',
    templateUrl: './dot-base-type-selector.component.html',
    styleUrls: ['./dot-base-type-selector.component.scss'],
    imports: [CommonModule, SelectModule, FormsModule]
})
export class DotBaseTypeSelectorComponent implements OnInit {
    private dotContentTypeService = inject(DotContentTypeService);
    private dotMessageService = inject(DotMessageService);

    @Input() value: SelectItem;
    @Output() selected = new EventEmitter<string>();

    options: Observable<SelectItem[]>;

    ngOnInit() {
        this.options = this.dotContentTypeService.getAllContentTypes().pipe(
            take(1),
            map((structures: StructureTypeView[]) => this.setOptions(structures))
        );
    }

    change(event: SelectChangeEvent) {
        this.selected.emit(event.value);
    }

    setOptions(baseTypes: StructureTypeView[]): SelectItem[] {
        return [
            {
                label: this.dotMessageService.get('contenttypes.selector.any.content.type'),
                value: ''
            },
            ...baseTypes.map((structure: StructureTypeView) => ({
                label: structure.label,
                value: structure.name
            }))
        ];
    }
}
