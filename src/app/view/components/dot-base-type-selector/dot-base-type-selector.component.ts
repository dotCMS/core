import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DotContentletService } from '../../../api/services/dot-contentlet.service';
import { StructureTypeView } from '../../../shared/models/contentlet/structure-type-view.model';
import { Observable } from 'rxjs/Observable';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { SelectItem } from 'primeng/primeng';
import { map, take } from 'rxjs/operators';

@Component({
    selector: 'dot-base-type-selector',
    templateUrl: './dot-base-type-selector.component.html',
    styleUrls: ['./dot-base-type-selector.component.scss']
})
export class DotBaseTypeSelectorComponent implements OnInit {
    @Input() value: SelectItem;
    @Output() selected = new EventEmitter<string>();

    options: Observable<SelectItem[]>;

    constructor(private dotContentletService: DotContentletService, private dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['contenttypes.selector.any.content.type'])
            .pipe(take(1))
            .subscribe(() => {
                this.options = this.dotContentletService
                    .getContentTypes()
                    .pipe(
                        take(1),
                        map((structures: StructureTypeView[]) =>
                            this.setOptions(this.dotMessageService.get('contenttypes.selector.any.content.type'), structures)
                        )
                    );
            });
    }

    change(item: SelectItem) {
        this.selected.emit(item.value);
    }

    setOptions(allOptions: string, baseTypes: StructureTypeView[]): SelectItem[] {
        return [
            { label: allOptions, value: '' },
            ...baseTypes.map((structure: StructureTypeView) => ({ label: structure.label, value: structure.name }))
        ];
    }
}
