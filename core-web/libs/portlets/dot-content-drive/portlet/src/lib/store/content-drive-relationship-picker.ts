import { Observable, of } from 'rxjs';

import { inject, Provider } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { map, take } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { AddRelationshipsComponent } from '@dotcms/edit-content';
import {
    DOT_RELATIONSHIP_PICKER,
    DotRelationshipPicker,
    getContentTypeIdFromRelationship
} from '@dotcms/ui';

/**
 * Content Drive's {@link DotRelationshipPicker} — the same dialog, opened with the same options the
 * field filter opened it with before the chip moved to `@dotcms/ui` (FR-021).
 *
 * Re-pointed at `AddRelationshipsComponent` by #37192. What this function returns, when it
 * completes and what the chip does with the result are unchanged — see
 * `specs/37192-relationship-field-assetpicker/contracts/relationship-picker.contract.md` (C1–C5),
 * whose acceptance signal is this file's spec passing **unmodified**.
 *
 * This provider is why the capability is a token: the dialog lives in
 * `@dotcms/edit-content`, which already depends on `@dotcms/ui`. A shared chip importing it would
 * make that dependency circular and drag the whole of `edit-content` into the legacy custom-element
 * bundle `@dotcms/ui` is compiled into. Inverted, the portlet — which may import both — supplies it.
 *
 * @param dialogService PrimeNG's dialog service, provided alongside this.
 * @return The picker capability.
 */
export function createContentDriveRelationshipPicker(
    dialogService: DialogService
): DotRelationshipPicker {
    return {
        open: (
            field: DotCMSContentTypeField,
            selectedInodes: string[]
        ): Observable<DotCMSContentlet[]> => {
            const ref = dialogService.open(AddRelationshipsComponent, {
                header: field.name,
                // Full screen runs through PrimeNG's own maximized state, driven from this
                // dialog's header. No maximize button is rendered — PrimeNG's lives in the
                // header we hid. The Image Editor omits this flag and still works, but the
                // Asset Picker sets it deliberately and is the closer analogue.
                maximizable: true,
                // Windowed size as a single `width`/`height`, never `90%` capped by an inline
                // `max-width`: those caps survive maximisation and keep clamping the dialog, which is
                // why the full-screen toggle appeared to do nothing. Same reasoning — and the same
                // shape — as the Asset Picker and the Image Editor.
                //
                // `.p-dialog` is capped at `max-height: 90%` by the theme, so asking for more than
                // 90vh would have no effect anyway.
                width: 'min(90vw, 114rem)',
                height: 'min(90vh, 68rem)',
                // The dialog fills its host so it can grow with the full-screen toggle.
                contentStyle: { height: '100%', overflow: 'hidden', padding: '0' },
                modal: true,
                appendTo: 'body',
                baseZIndex: 10000,
                maskStyleClass: 'p-dialog-mask-dynamic p-dialog-relationship-field',
                data: {
                    contentTypeId: getContentTypeIdFromRelationship(field),
                    selectionMode: 'single',
                    // Nothing to seed from: this caller holds inodes, not contentlets, because
                    // that is what the token's signature carries. `selectedInodes` is the
                    // best-effort path — rows are marked as they appear. Edit-time hands over the
                    // contentlets themselves and never relies on this.
                    selected: [],
                    selectedInodes,
                    // Apply stays enabled at zero selections — clearing is a valid filter state —
                    // which the dialog's own footer now does too (#37192). So the replacement
                    // footer this provider used to inject is gone, and only its label travels.
                    // The old one could reach the dialog's store solely because that store was
                    // `providedIn: 'root'`; this one is per-dialog, as it should be.
                    confirmLabel: 'content-drive.field-filter.apply'
                }
            });

            // A dialog that could not open still has to complete, or the caller waits forever for a
            // selection nobody can make.
            if (!ref) {
                return of([]);
            }

            // A cancel closes with `undefined`; the contract promises an empty list either way, so
            // the translation happens here rather than in every chip.
            return ref.onClose.pipe(
                take(1),
                map((items) => (Array.isArray(items) ? (items as DotCMSContentlet[]) : []))
            );
        }
    };
}

/**
 * Provides {@link DOT_RELATIONSHIP_PICKER} for the Content Drive toolbar's field filters.
 *
 * Goes on the shell, beside the filter facade: `DialogService` is component-scoped, so the dialog
 * belongs to this portlet's subtree rather than the application.
 */
export function provideContentDriveRelationshipPicker(): Provider {
    return {
        provide: DOT_RELATIONSHIP_PICKER,
        useFactory: () => createContentDriveRelationshipPicker(inject(DialogService))
    };
}
