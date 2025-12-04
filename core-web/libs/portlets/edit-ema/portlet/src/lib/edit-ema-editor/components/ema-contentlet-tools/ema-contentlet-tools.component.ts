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

const BUTTON_WIDTH = 40;
const BUTTON_HEIGHT = 40;
const ACTIONS_CONTAINER_HEIGHT = 40;

const ACTIONS_CONTAINER_WIDTH_WITH_VTL = 178;
const INITIAL_ACTIONS_CONTAINER_WIDTH = 128;

@Component({
    selector: 'dot-ema-contentlet-tools',
    imports: [NgStyle, ButtonModule, MenuModule, JsonPipe, TooltipModule, DotMessagePipe],
    templateUrl: './ema-contentlet-tools.component.html',
    styleUrls: ['./ema-contentlet-tools.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmaContentletToolsComponent {
    @ViewChild('menu') menu: Menu;
    @ViewChild('menuVTL') menuVTL: Menu;
    @ViewChild('dragImage') dragImage: ElementRef;

    readonly $contentletArea = input.required<ContentletArea>({ alias: 'contentletArea' });
    readonly $isEnterprise = input<boolean>(false, { alias: 'isEnterprise' });
    readonly $allowContentDelete = input<boolean>(true, { alias: 'allowContentDelete' });
    readonly $contentletPayload = computed(() => this.$contentletArea()?.payload);
    readonly $hasContainer = computed(() => !!this.$contentletPayload()?.container);
    readonly $hasVtlFiles = computed(() => !!this.$contentletPayload()?.vtlFiles?.length);
    readonly $dragPayload = computed(() => {
        const payload = this.$contentletPayload();

        if (!payload?.container || !payload?.contentlet) {
            return null;
        }

        return {
            container: payload.container,
            contentlet: payload.contentlet,
            move: true
        };
    });
    readonly $isContainerEmpty = computed(
        () => this.$contentletPayload()?.contentlet.identifier === 'TEMP_EMPTY_CONTENTLET'
    );

    @Output() addContent = new EventEmitter<ActionPayload>();
    @Output() addForm = new EventEmitter<ActionPayload>();
    @Output() addWidget = new EventEmitter<ActionPayload>();
    @Output() edit = new EventEmitter<ActionPayload>();
    @Output() editVTL = new EventEmitter<VTLFile>();
    @Output() delete = new EventEmitter<ActionPayload>();

    protected readonly deleteButtonTooltip = computed(() => {
        return this.$allowContentDelete() ? null : 'uve.disable.delete.button.on.personalization';
    });

    #dotMessageService = inject(DotMessageService);
    ACTIONS_CONTAINER_WIDTH = INITIAL_ACTIONS_CONTAINER_WIDTH; // Now is dynamic based on the page type (Headless - VTL)
    vtlFiles: MenuItem[] = [];
    #buttonPosition: 'after' | 'before' = 'after';

    readonly #comunityItems: MenuItem[] = [
        {
            label: this.#dotMessageService.get('content'),
            command: () => {
                this.addContent.emit({
                    ...this.$contentletPayload(),
                    position: this.#buttonPosition
                });
            }
        },
        {
            label: this.#dotMessageService.get('Widget'),
            command: () => {
                this.addWidget.emit({
                    ...this.$contentletPayload(),
                    position: this.#buttonPosition
                });
            }
        }
    ];
    readonly #enterpriseItems: MenuItem[] = [
        {
            label: this.#dotMessageService.get('form'),
            command: () => {
                this.addForm.emit({
                    ...this.$contentletPayload(),
                    position: this.#buttonPosition
                });
            }
        }
    ];

    readonly items = signal<MenuItem[]>(this.#comunityItems);

    protected styles: Record<string, { [klass: string]: unknown }> = {};
    #enterpriseMenuExtended = false;

    constructor() {
        effect(() => {
            const contentletArea = this.$contentletArea();
            const payload = this.$contentletPayload();
            const isEnterprise = this.$isEnterprise();

            if (!contentletArea || !payload) {
                return;
            }

            if (isEnterprise && !this.#enterpriseMenuExtended) {
                this.items.update((items) => [...items, ...this.#enterpriseItems]);
                this.#enterpriseMenuExtended = true;
            }

            this.setVtlFiles(payload);

            this.ACTIONS_CONTAINER_WIDTH = payload.vtlFiles
                ? ACTIONS_CONTAINER_WIDTH_WITH_VTL
                : INITIAL_ACTIONS_CONTAINER_WIDTH;

            this.styles = {
                bounds: this.getBoundsPosition(contentletArea),
                topButton: this.getTopButtonPosition(contentletArea),
                bottomButton: this.getBottomButtonPosition(contentletArea),
                actions: this.getActionPosition(contentletArea)
            };
        });
    }

    /**
     * Sets the VTL files for the component.
     *
     * @memberof EmaContentletToolsComponent
     */
    setVtlFiles(contentletArea: ContentletArea['payload'] = this.$contentletPayload()) {
        this.vtlFiles = contentletArea?.vtlFiles?.map((file) => ({
            label: file.name,
            command: () => {
                this.editVTL.emit(file);
            }
        }));
    }

    dragStart(event: DragEvent): void {
        event.dataTransfer.setDragImage(this.dragImage.nativeElement, 0, 0);
    }

    /**
     * Set the position flag to add the contentlet before or after the current one
     *
     * @param {('before' | 'after')} position
     * @memberof EmaContentletToolsComponent
     */
    setPositionFlag(position: 'before' | 'after'): void {
        this.#buttonPosition = position;
    }

    /**
     * Set the position for the bounds div
     *
     * @return {*}  {Record<string, string>}
     * @memberof EmaContentletToolsComponent
     */
    private getBoundsPosition(
        contentletArea: ContentletArea = this.$contentletArea()
    ): Record<string, string> {
        return {
            left: `${contentletArea.x}px`,
            top: `${contentletArea.y}px`,
            width: `${contentletArea.width}px`,
            height: `${contentletArea.height}px`
        };
    }

    /**
     * Set the position for the top add button
     *
     * @return {*}  {Record<string, string>}
     * @memberof EmaContentletToolsComponent
     */
    private getTopButtonPosition(
        contentletArea: ContentletArea = this.$contentletArea()
    ): Record<string, string> {
        const contentletCenterX = contentletArea.x + contentletArea.width / 2;
        const buttonLeft = contentletCenterX - BUTTON_WIDTH / 2;
        const buttonTop = contentletArea.y - BUTTON_HEIGHT / 2;

        return {
            position: 'absolute',
            left: contentletArea.width < 250 ? `${contentletArea.x + 8}px` : `${buttonLeft}px`,
            top: `${buttonTop}px`,
            zIndex: '1'
        };
    }

    /**
     * Set the position for the bottom add button
     *
     * @return {*}  {Record<string, string>}
     * @memberof EmaContentletToolsComponent
     */
    private getBottomButtonPosition(
        contentletArea: ContentletArea = this.$contentletArea()
    ): Record<string, string> {
        const contentletCenterX = contentletArea.x + contentletArea.width / 2;
        const buttonLeft = contentletCenterX - BUTTON_WIDTH / 2;
        const buttonTop = contentletArea.y + contentletArea.height - BUTTON_HEIGHT / 2;

        return {
            position: 'absolute',
            top: `${buttonTop}px`,
            left: `${buttonLeft}px`,
            zIndex: '1'
        };
    }

    /**
     * Set the position for the action buttons
     *
     * @return {*}  {Record<string, string>}
     * @memberof EmaContentletToolsComponent
     */
    private getActionPosition(
        contentletArea: ContentletArea = this.$contentletArea()
    ): Record<string, string> {
        const contentletCenterX = contentletArea.x + contentletArea.width;
        const left = contentletCenterX - this.ACTIONS_CONTAINER_WIDTH - 8;
        const top = contentletArea.y - ACTIONS_CONTAINER_HEIGHT / 2;

        return {
            position: 'absolute',
            left: `${left}px`,
            top: `${top}px`,
            zIndex: '1',
            width: `${this.ACTIONS_CONTAINER_WIDTH}px`
        };
    }

    /**
     * Hide all context menus when the contentlet changes
     *
     * @memberof EmaContentletToolsComponent
     */
    hideMenus() {
        this.menu?.hide();
        this.menuVTL?.hide();
    }
}
