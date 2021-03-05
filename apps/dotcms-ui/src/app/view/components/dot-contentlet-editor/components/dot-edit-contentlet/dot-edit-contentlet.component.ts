import { Component, OnInit, EventEmitter, Output, Input } from '@angular/core';

import { Observable } from 'rxjs';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';

/**
 * Allow user to edit a contentlet to DotCMS instance
 *
 * @export
 * @class DotEditContentletComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-edit-contentlet',
    templateUrl: './dot-edit-contentlet.component.html',
    styleUrls: ['./dot-edit-contentlet.component.scss']
})
export class DotEditContentletComponent implements OnInit {
    @Input()
    inode: string;
    @Output()
    close: EventEmitter<any> = new EventEmitter();
    @Output()
    custom: EventEmitter<any> = new EventEmitter();

    url$: Observable<string>;

    constructor(private dotContentletEditorService: DotContentletEditorService) {}

    ngOnInit() {
        this.url$ = this.dotContentletEditorService.editUrl$;
    }
}
