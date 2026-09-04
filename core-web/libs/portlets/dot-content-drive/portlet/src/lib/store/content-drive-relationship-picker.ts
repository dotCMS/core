import { Observable, of } from 'rxjs';

import { inject, Provider } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { map, take } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotSelectExistingContentComponent } from '@dotcms/edit-content';
import {
    DOT_RELATIONSHIP_PICKER,
    DotRelationshipPicker,
    getContentTypeIdFromRelationship
} from '@dotcms/ui';

import { DotContentDriveRelationshipFooterComponent } from '../components/dot-content-drive-relationship-footer/dot-content-drive-relationship-footer.component';

/**
 * Content Drive's {@link DotRelationshipPicker} — the same dialog, opened with the same options the
 * field filter opened it with before the chip moved to `@dotcms/ui` (FR-021).
 *
 * This provider is why the capability is a token: `DotSelectExistingContentComponent` lives in
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
            const ref = dialogService.open(DotSelectExistingContentComponent, {
                header: field.name,
                width: '90%',
                height: '90%',
                modal: true,
                appendTo: 'body',
                baseZIndex: 10000,
                maskStyleClass: 'p-dialog-mask-dynamic p-dialog-relationship-field',
                style: { 'max-width': '1040px', 'max-height': '800px' },
                // Apply stays enabled at zero selections here: clearing is a valid filter state.
                templates: { footer: DotContentDriveRelationshipFooterComponent },
                data: {
                    contentTypeId: getContentTypeIdFromRelationship(field),
                    selectionMode: 'single',
                    currentItemsIds: selectedInodes
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
