import { Component, OnInit, EventEmitter, Output } from '@angular/core';

import { Observable } from 'rxjs';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';

/**
 * Allow user to add a contentlet to DotCMS instance
 *
 * @export
 * @class DotAddContentletComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-add-contentlet',
    templateUrl: './dot-add-contentlet.component.html',
    styleUrls: ['./dot-add-contentlet.component.scss']
})
export class DotAddContentletComponent implements OnInit {
    @Output()
    close: EventEmitter<any> = new EventEmitter();

    @Output()
    custom: EventEmitter<any> = new EventEmitter();
    url$: Observable<string>;
    header$: Observable<string>;

    constructor(private dotContentletEditorService: DotContentletEditorService) {}

    ngOnInit() {
        this.url$ = this.dotContentletEditorService.addUrl$;
        this.header$ = this.dotContentletEditorService.header$;
    }
}
