import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
    signal
} from '@angular/core';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { IMAGE_EDITOR_LAUNCHER, LegacyDojoImageEditorLauncher } from '../../services/image-editor';
import { DotFileFieldComponent } from '../dot-file-field/dot-file-field.component';

/**
 * Custom-element bridge for the legacy `dotcms-binary-field` web component.
 *
 * Exposes the exact imperative contract the legacy JSP editor depends on
 * (`field`, `contentlet`, `imageEditor` set as DOM properties and a
 * `valueUpdated` CustomEvent) using classic `@Input()`/`@Output()` decorators.
 *
 * Signal `input()` does not hydrate from imperative DOM property assignment when
 * a component is registered with `@angular/elements` (spike #36055), so this thin
 * bridge keeps the classic API at the boundary while the unified
 * {@link DotFileFieldComponent} stays signal-based internally.
 *
 * The legacy Dojo image editor is wired through {@link LegacyDojoImageEditorLauncher}.
 */
@Component({
    selector: 'dot-binary-field-ce-bridge',
    template: `
        <dot-file-field
            [field]="$field()"
            [contentlet]="$contentlet()"
            [hasError]="false"
            (valueUpdated)="valueUpdated.emit($event)" />
    `,
    imports: [DotFileFieldComponent],
    providers: [{ provide: IMAGE_EDITOR_LAUNCHER, useClass: LegacyDojoImageEditorLauncher }],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldCeBridgeComponent {
    /** Content type field definition (set imperatively by the legacy JSP). */
    @Input({ required: true })
    set field(value: DotCMSContentTypeField) {
        this.$field.set(value);
    }

    /** Current contentlet (set imperatively by the legacy JSP). */
    @Input({ required: true })
    set contentlet(value: DotCMSContentlet) {
        this.$contentlet.set(value);
    }

    /** Whether the image editor is enabled (preserved for contract parity). */
    @Input()
    set imageEditor(value: boolean) {
        this.$imageEditor.set(value);
    }

    /** Emitted as a DOM `valueUpdated` CustomEvent for the legacy editor. */
    @Output() valueUpdated = new EventEmitter<{ value: string; fileName: string }>();

    protected readonly $field = signal<DotCMSContentTypeField>({} as DotCMSContentTypeField);
    protected readonly $contentlet = signal<DotCMSContentlet>({} as DotCMSContentlet);
    protected readonly $imageEditor = signal<boolean>(false);
}
