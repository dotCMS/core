import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { PaginatorService } from '@services/paginator';
import { ContentType } from '../../../../content-types/shared/content-type.model';
import { DotMessageService } from '@services/dot-messages-service';
import { LazyLoadEvent } from 'primeng/primeng';
import { take } from 'rxjs/operators';

@Component({
    providers: [PaginatorService],
    selector: 'dot-form-selector',
    templateUrl: './dot-form-selector.component.html',
    styleUrls: ['./dot-form-selector.component.scss']
})
export class DotFormSelectorComponent implements OnInit {
    @Input()
    show = false;
    @Output()
    select = new EventEmitter<ContentType>();
    @Output()
    close = new EventEmitter<any>();

    items: ContentType[];
    messages: {
        [key: string]: string;
    } = {};

    constructor(public paginatorService: PaginatorService, private dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['contenttypes.form.name', 'Select', 'modes.Add-Form'])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messages = messages;
            });

        this.paginatorService.url = 'v1/contenttype?type=FORM';
    }

    /**
     * Call when click on any pagination link
     *
     * @param {LazyLoadEvent} event
     * @memberof DotFormSelectorComponent
     */
    loadData(event: LazyLoadEvent): void {
        this.paginatorService
            .getWithOffset(event.first)
            .pipe(take(1))
            .subscribe((items: ContentType[]) => {
                this.items = items;
            });
    }
}
