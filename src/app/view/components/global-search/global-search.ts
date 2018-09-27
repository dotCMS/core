import { Component, ViewEncapsulation, OnInit } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-global-search',
    styleUrls: ['./global-search.scss'],
    templateUrl: 'global-search.html'
})
export class GlobalSearchComponent implements OnInit {
    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService.getMessages(['search']).subscribe((res) => {
            this.i18nMessages = res;
        });
    }
}
