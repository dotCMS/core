import { Observable } from 'rxjs';

import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';

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
    styleUrls: ['./dot-add-contentlet.component.scss'],
    standalone: false
})
export class DotAddContentletComponent implements OnInit {
    private dotContentletEditorService = inject(DotContentletEditorService);

    @Output()
    shutdown: EventEmitter<unknown> = new EventEmitter();

    @Output()
    custom: EventEmitter<unknown> = new EventEmitter();
    url$: Observable<string>;
    header$: Observable<string>;

    ngOnInit() {
        this.url$ = this.dotContentletEditorService.addUrl$;
        this.header$ = this.dotContentletEditorService.header$;
    }
}
