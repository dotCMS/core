import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    HostListener,
    Input,
    OnInit,
    Output,
    ViewChild,
    inject
} from '@angular/core';

import { OverlayPanel } from 'primeng/overlaypanel';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { FieldService } from '../service';

/**
 * This display field after being dropped into a Content Type Drop zone
 * @export
 * @class ContentTypesFieldDragabbleItemComponent
 */
@Component({
    selector: 'dot-content-type-field-dragabble-item',
    styleUrls: ['./content-type-field-dragabble-item.component.scss'],
    templateUrl: './content-type-field-dragabble-item.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ContentTypesFieldDragabbleItemComponent implements OnInit {
    private dotMessageService = inject(DotMessageService);
    fieldService = inject(FieldService);

    @Input()
    isSmall = false;
    @Input()
    field: DotCMSContentTypeField;
    @Output()
    remove: EventEmitter<DotCMSContentTypeField> = new EventEmitter();
    @Output()
    edit: EventEmitter<DotCMSContentTypeField> = new EventEmitter();

    @ViewChild('op') overlayPanel: OverlayPanel;

    open = false;

    fieldAttributesArray: string[];

    fieldTypeLabel: string;
    fieldAttributesString: string;
    icon: string;

    ngOnInit(): void {
        this.fieldTypeLabel = this.field.fieldTypeLabel ? this.field.fieldTypeLabel : null;

        this.fieldAttributesArray = [
            {
                name: this.dotMessageService.get('contenttypes.field.atributes.required'),
                value: this.field.required
            },
            {
                name: this.dotMessageService.get('contenttypes.field.atributes.indexed'),
                value: this.field.indexed
            },
            {
                name: this.dotMessageService.get('contenttypes.field.atributes.listed'),
                value: this.field.listed
            }
        ]
            .filter((field) => field.value)
            .map((field) => field.name);

        this.fieldAttributesString = this.fieldAttributesArray.join(', ');

        this.icon = this.fieldService.getIcon(this.field.clazz);
    }

    /**
     *To reassign the open variable
     *
     * @param {boolean} state
     * @memberof ContentTypesFieldDragabbleItemComponent
     */
    setOpen(state: boolean) {
        this.open = state;
    }

    /**
     * This method opens the edit modal when the user clicks on the field
     * @param {MouseEvent} $event
     * @memberof ContentTypesFieldDragabbleItemComponent
     */
    @HostListener('click', ['$event'])
    onClick($event: MouseEvent) {
        $event.stopPropagation();
        this.edit.emit(this.field);
        this.overlayPanel.hide();
    }

    /**
     * This emthod is used to close the attributes list when the user drags the field
     * @memberof ContentTypesFieldDragabbleItemComponent
     */
    @HostListener('mousedown')
    onMouseDown() {
        this.overlayPanel.hide();
    }

    /**
     * This method is used to close the attributes list when the user clicks outside the field
     * @param {MouseEvent} $event
     * @memberof ContentTypesFieldDragabbleItemComponent
     */
    @HostListener('window:click', ['$event'])
    onWindowClick($event: MouseEvent) {
        $event.stopPropagation();
        this.overlayPanel.hide();
    }

    /**
     * This method is used to open the attributes list when the user clicks on the open button
     * @param {MouseEvent} $event
     * @memberof ContentTypesFieldDragabbleItemComponent
     */
    openAttr($event: MouseEvent) {
        $event.stopPropagation();
        this.overlayPanel.show($event, $event.target);

        setTimeout(() => {
            this.overlayPanel.hide();
        }, 2000);
    }

    /**
     * This method is used to remove the field from the Content Type
     * @param {MouseEvent} $event
     * @memberof ContentTypesFieldDragabbleItemComponent
     */
    removeItem($event: MouseEvent): void {
        $event.stopPropagation();
        this.remove.emit(this.field);
    }
}
