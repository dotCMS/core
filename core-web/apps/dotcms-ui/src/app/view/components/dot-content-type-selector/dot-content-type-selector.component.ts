import { Observable } from 'rxjs';

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

import { SelectItem } from 'primeng/api';

import { map, take } from 'rxjs/operators';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-content-type-selector',
    templateUrl: './dot-content-type-selector.component.html',
    styleUrls: ['./dot-content-type-selector.component.scss']
})
export class DotContentTypeSelectorComponent implements OnInit {
    @Input() value: SelectItem;
    @Output() selected = new EventEmitter<string>();

    options$: Observable<SelectItem[]>;

    constructor(
        private dotContentTypeService: DotContentTypeService,
        private dotMessageService: DotMessageService
    ) {}

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
