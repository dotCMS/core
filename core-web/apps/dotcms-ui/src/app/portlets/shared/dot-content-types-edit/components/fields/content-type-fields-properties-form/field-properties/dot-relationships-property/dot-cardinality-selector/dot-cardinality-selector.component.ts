import { Observable } from 'rxjs';

import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';

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
    providers: [],
    selector: 'dot-cardinality-selector',
    templateUrl: './dot-cardinality-selector.component.html',
    styleUrls: ['./dot-cardinality-selector.component.scss'],
    standalone: false
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
