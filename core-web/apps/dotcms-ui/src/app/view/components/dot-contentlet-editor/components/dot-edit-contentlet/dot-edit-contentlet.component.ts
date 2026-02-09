import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotContentletWrapperComponent } from '../dot-contentlet-wrapper/dot-contentlet-wrapper.component';

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
    imports: [CommonModule, DotContentletWrapperComponent]
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
