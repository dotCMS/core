import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotRelationshipCardinality } from '../model/dot-relationship-cardinality.model';
import { DotRelationshipService } from '../services/dot-relationship.service';

/**
 *Selector for relationships cardinalities
 *
 * @export
 * @class DotCardinalitySelectorComponent
 * @implements {OnInit}
 * @implements {OnChanges}
 */
@Component({
    selector: 'dot-cardinality-selector',
    templateUrl: './dot-cardinality-selector.component.html',
    styleUrls: ['./dot-cardinality-selector.component.scss'],
    imports: [DropdownModule, FormsModule, AsyncPipe]
})
export class DotCardinalitySelectorComponent implements OnInit {
    private dotRelationshipService = inject(DotRelationshipService);

    @Input()
    value: number;

    @Input()
    disabled: boolean;

    @Output()
    switch: EventEmitter<number> = new EventEmitter();

    options: Observable<DotRelationshipCardinality[]>;

    ngOnInit() {
        this.options = this.dotRelationshipService.loadCardinalities();
    }
}
