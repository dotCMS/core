import { Observable } from 'rxjs';

import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';

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
    styleUrls: ['./dot-edit-contentlet.component.scss'],
    standalone: false
})
export class DotEditContentletComponent implements OnInit {
    private dotContentletEditorService = inject(DotContentletEditorService);

    @Input()
    inode: string;
    @Output()
    shutdown: EventEmitter<unknown> = new EventEmitter();
    @Output()
    custom: EventEmitter<unknown> = new EventEmitter();

    url$: Observable<string>;

    ngOnInit() {
        this.url$ = this.dotContentletEditorService.editUrl$;
    }
}
