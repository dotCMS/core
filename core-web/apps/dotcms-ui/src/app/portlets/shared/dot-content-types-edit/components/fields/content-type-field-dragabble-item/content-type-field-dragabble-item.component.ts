import {
    ChangeDetectionStrategy,
    Component,
    HostListener,
    OnInit,
    inject,
    input,
    output,
    viewChild
} from '@angular/core';

import { Popover } from 'primeng/popover';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { camelCase } from '@dotcms/utils';

import { FieldService } from '../service';

/**
 * This display field after being dropped into a Content Type Drop zone
 * @export
 * @class ContentTypesFieldDragabbleItemComponent
 */
@Component({
    selector: 'dot-content-type-field-dragabble-item',
    templateUrl: './content-type-field-dragabble-item.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
    host: {
        class: 'bg-white hover:shadow-md cursor-move flex flex-row items-center gap-3  min-h-18 transition-shadow duration-200 rounded-md border border-gray-400 mb-1 w-full box-border relative hover:z-10 group'
    }
})
export class ContentTypesFieldDragabbleItemComponent implements OnInit {
    private dotMessageService = inject(DotMessageService);
    fieldService = inject(FieldService);

    readonly $isSmall = input<boolean>(false, { alias: 'isSmall' });
    readonly $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    readonly remove = output<DotCMSContentTypeField>();
    readonly edit = output<DotCMSContentTypeField>();

    readonly $overlayPanel = viewChild.required<Popover>('op');

    /** Local copy of field for access */
    field: DotCMSContentTypeField;

    isDragging = false;
    open = false;

    fieldAttributesArray: string[];

    fieldTypeLabel: string;
    fieldAttributesString: string;
    icon: string;

    get variableToShow(): string {
        const field = this.$field();
        return field?.variable || camelCase(field?.name || '');
    }

    ngOnInit(): void {
        this.field = this.$field();
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
        this.$overlayPanel().hide();
    }

    /**
     * This emthod is used to close the attributes list when the user drags the field
     * @memberof ContentTypesFieldDragabbleItemComponent
     */
    @HostListener('mousedown')
    onMouseDown() {
        this.$overlayPanel().hide();
    }

    /**
     * This method is used to close the attributes list when the user clicks outside the field
     * @param {MouseEvent} $event
     * @memberof ContentTypesFieldDragabbleItemComponent
     */
    @HostListener('window:click', ['$event'])
    onWindowClick($event: MouseEvent) {
        $event.stopPropagation();
        this.$overlayPanel().hide();
    }

    /**
     * This method is used to open the attributes list when the user clicks on the open button
     * @param {MouseEvent} $event
     * @memberof ContentTypesFieldDragabbleItemComponent
     */
    openAttr($event: MouseEvent) {
        $event.stopPropagation();
        this.$overlayPanel().show($event, $event.target);

        setTimeout(() => {
            this.$overlayPanel().hide();
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
