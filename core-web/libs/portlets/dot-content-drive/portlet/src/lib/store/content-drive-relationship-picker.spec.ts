import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, Subject } from 'rxjs';

import { DialogService } from 'primeng/dynamicdialog';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { AddRelationshipsComponent } from '@dotcms/edit-content';
import { DOT_RELATIONSHIP_PICKER, DotRelationshipPicker } from '@dotcms/ui';

import { provideContentDriveRelationshipPicker } from './content-drive-relationship-picker';

const relationshipField = (): DotCMSContentTypeField =>
    ({
        variable: 'author',
        name: 'Author',
        fieldType: 'Relationship',
        relationships: { velocityVar: 'Author.blogs', cardinality: 1, isParentField: true }
    }) as DotCMSContentTypeField;

const contentlet = (identifier: string): DotCMSContentlet =>
    ({ identifier, inode: `inode-${identifier}`, title: identifier }) as DotCMSContentlet;

/**
 * The Content Drive side of the {@link DOT_RELATIONSHIP_PICKER} seam.
 *
 * This wiring used to live inside `DotContentDriveFieldFilterComponent` and was covered by its
 * spec. Moving it behind a token to keep `@dotcms/ui` free of `@dotcms/edit-content` (FR-020) also
 * moved it out of that coverage: the chip's spec now asserts against a token mock, which by design
 * says nothing about what this provider actually opens. These tests are that half.
 */
describe('provideContentDriveRelationshipPicker', () => {
    let spectator: SpectatorService<unknown>;
    let picker: DotRelationshipPicker;
    let dialogService: jest.Mocked<Pick<DialogService, 'open'>>;

    const createService = createServiceFactory({
        service: class {},
        providers: [mockProvider(DialogService, { open: jest.fn() })]
    });

    const setup = () => {
        spectator = createService({ providers: [provideContentDriveRelationshipPicker()] });
        dialogService = spectator.inject(DialogService) as unknown as jest.Mocked<
            Pick<DialogService, 'open'>
        >;
        picker = spectator.inject(DOT_RELATIONSHIP_PICKER);
    };

    beforeEach(() => setup());

    afterEach(() => jest.clearAllMocks());

    /**
     * Names the component this provider opens.
     *
     * Updated by #37192 when the dialog was replaced. That is permitted: the contract
     * (`specs/37192-relationship-field-assetpicker/contracts/relationship-picker.contract.md`, C5)
     * fixes what `open()` **returns** and when it completes, and explicitly allows the dialog
     * itself to change — "its dialog may look different after this work". The five cases below,
     * which are the contract proper, passed unmodified through that swap.
     */
    it('should open the content-selection dialog Content Drive opens', () => {
        dialogService.open.mockReturnValue({ onClose: of(undefined) } as never);

        picker.open(relationshipField(), []).subscribe();

        expect(dialogService.open).toHaveBeenCalledWith(
            AddRelationshipsComponent,
            expect.objectContaining({
                // The field's name is the dialog title; the rest is the dialog's geometry.
                //
                // Updated by #37192: the windowed size moved from `90%` plus an inline
                // `max-width` to a single `min()` width, because those inline caps survive
                // maximisation and kept the full-screen toggle from doing anything. Presentation,
                // which the contract explicitly allows to change (C5).
                header: 'Author',
                width: 'min(90vw, 114rem)',
                height: 'min(90vh, 68rem)',
                modal: true,
                appendTo: 'body',
                maskStyleClass: 'p-dialog-mask-dynamic p-dialog-relationship-field'
            })
        );
    });

    it('should label the confirm action "Apply", as a filter rather than an edit', () => {
        // Clearing the relationship is a valid filter state, and the dialog's own footer keeps
        // Apply enabled at zero selections (#37192) — so this surface no longer injects a
        // replacement footer, only its label. The replaced footer could reach the dialog's store
        // solely because that store was `providedIn: 'root'`; the new one is per-dialog.
        dialogService.open.mockReturnValue({ onClose: of(undefined) } as never);

        picker.open(relationshipField(), []).subscribe();

        expect(dialogService.open).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                data: expect.objectContaining({
                    confirmLabel: 'content-drive.field-filter.apply'
                })
            })
        );
    });

    it('should derive the target content type from the relationship field', () => {
        dialogService.open.mockReturnValue({ onClose: of(undefined) } as never);

        picker.open(relationshipField(), ['inode-1']).subscribe();

        expect(dialogService.open).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                data: {
                    // `Author.blogs` → `Author`.
                    contentTypeId: 'Author',
                    // Always single: a filter matches one related value, and cardinality is an
                    // edit-time concern that would disable rows here for no reason.
                    selectionMode: 'single',
                    // Nothing to seed from: this caller holds inodes, not contentlets, because
                    // that is all the token's signature carries. #37192.
                    selected: [],
                    selectedInodes: ['inode-1'],
                    confirmLabel: 'content-drive.field-filter.apply'
                }
            })
        );
    });

    it('should report the chosen contentlets', () => {
        const onClose = new Subject<DotCMSContentlet[]>();
        dialogService.open.mockReturnValue({ onClose } as never);
        const received: DotCMSContentlet[][] = [];

        picker.open(relationshipField(), []).subscribe((items) => received.push(items));
        onClose.next([contentlet('id-1')]);

        expect(received).toEqual([[contentlet('id-1')]]);
    });

    it('should report an empty list when the editor cancels', () => {
        // A cancel closes with `undefined`. The contract promises a list either way, so no chip
        // has to guard for it.
        const onClose = new Subject<DotCMSContentlet[] | undefined>();
        dialogService.open.mockReturnValue({ onClose } as never);
        const received: DotCMSContentlet[][] = [];

        picker.open(relationshipField(), []).subscribe((items) => received.push(items));
        onClose.next(undefined);

        expect(received).toEqual([[]]);
    });

    it('should complete after the first close, so a reopened dialog cannot write twice', () => {
        const onClose = new Subject<DotCMSContentlet[]>();
        dialogService.open.mockReturnValue({ onClose } as never);
        let completed = false;

        picker.open(relationshipField(), []).subscribe({ complete: () => (completed = true) });
        onClose.next([contentlet('id-1')]);

        expect(completed).toBe(true);
    });

    it('should complete with an empty list when the dialog could not open', () => {
        // Otherwise the chip waits forever for a selection nobody can make.
        dialogService.open.mockReturnValue(undefined as never);
        const received: DotCMSContentlet[][] = [];
        let completed = false;

        picker.open(relationshipField(), []).subscribe({
            next: (items) => received.push(items),
            complete: () => (completed = true)
        });

        expect(received).toEqual([[]]);
        expect(completed).toBe(true);
    });
});
