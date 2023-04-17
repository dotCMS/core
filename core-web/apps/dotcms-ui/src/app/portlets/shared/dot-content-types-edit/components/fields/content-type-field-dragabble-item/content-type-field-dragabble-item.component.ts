import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    HostListener,
    Input,
    OnDestroy,
    OnInit,
    Output
} from '@angular/core';

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
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ContentTypesFieldDragabbleItemComponent implements OnInit, OnDestroy {
    @Input()
    field: DotCMSContentTypeField;
    @Output()
    remove: EventEmitter<DotCMSContentTypeField> = new EventEmitter();
    @Output()
    edit: EventEmitter<DotCMSContentTypeField> = new EventEmitter();

    shouldResize = false;
    small = false;
    open = false;
    fieldAttributes: string[];
    icon: string;

    private resizeObserver$: ResizeObserver;

    constructor(
        private dotMessageService: DotMessageService,
        public fieldService: FieldService,
        private el: ElementRef,
        private cd: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.fieldAttributes = [
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

        this.icon = this.fieldService.getIcon(this.field.clazz);

        this.shouldResize = this.fieldAttributes.length > 1;

        if (this.shouldResize) {
            this.resizeObserver$ = new ResizeObserver((entries) => {
                const [host] = entries;

                if (host.contentRect.width < 300 && !this.small) {
                    this.small = true;

                    this.cd.detectChanges();
                } else if (host.contentRect.width > 300 && this.small) {
                    this.small = false;

                    this.cd.detectChanges();
                }
            });

            this.resizeObserver$.observe(this.el.nativeElement);
        }
    }

    ngOnDestroy(): void {
        if (this.shouldResize) this.resizeObserver$.disconnect();
    }

    @HostListener('click', ['$event'])
    onClick($event: MouseEvent) {
        $event.stopPropagation();
        this.edit.emit(this.field);
        this.open = false;
    }

    // To close the attrs on drag
    @HostListener('mousedown')
    onMouseDown() {
        this.open = false;
    }

    @HostListener('window:click', ['$event'])
    onWindowClick($event: MouseEvent) {
        $event.stopPropagation();
        this.open = false;
    }

    // Opens the attributes list when the component is small
    openAttr($event: MouseEvent) {
        $event.stopPropagation();
        this.open = true;
    }

    // Closes the attributes list when the component is small
    closeAttr($event: MouseEvent) {
        $event.stopPropagation();
        this.open = false;
    }

    removeItem($event: MouseEvent): void {
        $event.stopPropagation();
        this.remove.emit(this.field);
    }
}
