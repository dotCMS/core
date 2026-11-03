import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotContainer } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentTypeInUseDialogComponent } from './dot-content-type-in-use-dialog.component';

const messageServiceMock = new MockDotMessageService({
    'message.contenttypes.in_use.explainer': 'The containers below use this Content Type.',
    'contenttypes.action.delete': 'Delete',
    'contenttypes.action.cancel': 'Cancel'
});

/**
 * Dialog shown before deleting a content type that containers use: it lists the containers
 * and lets the user delete anyway or cancel.
 */
describe('DotContentTypeInUseDialogComponent', () => {
    let spectator: Spectator<DotContentTypeInUseDialogComponent>;

    const createComponent = createComponentFactory({
        component: DotContentTypeInUseDialogComponent,
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: DynamicDialogRef, useValue: { close: vi.fn() } },
            { provide: DynamicDialogConfig, useValue: { data: [] } }
        ],
        detectChanges: false
    });

    const setup = (data: DotContainer[]): void => {
        spectator = createComponent({
            providers: [{ provide: DynamicDialogConfig, useValue: { data } }]
        });
        spectator.detectChanges();
    };

    it('renders each container title as text and the path when present', () => {
        // Given: two containers, one with a path and one without
        setup([
            { identifier: 'a', title: 'Home <b>1</b>', path: '/a-path' },
            { identifier: 'b', title: 'Second' }
        ] as DotContainer[]);

        // When: the dialog renders
        const rows = spectator.queryAll(byTestId('in-use-container'));

        // Then: two rows, titles are rendered as text and the path only for the first container
        expect(rows.length).toBe(2);
        expect(
            rows[0].querySelector('[data-testid="in-use-container-title"]')?.textContent
        ).toContain('Home <b>1</b>');
        expect(spectator.query('li b')).toBeNull();

        const paths = spectator.queryAll(byTestId('in-use-container-path'));
        expect(paths.length).toBe(1);
        expect(rows[0].contains(paths[0])).toBe(true);
        expect(paths[0].textContent?.trim()).toBe('/a-path');
    });

    it('closes with true on Delete', () => {
        // Given: the dialog is open
        setup([{ identifier: 'a', title: 'A' }] as DotContainer[]);

        // When: Delete is clicked
        spectator.click('[data-testid="in-use-delete"] button');

        // Then: the dialog closes with true
        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith(true);
    });

    it('closes with false on Cancel', () => {
        // Given: the dialog is open
        setup([{ identifier: 'a', title: 'A' }] as DotContainer[]);

        // When: Cancel is clicked
        spectator.click('[data-testid="in-use-cancel"] button');

        // Then: the dialog closes with false
        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith(false);
    });
});
