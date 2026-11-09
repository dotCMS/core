import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotContainersByContentType, DotMessageService } from '@dotcms/data-access';
import { DotContainer } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentTypeInUseDialogComponent } from './dot-content-type-in-use-dialog.component';

const messageServiceMock = new MockDotMessageService({
    'message.contenttypes.in_use.explainer': 'The containers below use this Content Type.',
    'message.contenttypes.in_use.truncated': 'Showing {0} of {1} containers',
    'contenttypes.action.delete': 'Delete',
    'contenttypes.action.cancel': 'Cancel',
    Archived: 'Archived'
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
            { provide: DynamicDialogConfig, useValue: { data: undefined } }
        ],
        detectChanges: false
    });

    const setup = (data: DotContainersByContentType | undefined): void => {
        spectator = createComponent({
            providers: [{ provide: DynamicDialogConfig, useValue: { data } }]
        });
        spectator.detectChanges();
    };

    const usage = (
        containers: Partial<DotContainer>[],
        total = containers.length
    ): DotContainersByContentType => ({ containers: containers as DotContainer[], total });

    it('renders each container title as text and the path when present', () => {
        // Given: two containers, one with a path and one with neither path nor site
        setup(
            usage([
                { identifier: 'a', title: 'Home <b>1</b>', path: '/a-path' },
                { identifier: 'b', title: 'Second' }
            ])
        );

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

    it('shows the site when a container has no path', () => {
        // Given: a database container, which has a site but no path
        setup(usage([{ identifier: 'a', title: 'Default', hostName: 'demo.dotcms.com' }]));

        // When: the dialog renders
        const path = spectator.query(byTestId('in-use-container-path'));

        // Then: the site is shown where the path would be
        expect(path?.textContent?.trim()).toBe('demo.dotcms.com');
    });

    it('tags archived containers only', () => {
        // Given: an archived container and an active one
        setup(
            usage([
                { identifier: 'a', title: 'Old', archived: true },
                { identifier: 'b', title: 'Current', archived: false }
            ])
        );

        // When: the dialog renders
        const rows = spectator.queryAll(byTestId('in-use-container'));
        const tags = spectator.queryAll(byTestId('in-use-container-archived'));

        // Then: only the archived container carries the tag
        expect(tags.length).toBe(1);
        expect(rows[0].contains(tags[0])).toBe(true);
        expect(tags[0].textContent).toContain('Archived');
    });

    it('says how many containers are not listed when the list is cut short', () => {
        // Given: two containers listed out of five
        setup(
            usage(
                [
                    { identifier: 'a', title: 'A' },
                    { identifier: 'b', title: 'B' }
                ],
                5
            )
        );

        // When: the dialog renders
        const notice = spectator.query(byTestId('in-use-truncated'));

        // Then: the notice gives both counts
        expect(notice?.textContent?.trim()).toBe('Showing 2 of 5 containers');
    });

    it('shows no notice when every container is listed', () => {
        // Given: as many containers listed as use the content type
        setup(usage([{ identifier: 'a', title: 'A' }]));

        // When: the dialog renders
        // Then: there is no notice
        expect(spectator.query(byTestId('in-use-truncated'))).toBeNull();
    });

    it('renders an empty list without data', () => {
        // Given: the dialog is opened without data
        setup(undefined);

        // When: the dialog renders
        // Then: no rows and no notice
        expect(spectator.queryAll(byTestId('in-use-container')).length).toBe(0);
        expect(spectator.query(byTestId('in-use-truncated'))).toBeNull();
    });

    it('closes with true on Delete', () => {
        // Given: the dialog is open
        setup(usage([{ identifier: 'a', title: 'A' }]));

        // When: Delete is clicked
        spectator.click('[data-testid="in-use-delete"] button');

        // Then: the dialog closes with true
        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith(true);
    });

    it('closes with false on Cancel', () => {
        // Given: the dialog is open
        setup(usage([{ identifier: 'a', title: 'A' }]));

        // When: Cancel is clicked
        spectator.click('[data-testid="in-use-cancel"] button');

        // Then: the dialog closes with false
        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith(false);
    });
});
