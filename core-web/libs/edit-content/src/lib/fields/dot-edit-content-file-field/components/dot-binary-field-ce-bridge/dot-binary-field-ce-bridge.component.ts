import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
    signal
} from '@angular/core';

import {
    ContentTypeBinaryField,
    DotCMSClazzes,
    DotCMSContentlet,
    DotCMSContentTypeField,
    DotCMSDataTypes,
    DotCMSFieldTypes
} from '@dotcms/dotcms-models';

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
 * Image editing uses Dojo DOM events via `useLegacyDojoImageEditor` on the inner
 * file field (the JSP listener in `edit_field.jsp` opens the editor).
 */
@Component({
    selector: 'dot-binary-field-ce-bridge',
    template: `
        @if ($field(); as field) {
            <dot-file-field
                [field]="field"
                [contentlet]="$contentlet()"
                [hasError]="false"
                [enableImageEditor]="$imageEditor()"
                [useLegacyDojoImageEditor]="true"
                (valueUpdated)="valueUpdated.emit($event)" />
        }
    `,
    imports: [DotFileFieldComponent],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldCeBridgeComponent {
    /** Content type field definition (set imperatively by the legacy JSP). */
    @Input({ required: true })
    set field(value: DotCMSContentTypeField) {
        // The bridge exists to render whatever the legacy JSP hands it AS a binary field, so
        // all three discriminants are forced together — setting `fieldType` alone used to
        // produce a field whose class said one thing and whose type said another. Inert at
        // runtime: nothing under `dot-file-field` reads `clazz` or `dataType`, and
        // `INPUT_TYPES.Binary` and `DotCMSFieldTypes.BINARY` are both `'Binary'`.
        this.$field.set({
            ...value,
            clazz: DotCMSClazzes.BINARY,
            dataType: DotCMSDataTypes.SYSTEM,
            fieldType: DotCMSFieldTypes.BINARY
        });
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

    /** Null until the legacy JSP sets it — the template waits rather than rendering a blank field. */
    protected readonly $field = signal<ContentTypeBinaryField | null>(null);
    protected readonly $contentlet = signal<DotCMSContentlet>({} as DotCMSContentlet);
    protected readonly $imageEditor = signal<boolean>(true);
}
