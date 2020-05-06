import { Component, OnInit, Output, EventEmitter } from '@angular/core';

import { Observable } from 'rxjs';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';

/**
 * Allow user to add a contentlet to DotCMS instance
 *
 * @export
 * @class DotCreateContentletComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-create-contentlet',
    templateUrl: './dot-create-contentlet.component.html',
    styleUrls: ['./dot-create-contentlet.component.scss']
})
export class DotCreateContentletComponent implements OnInit {
    @Output() close: EventEmitter<any> = new EventEmitter();
    url$: Observable<string>;
    @Output()
    custom: EventEmitter<any> = new EventEmitter();

    constructor(private dotContentletEditorService: DotContentletEditorService) {}

    ngOnInit() {
        this.url$ = this.dotContentletEditorService.createUrl$;
    }
}
