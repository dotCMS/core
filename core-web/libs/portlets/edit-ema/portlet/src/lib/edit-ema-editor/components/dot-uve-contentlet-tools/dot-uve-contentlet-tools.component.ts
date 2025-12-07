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

import { ActionPayload, ContentletPayload, VTLFile } from '../../../shared/models';
import { ContentletArea } from '../ema-page-dropzone/types';

// TODO: Add the CSS for this
// const MIN_WIDTH_FOR_CENTERED_BUTTON = 250;

@Component({
    selector: 'dot-uve-contentlet-tools',
    imports: [NgStyle, ButtonModule, MenuModule, JsonPipe, TooltipModule, DotMessagePipe],
    templateUrl: './dot-uve-contentlet-tools.component.html',
    styleUrls: ['./dot-uve-contentlet-tools.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[class.hide]': 'hide()'
    }
})
export class DotUveContentletToolsComponent {
    readonly contentletArea = input.required<ContentletArea>({ alias: 'contentletArea' });
    readonly isEnterprise = input<boolean>(false, { alias: 'isEnterprise' });
    readonly allowContentDelete = input<boolean>(true, { alias: 'allowContentDelete' });
    readonly hide = input<boolean>(false, { alias: 'hide' });
    readonly activeContentlet = input<ContentletPayload | null>(null, {
        alias: 'activeContentlet'
    });

    // @Output() actions = new
    @Output() editVTL = new EventEmitter<VTLFile>();
    @Output() editContent = new EventEmitter<ActionPayload>();
    @Output() deleteContent = new EventEmitter<ActionPayload>();
    @Output() addContent = new EventEmitter<{
        type: 'content' | 'form' | 'widget';
        payload: ActionPayload;
    }>();
    @Output() selectContent = new EventEmitter<ContentletPayload>();

    @ViewChild('menu') menu?: Menu;
    @ViewChild('menuVTL') menuVTL?: Menu;
    @ViewChild('dragImage') dragImage?: ElementRef<HTMLDivElement>;

    readonly #dotMessageService = inject(DotMessageService);

    readonly contentContext = computed<ActionPayload>(() => ({
        ...this.contentletArea()?.payload,
        position: this.buttonPosition()
    }));
    readonly hasVtlFiles = computed(() => !!this.contentContext()?.vtlFiles?.length);
    readonly isActive = computed(
        () => this.contentContext()?.contentlet?.identifier === this.activeContentlet()?.identifier
    );
    readonly isContainerEmpty = computed(
        () => this.contentContext()?.contentlet?.identifier === 'TEMP_EMPTY_CONTENTLET'
    );

    protected readonly deleteButtonTooltip = computed(() => {
        if (!this.allowContentDelete()) {
            return 'uve.disable.delete.button.on.personalization';
        }

        return null;
    });

    protected readonly menuItems = computed<MenuItem[]>(() => {
        const items = [
            {
                label: this.#dotMessageService.get('content'),
                command: () =>
                    this.addContent.emit({ type: 'content', payload: this.contentContext() })
            },
            {
                label: this.#dotMessageService.get('Widget'),
                command: () =>
                    this.addContent.emit({ type: 'widget', payload: this.contentContext() })
            }
        ];

        if (this.isEnterprise()) {
            items.push({
                label: this.#dotMessageService.get('form'),
                command: () =>
                    this.addContent.emit({ type: 'form', payload: this.contentContext() })
            });
        }

        return items;
    });

    protected readonly vtlMenuItems = computed<MenuItem[]>(() => {
        const payload = this.contentContext();
        return payload?.vtlFiles?.map((file) => ({
            label: file?.name,
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
        const { container, contentlet } = this.contentContext() ?? {};

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
}
