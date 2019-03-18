import {
    DotLargeMessageDisplayService,
    DotLargeMessageDisplayParams
} from './services/dot-large-message-display.service';
import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

@Component({
    selector: 'dot-large-message-display',
    templateUrl: './dot-large-message-display.component.html',
    styleUrls: ['./dot-large-message-display.component.scss']
})
export class DotLargeMessageDisplayComponent implements OnInit {
    data$: Observable<DotLargeMessageDisplayParams>;

    constructor(public dotLargeMessageDisplayService: DotLargeMessageDisplayService) {}

    ngOnInit() {
        this.data$ = this.dotLargeMessageDisplayService.sub();
    }

    close() {
        this.dotLargeMessageDisplayService.clear();
    }

}
