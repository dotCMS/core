import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { ActiveTool } from '../../models/image-editor.models';
import { imageEditorToolEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

/** A selectable tool on the floating canvas rail. */
interface ToolRailItem {
    /** The tool identifier dispatched on selection. */
    id: ActiveTool;
    /** PrimeNG icon class for the button. */
    icon: string;
    /** i18n key for the aria-label and tooltip. */
    label: string;
    /** `data-testid` value for the button. */
    testId: string;
}

/**
 * Floating vertical rail of canvas tools (move, crop, focal point). Acts as a
 * `toolbar` with roving tabindex: the active tool is the only focusable button,
 * matching the WAI-ARIA toolbar pattern. Selecting a tool dispatches
 * {@link imageEditorToolEvents.toolSelected} so the store owns the active tool.
 */
@Component({
    selector: 'dot-image-editor-tool-rail',
    templateUrl: './dot-image-editor-tool-rail.component.html',
    styleUrl: './dot-image-editor-tool-rail.component.scss',
    imports: [ButtonModule, TooltipModule, DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        role: 'toolbar',
        'aria-orientation': 'vertical'
    }
})
export class DotImageEditorToolRailComponent {
    protected readonly store = inject(ImageEditorStore);
    readonly #dispatch = injectDispatch(imageEditorToolEvents);

    /** The ordered tools rendered on the rail. */
    protected readonly tools: ToolRailItem[] = [
        {
            id: 'move',
            icon: 'pi pi-arrows-alt',
            label: 'edit.content.image-editor.tool.move',
            testId: 'image-editor-tool-move'
        },
        {
            id: 'crop',
            icon: 'pi pi-crop',
            label: 'edit.content.image-editor.tool.crop',
            testId: 'image-editor-tool-crop'
        },
        {
            id: 'focal',
            icon: 'pi pi-circle',
            label: 'edit.content.image-editor.tool.focal',
            testId: 'image-editor-tool-focal'
        }
    ];

    /** Selects a tool, delegating the state change to the store. */
    protected selectTool(id: ActiveTool): void {
        this.#dispatch.toolSelected(id);
    }
}
