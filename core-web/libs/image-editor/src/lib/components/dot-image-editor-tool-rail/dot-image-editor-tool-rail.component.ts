import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { ActiveTool, ToolRailItem } from '../../models/image-editor.models';
import { imageEditorToolEvents } from '../../store/image-editor.events';
import { ImageEditorStore } from '../../store/image-editor.store';

/**
 * Floating vertical rail of canvas tools (move, crop). Acts as a `toolbar` with
 * roving tabindex: the active tool is the only focusable button, matching the
 * WAI-ARIA toolbar pattern. Selecting a tool dispatches
 * {@link imageEditorToolEvents.toolSelected} so the store owns the active tool.
 */
@Component({
    selector: 'dot-image-editor-tool-rail',
    templateUrl: './dot-image-editor-tool-rail.component.html',
    styleUrl: './dot-image-editor-tool-rail.component.scss',
    imports: [TooltipModule, DotMessagePipe],
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
            label: 'edit.content.image-editor.tool.move',
            testId: 'image-editor-tool-move'
        },
        {
            id: 'crop',
            label: 'edit.content.image-editor.tool.crop',
            testId: 'image-editor-tool-crop'
        }
    ];

    /** Selects a tool, delegating the state change to the store. */
    protected selectTool(id: ActiveTool): void {
        this.#dispatch.toolSelected(id);
    }
}
