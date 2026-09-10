import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, Subject } from 'rxjs';

import { DialogService } from 'primeng/dynamicdialog';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotSelectExistingContentComponent } from '@dotcms/edit-content';
import { DOT_RELATIONSHIP_PICKER, DotRelationshipPicker } from '@dotcms/ui';

import { provideContentDriveRelationshipPicker } from './content-drive-relationship-picker';

import { DotContentDriveRelationshipFooterComponent } from '../components/dot-content-drive-relationship-footer/dot-content-drive-relationship-footer.component';

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

    it('should open the same content-selection dialog Content Drive always opened', () => {
        dialogService.open.mockReturnValue({ onClose: of(undefined) } as never);

        picker.open(relationshipField(), []).subscribe();

        expect(dialogService.open).toHaveBeenCalledWith(
            DotSelectExistingContentComponent,
            expect.objectContaining({
                // The field's name is the dialog title; the rest is the geometry the drive shipped.
                header: 'Author',
                width: '90%',
                height: '90%',
                modal: true,
                appendTo: 'body',
                maskStyleClass: 'p-dialog-mask-dynamic p-dialog-relationship-field'
            })
        );
    });

    it('should use the filter footer, where Apply is enabled at zero selections', () => {
        // Clearing the relationship is a valid filter state — which is the whole reason this
        // footer exists instead of edit-content's own.
        dialogService.open.mockReturnValue({ onClose: of(undefined) } as never);

        picker.open(relationshipField(), []).subscribe();

        expect(dialogService.open).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                templates: { footer: DotContentDriveRelationshipFooterComponent }
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
                    currentItemsIds: ['inode-1']
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
