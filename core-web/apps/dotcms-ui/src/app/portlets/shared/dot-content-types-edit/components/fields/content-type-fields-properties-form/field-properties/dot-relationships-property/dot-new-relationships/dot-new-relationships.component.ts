import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectModule } from 'primeng/select';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

import { DotCardinalitySelectorComponent } from '../dot-cardinality-selector/dot-cardinality-selector.component';
import { DotRelationshipsPropertyValue } from '../model/dot-relationships-property-value.model';

@Component({
    selector: 'dot-new-relationships',
    templateUrl: './dot-new-relationships.component.html',
    styleUrls: ['./dot-new-relationships.component.scss'],
    imports: [
        SelectModule,
        DotCardinalitySelectorComponent,
        FormsModule,
        DotMessagePipe,
        DotFieldRequiredDirective
    ]
})
export class DotNewRelationshipsComponent implements OnInit, OnChanges {
    private contentTypeService = inject(DotContentTypeService);

    @Input() cardinality: number;

    @Input() velocityVar: string;

    @Input() editing: boolean;

    @Output() switch: EventEmitter<DotRelationshipsPropertyValue> = new EventEmitter();

    contentTypes: DotCMSContentType[] = [];
    contentType: DotCMSContentType;
    currentCardinalityIndex: number;
    loading = false;

    ngOnInit(): void {
        if (!this.editing) {
            this.loadContentTypes();
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.velocityVar) {
            this.loadContentType(changes.velocityVar.currentValue);
        }

        if (changes.cardinality) {
            this.currentCardinalityIndex = changes.cardinality.currentValue;
        }
    }

    /**
     * Trigger a change event, it send a object with the current content type's variable and
     * the current candinality's index.
     *
     * @memberof DotNewRelationshipsComponent
     */
    triggerChanged(): void {
        this.switch.next({
            velocityVar:
                this.velocityVar || (this.contentType ? this.contentType.variable : undefined),
            cardinality: this.currentCardinalityIndex
        });
    }

    /**
     *Call when the selected cardinality changed
     *
     * @param {number} cardinalityIndex selected cardinality index
     * @memberof DotNewRelationshipsComponent
     */
    cardinalityChanged(cardinalityIndex: number): void {
        this.currentCardinalityIndex = cardinalityIndex;
        this.triggerChanged();
    }

    /**
     * Load all content types
     *
     * @private
     * @memberof DotNewRelationshipsComponent
     */
    private loadContentTypes(): void {
        if (this.loading) {
            return;
        }

        this.loading = true;
        this.contentTypeService
            .getContentTypes({
                page: 100 // Request a large page size to get all content types
            })
            .subscribe({
                next: (contentTypes) => {
                    this.contentTypes = contentTypes;
                    this.loading = false;
                },
                error: () => {
                    this.loading = false;
                }
            });
    }

    private loadContentType(velocityVar: string) {
        if (velocityVar) {
            if (velocityVar.includes('.')) {
                velocityVar = velocityVar.split('.')[0];
            }

            this.contentTypeService.getContentType(velocityVar).subscribe((contentType) => {
                this.contentType = contentType;
            });
        } else {
            this.contentType = null;
        }
    }
}
