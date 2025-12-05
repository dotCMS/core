import { JsonPipe, NgStyle } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Output,
    ViewChild,
    computed,
    effect,
    inject,
    input,
    signal
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { ActionPayload, VTLFile } from '../../../shared/models';
import { ContentletArea } from '../ema-page-dropzone/types';

// TODO: Add the CSS for this
// const MIN_WIDTH_FOR_CENTERED_BUTTON = 250;

@Component({
    selector: 'dot-ema-contentlet-tools',
    imports: [NgStyle, ButtonModule, MenuModule, JsonPipe, TooltipModule, DotMessagePipe],
    templateUrl: './ema-contentlet-tools.component.html',
    styleUrls: ['./ema-contentlet-tools.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[class.hide]': 'hide()'
    }
})
export class EmaContentletToolsComponent {
    readonly contentletArea = input.required<ContentletArea>({ alias: 'contentletArea' });
    readonly isEnterprise = input<boolean>(false, { alias: 'isEnterprise' });
    readonly allowContentDelete = input<boolean>(true, { alias: 'allowContentDelete' });
    readonly hide = input<boolean>(false, { alias: 'hide' });

    @Output() addContent = new EventEmitter<ActionPayload>();
    @Output() addForm = new EventEmitter<ActionPayload>();
    @Output() addWidget = new EventEmitter<ActionPayload>();
    @Output() edit = new EventEmitter<ActionPayload>();
    @Output() editVTL = new EventEmitter<VTLFile>();
    @Output() delete = new EventEmitter<ActionPayload>();

    @ViewChild('menu') menu?: Menu;
    @ViewChild('menuVTL') menuVTL?: Menu;
    @ViewChild('dragImage') dragImage?: ElementRef<HTMLDivElement>;

    readonly #dotMessageService = inject(DotMessageService);

    readonly contentletPayload = computed(() => this.contentletArea()?.payload);
    readonly hasContainer = computed(() => !!this.contentletPayload()?.container);
    readonly hasVtlFiles = computed(() => !!this.contentletPayload()?.vtlFiles?.length);
    readonly isContainerEmpty = computed(
        () => this.contentletPayload()?.contentlet.identifier === 'TEMP_EMPTY_CONTENTLET'
    );

    protected readonly deleteButtonTooltip = computed(() => {
        return this.allowContentDelete()
            ? null
            : this.#dotMessageService.get('uve.disable.delete.button.on.personalization');
    });

    protected readonly menuItems = computed<MenuItem[]>(() => {
        const items = [
            {
                label: this.#dotMessageService.get('content'),
                command: () => this.emitAddAction(this.addContent)
            },
            {
                label: this.#dotMessageService.get('Widget'),
                command: () => this.emitAddAction(this.addWidget)
            }
        ];

        if (this.isEnterprise()) {
            items.push({
                label: this.#dotMessageService.get('form'),
                command: () => this.emitAddAction(this.addForm)
            });
        }

        return items;
    });

    protected readonly vtlMenuItems = computed<MenuItem[]>(() => {
        const payload = this.contentletPayload();
        return payload.vtlFiles.map((file) => ({
            label: file.name,
            command: () => this.editVTL.emit(file)
        }));
    });

    protected readonly buttonPosition = signal<'after' | 'before'>('after');

    protected readonly boundsStyles = computed(() => {
        const contentletArea = this.contentletArea();
        return {
            left: `${contentletArea?.x ?? 0}px`,
            top: `${contentletArea?.y ?? 0}px`,
            width: `${contentletArea?.width ?? 0}px`,
            height: `${contentletArea?.height ?? 0}px`
        };
    });

    readonly dragPayload = computed(() => {
        const { container, contentlet } = this.contentletPayload() ?? {};

        if (!container || !contentlet) {
            return null;
        }

        return {
            container,
            contentlet,
            move: true
        };
    });

    constructor() {
        effect(() => {
            // If this changes, we need to hide the menus
            this.contentletArea();
            this.hideMenus();
        });
    }

    protected dragStart(event: DragEvent): void {
        if (!this.dragImage?.nativeElement || !event.dataTransfer) {
            return;
        }

        event.dataTransfer.setDragImage(this.dragImage.nativeElement, 0, 0);
    }

    protected setPositionFlag(position: 'before' | 'after'): void {
        this.buttonPosition.set(position);
    }

    protected hideMenus(): void {
        this.menu?.hide();
        this.menuVTL?.hide();
    }

    private emitAddAction(emitter: EventEmitter<ActionPayload>): void {
        const payload = this.contentletPayload();

        if (!payload) {
            return;
        }

        emitter.emit({
            ...payload,
            position: this.buttonPosition()
        });
    }
}
