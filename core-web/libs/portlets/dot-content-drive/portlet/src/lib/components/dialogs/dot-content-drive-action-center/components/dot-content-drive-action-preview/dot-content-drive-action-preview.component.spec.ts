import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    DotContentDriveActionPreviewComponent,
    PREVIEW_ROWS_PER_PAGE
} from './dot-content-drive-action-preview.component';

const contentlet = (overrides: Partial<DotCMSContentlet> & { inode: string }): DotCMSContentlet =>
    ({
        identifier: `id-${overrides.inode}`,
        title: `Title ${overrides.inode}`,
        contentType: 'Blog',
        baseType: 'CONTENT',
        live: false,
        working: true,
        archived: false,
        locked: false,
        hasTitleImage: false,
        ...overrides
    }) as DotCMSContentlet;

/** Builds `count` contentlets, used to cross the pagination threshold. */
const contentlets = (count: number): DotCMSContentlet[] =>
    Array.from({ length: count }, (_, index) => contentlet({ inode: `inode-${index}` }));

describe('DotContentDriveActionPreviewComponent', () => {
    let spectator: Spectator<DotContentDriveActionPreviewComponent>;

    const createComponent = createComponentFactory({
        component: DotContentDriveActionPreviewComponent,
        providers: [
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
            })
        ],
        detectChanges: false
    });

    const ITEMS = [
        contentlet({ inode: 'inode-1', title: 'Destination Guide', contentType: 'Web Page' }),
        contentlet({ inode: 'inode-2', title: 'Costa Rica', live: true, hasLiveVersion: true })
    ];

    beforeEach(() => {
        spectator = createComponent({
            props: { items: ITEMS, selection: ITEMS, disabled: false }
        });
        spectator.detectChanges();
    });

    describe('rendering', () => {
        it('should render a row per item', () => {
            expect(spectator.queryAll(byTestId('preview-row')).length).toBe(2);
        });

        it('should render the title, status and type of each row', () => {
            const [firstRow] = spectator.queryAll(byTestId('preview-row'));

            expect(
                firstRow.querySelector('[data-testid="preview-row-title"]').textContent
            ).toContain('Destination Guide');
            expect(
                firstRow.querySelector('[data-testid="preview-row-type"]').textContent
            ).toContain('Web Page');
            // Status comes from the shared badge, which resolves its own label.
            expect(firstRow.querySelector('[data-testid="preview-row-status"] p-tag')).toBeTruthy();
        });

        it('should render nothing but the header for an empty list', () => {
            spectator.setInput('items', []);
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('preview-row')).length).toBe(0);
            expect(spectator.query(byTestId('preview-header-row'))).toBeTruthy();
        });
    });

    describe('selection', () => {
        it('should emit the remaining items when a row is unchecked', () => {
            const emitted = jest.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.click(
                spectator
                    .queryAll(byTestId('preview-row'))[0]
                    .querySelector('[data-testid="preview-row-checkbox"] input')
            );
            spectator.detectChanges();

            expect(emitted).toHaveBeenCalledWith([ITEMS[1]]);
        });

        it('should emit the added item when an unchecked row is checked again', () => {
            spectator.setInput('selection', [ITEMS[1]]);
            spectator.detectChanges();

            const emitted = jest.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.click(
                spectator
                    .queryAll(byTestId('preview-row'))[0]
                    .querySelector('[data-testid="preview-row-checkbox"] input')
            );
            spectator.detectChanges();

            expect(emitted).toHaveBeenCalledWith([ITEMS[1], ITEMS[0]]);
        });

        it('should emit an empty list when the header checkbox clears everything', () => {
            const emitted = jest.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.click(
                spectator.query(byTestId('preview-header-checkbox')).querySelector('input')
            );
            spectator.detectChanges();

            expect(emitted).toHaveBeenCalledWith([]);
        });

        it('should keep same-identifier language variants as distinct rows', () => {
            // Keyed on inode, not identifier: two language versions of one contentlet share an
            // identifier, and inodes are what gets fired.
            const variants = [
                contentlet({ inode: 'inode-en', identifier: 'shared', languageId: 1 }),
                contentlet({ inode: 'inode-es', identifier: 'shared', languageId: 2 })
            ];

            spectator.setInput('items', variants);
            spectator.setInput('selection', variants);
            spectator.detectChanges();

            const emitted = jest.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.click(
                spectator
                    .queryAll(byTestId('preview-row'))[0]
                    .querySelector('[data-testid="preview-row-checkbox"] input')
            );
            spectator.detectChanges();

            expect(emitted).toHaveBeenCalledWith([variants[1]]);
        });
    });

    describe('disabled', () => {
        it('should disable every checkbox', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            const inputs = [
                spectator.query(byTestId('preview-header-checkbox')).querySelector('input'),
                ...spectator
                    .queryAll(byTestId('preview-row-checkbox'))
                    .map((checkbox) => checkbox.querySelector('input'))
            ] as HTMLInputElement[];

            expect(inputs.length).toBe(3);
            inputs.forEach((input) => expect(input.disabled).toBe(true));
        });

        it('should not emit when a disabled row is clicked', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            const emitted = jest.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.click(
                spectator
                    .queryAll(byTestId('preview-row'))[0]
                    .querySelector('[data-testid="preview-row-checkbox"] input')
            );
            spectator.detectChanges();

            expect(emitted).not.toHaveBeenCalled();
        });
    });

    describe('pagination', () => {
        it('should not paginate a selection that fits on one page', () => {
            spectator.setInput('items', contentlets(PREVIEW_ROWS_PER_PAGE));
            spectator.detectChanges();

            expect(spectator.query('.p-paginator')).toBeNull();
        });

        it('should paginate once the selection outgrows a page', () => {
            spectator.setInput('items', contentlets(PREVIEW_ROWS_PER_PAGE + 1));
            spectator.detectChanges();

            expect(spectator.query('.p-paginator')).toBeTruthy();
            expect(spectator.queryAll(byTestId('preview-row')).length).toBe(PREVIEW_ROWS_PER_PAGE);
        });
    });
});
