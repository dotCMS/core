import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { StructureTypeView } from '@models/contentlet/structure-type-view.model';
import { Observable } from 'rxjs';
import { DotMessageService } from '@services/dot-messages-service';
import { SelectItem } from 'primeng/primeng';
import { map, take } from 'rxjs/operators';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';

@Component({
    selector: 'dot-base-type-selector',
    templateUrl: './dot-base-type-selector.component.html',
    styleUrls: ['./dot-base-type-selector.component.scss']
})
export class DotBaseTypeSelectorComponent implements OnInit {
    @Input() value: SelectItem;
    @Output() selected = new EventEmitter<string>();

    options: Observable<SelectItem[]>;

    constructor(
        private dotContentTypeService: DotContentTypeService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.options = this.dotContentTypeService
            .getAllContentTypes()
            .pipe(take(1), map((structures: StructureTypeView[]) => this.setOptions(structures)));
    }

    change(item: SelectItem) {
        this.selected.emit(item.value);
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
