import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe, DotWorkflowComponent } from '@dotcms/ui';

import { DotCardinalitySelectorComponent } from '../dot-cardinality-selector/dot-cardinality-selector.component';
import { DotRelationshipsPropertyValue } from '../model/dot-relationships-property-value.model';

@Component({
    selector: 'dot-new-relationships',
    templateUrl: './dot-new-relationships.component.html',
    styleUrls: ['./dot-new-relationships.component.scss'],
    imports: [
        DotWorkflowComponent,
        DotCardinalitySelectorComponent,
        FormsModule,
        DotMessagePipe,
        DotFieldRequiredDirective
    ]
})
export class DotNewRelationshipsComponent implements OnChanges {
    private contentTypeService = inject(DotContentTypeService);

    @Input() cardinality: number;

    @Input() velocityVar: string;

    @Input() editing: boolean;

    @Output() switch: EventEmitter<DotRelationshipsPropertyValue> = new EventEmitter();

    contentType: DotCMSContentType;
    currentCardinalityIndex: number;

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.velocityVar) {
            this.loadContentType(changes.velocityVar.currentValue);
        }

        if (changes.cardinality) {
            this.currentCardinalityIndex = changes.cardinality.currentValue;
        }
    }

    /**
     * Handle content type change from dot-workflow component
     *
     * @param contentType The selected content type
     * @memberof DotNewRelationshipsComponent
     */
    onContentTypeChange(contentType: DotCMSContentType | null): void {
        this.contentType = contentType;
        this.triggerChanged();
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
