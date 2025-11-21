import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { SelectModule } from 'primeng/select';

import { map, take } from 'rxjs/operators';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-content-type-selector',
    templateUrl: './dot-content-type-selector.component.html',
    styleUrls: ['./dot-content-type-selector.component.scss'],
    imports: [CommonModule, SelectModule, FormsModule]
})
export class DotContentTypeSelectorComponent implements OnInit {
    private dotContentTypeService = inject(DotContentTypeService);
    private dotMessageService = inject(DotMessageService);

    @Input() value: SelectItem;
    @Output() selected = new EventEmitter<string>();

    options$: Observable<SelectItem[]>;

    ngOnInit() {
        this.options$ = this.dotContentTypeService.getContentTypes({ page: 999 }).pipe(
            take(1),
            map((contentTypes: DotCMSContentType[]) => this.setOptions(contentTypes))
        );
    }

    change(item: SelectItem) {
        this.selected.emit(item.value);
    }

    setOptions(contentTypes: DotCMSContentType[]): SelectItem[] {
        return [
            {
                label: this.dotMessageService.get('contenttypes.selector.any.content.type'),
                value: ''
            },
            ...contentTypes.map((contentType: DotCMSContentType) => ({
                label: contentType.name,
                value: contentType.variable
            }))
        ];
    }
}
