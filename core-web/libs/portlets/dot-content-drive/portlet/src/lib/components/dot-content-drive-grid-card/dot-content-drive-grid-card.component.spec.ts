import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { beforeEach, describe, expect, it, Mock, vi } from 'vitest';

import { By } from '@angular/platform-browser';

import { Avatar } from 'primeng/avatar';
import { Card } from 'primeng/card';
import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotContentDriveItem } from '@dotcms/dotcms-models';
import {
    DotContentletStatusBadgeComponent,
    DotContentThumbnailComponent,
    DotGravatarDirective
} from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveGridCardComponent } from './dot-content-drive-grid-card.component';
import {
    GRID_FOLDER,
    GRID_IMAGE,
    GRID_LANGUAGES,
    GRID_LOCKED_BLOG,
    GRID_MESSAGES,
    GRID_ORPHAN_PAGE
} from './grid-card.mocks';

describe('DotContentDriveGridCardComponent', () => {
    let spectator: Spectator<DotContentDriveGridCardComponent>;

    const createComponent = createComponentFactory({
        component: DotContentDriveGridCardComponent,
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService(GRID_MESSAGES) }
        ],
        detectChanges: false
    });

    const render = (item: DotContentDriveItem, inputs: Record<string, unknown> = {}) => {
        spectator = createComponent({
            props: { item, cardId: 'card-7', languagesMap: GRID_LANGUAGES, ...inputs } as never
        });
        spectator.detectChanges();
    };

    const text = (testId: string) => spectator.query(byTestId(testId))?.textContent?.trim();

    describe('content card (US1 #3, #7)', () => {
        beforeEach(() => render(GRID_IMAGE));

        it('should show the preview the table shows, for the item itself', () => {
            const thumbnail = spectator.query(DotContentThumbnailComponent);

            expect(thumbnail).toBeTruthy();
            expect(thumbnail?.$contentlet()).toBe(GRID_IMAGE);
            expect(spectator.query(byTestId('grid-card-folder-icon'))).toBeNull();
        });

        it('should put the avatar and the status over the preview, and the checkbox opposite', () => {
            const overlay = spectator.query(byTestId('grid-card-overlay'));

            expect(overlay?.querySelector('[data-testid="grid-card-avatar"]')).toBeTruthy();
            expect(overlay?.querySelector('[data-testid="grid-card-status"]')).toBeTruthy();
            expect(spectator.query(byTestId('grid-card-checkbox'))).toBeTruthy();
        });

        it('should give the status badge the item as its state', () => {
            expect(spectator.query(DotContentletStatusBadgeComponent)?.state()).toBe(GRID_IMAGE);
        });

        it('should show the title, the language and the content type', () => {
            expect(text('grid-card-title')).toBe(GRID_IMAGE.title);
            expect(text('grid-card-language')).toBe('en-US');
            expect(text('grid-card-type')).toBe('Image');
        });

        it("should show the editor's initial and make no Gravatar request", () => {
            expect(spectator.query(Avatar)?.label).toBe('L');
            expect(spectator.query(DotGravatarDirective)).toBeNull();
        });

        it('should mark the preview as the drag picture', () => {
            expect(
                spectator.query(byTestId('grid-card-thumbnail'))?.hasAttribute('data-drag-preview')
            ).toBe(true);
        });

        it('should name the editor for screen readers and on hover', () => {
            expect(text('grid-card-editor-label')).toBe('Edited by: Luca Romano');
        });

        it('should show no date', () => {
            expect(spectator.query(byTestId('grid-card-date'))).toBeNull();
        });
    });

    describe('accessible name (FR-031)', () => {
        it('should give the title the id the grid labels the card by', () => {
            render(GRID_LOCKED_BLOG);

            expect(spectator.query('#card-7-label')?.textContent?.trim()).toBe(
                GRID_LOCKED_BLOG.title
            );
        });

        it('should sum up the rest under the id the grid describes the card by', () => {
            render(GRID_LOCKED_BLOG);

            const description = spectator.query('#card-7-description')?.textContent ?? '';

            ['Draft', 'es-ES', 'Blog', 'Edited by: Maya Chen', 'Locked by: Maya Chen'].forEach(
                (part) => expect(description).toContain(part)
            );
        });

        it('should describe a folder by its type and owner only', () => {
            render(GRID_FOLDER);

            const description = spectator.query('#card-7-description')?.textContent ?? '';

            expect(description).toContain('Folder');
            expect(description).toContain('Edited by: Daniel Ortiz');
            expect(description).not.toContain('Unlocked');
        });
    });

    describe('spacing (design)', () => {
        it("should set the body's padding and gap through the card's design tokens", () => {
            render(GRID_IMAGE);

            expect(spectator.query(Card)?.dt()).toEqual({
                body: { padding: '1rem 1rem 0.875rem', gap: '0.375rem' }
            });
        });
    });

    describe('lock (US1 #10)', () => {
        it('should show a closed lock naming who holds it', () => {
            render(GRID_LOCKED_BLOG);

            const lock = spectator.query(byTestId('grid-card-lock'));

            expect(lock?.classList).toContain('pi-lock');
            expect(text('grid-card-lock-label')).toBe('Locked by: Maya Chen');
        });

        it('should show an open lock when nobody holds it', () => {
            render(GRID_IMAGE);

            const lock = spectator.query(byTestId('grid-card-lock'));

            expect(lock?.classList).toContain('pi-lock-open');
            expect(text('grid-card-lock-label')).toBe('Unlocked');
        });

        it('should say "Locked" when the holder cannot be named', () => {
            render({ ...GRID_LOCKED_BLOG, lockedBy: 'some-user-id' } as DotContentDriveItem);

            expect(text('grid-card-lock-label')).toBe('Locked');
        });
    });

    describe('missing editor (edge case)', () => {
        it('should show a neutral placeholder and say the editor is unknown', () => {
            render(GRID_ORPHAN_PAGE);

            expect(spectator.query(Avatar)?.icon).toBe('pi pi-user');
            expect(text('grid-card-editor-label')).toBe('Edited by: Unknown');
        });
    });

    describe('editor named by an email (privacy)', () => {
        it('should show the initial and never ask Gravatar for the address', () => {
            // A user with no first or last name is named after their email. Hashing it to fetch a
            // picture would send it to gravatar.com from every card they edited.
            render({ ...GRID_IMAGE, modUserName: 'luca@example.com' } as DotContentDriveItem);

            expect(spectator.query(Avatar)?.label).toBe('L');
            expect(spectator.query('img')?.getAttribute('src') ?? '').not.toContain('gravatar');
        });
    });

    describe('folder card (US1 #5, #12)', () => {
        beforeEach(() => render(GRID_FOLDER));

        it('should show the folder icon instead of a preview', () => {
            expect(
                spectator
                    .query(byTestId('grid-card-folder-icon'))
                    ?.hasAttribute('data-drag-preview')
            ).toBe(true);
            expect(spectator.query(DotContentThumbnailComponent)).toBeNull();
        });

        it('should show its name, its owner and the word Folder', () => {
            expect(text('grid-card-title')).toBe('activities');
            expect(spectator.query(Avatar)?.label).toBe('D');
            expect(text('grid-card-type')).toBe('Folder');
        });

        it('should show no status, no language and no lock', () => {
            expect(spectator.query(byTestId('grid-card-status'))).toBeNull();
            expect(spectator.query(byTestId('grid-card-language'))).toBeNull();
            expect(spectator.query(byTestId('grid-card-lock'))).toBeNull();
        });
    });

    describe('busy card (FR-028)', () => {
        beforeEach(() => render(GRID_IMAGE, { busy: true }));

        it('should show a spinner in place of the preview', () => {
            expect(spectator.query(byTestId('grid-card-busy'))).toBeTruthy();
            expect(spectator.query(DotContentThumbnailComponent)).toBeNull();
        });

        it('should disable its checkbox', () => {
            expect(
                spectator.query(byTestId('grid-card-checkbox'))?.querySelector('input')?.disabled
            ).toBe(true);
        });
    });

    describe('selection', () => {
        it('should tick the checkbox when selected', async () => {
            render(GRID_IMAGE, { selected: true });
            // `ngModel` writes the value a microtask after the first check.
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            expect(
                spectator.query(byTestId('grid-card-checkbox'))?.querySelector('input')?.checked
            ).toBe(true);
        });

        it('should outline a selected card, as the design does', () => {
            render(GRID_IMAGE, { selected: true });

            expect(spectator.query(byTestId('grid-card-root'))?.className).toContain(
                'border-(--p-primary-color)!'
            );
        });

        it('should leave an unselected card with the theme border', () => {
            render(GRID_IMAGE);

            expect(spectator.query(byTestId('grid-card-root'))?.className).not.toContain(
                'border-(--p-primary-color)'
            );
        });

        it('should report a checkbox click and not open the item', () => {
            render(GRID_IMAGE);
            const toggle = vi.fn();
            const open = vi.fn();
            spectator.output('toggleSelect').subscribe(toggle);
            spectator.output('open').subscribe(open);

            spectator.click(byTestId('grid-card-checkbox'));

            expect(toggle).toHaveBeenCalledTimes(1);
            expect(open).not.toHaveBeenCalled();
        });
    });

    describe('more actions (US1 #11)', () => {
        it('should report a ⋮ click and not open the item', () => {
            render(GRID_IMAGE);
            const menu = vi.fn();
            const open = vi.fn();
            spectator.output('menu').subscribe(menu);
            spectator.output('open').subscribe(open);

            spectator.click(
                spectator.query(byTestId('grid-card-menu'))?.querySelector('button') as Element
            );

            expect(menu).toHaveBeenCalledTimes(1);
            expect(open).not.toHaveBeenCalled();
        });

        it('should let the ⋮ click reach the page, so a second click closes the menu', () => {
            // The menu closes on PrimeNG's document click listener. Stopping the click on the card
            // left the menu open on a second ⋮ click, where the table's closes it.
            render(GRID_IMAGE);
            const reachedDocument = vi.fn();
            document.addEventListener('click', reachedDocument);

            spectator.click(
                spectator.query(byTestId('grid-card-menu'))?.querySelector('button') as Element
            );
            document.removeEventListener('click', reachedDocument);

            expect(reachedDocument).toHaveBeenCalled();
        });

        it('should label the ⋮ button', () => {
            render(GRID_IMAGE);

            expect(
                spectator
                    .query(byTestId('grid-card-menu'))
                    ?.querySelector('button')
                    ?.getAttribute('aria-label')
            ).toBe('More actions');
        });

        it('should not offer ⋮ in a read-only grid', () => {
            render(GRID_IMAGE, { readOnly: true });

            expect(spectator.query(byTestId('grid-card-menu'))).toBeNull();
        });
    });

    describe('opening (US5 #1)', () => {
        let open: Mock<(event: MouseEvent) => void>;

        beforeEach(() => {
            render(GRID_LOCKED_BLOG);
            open = vi.fn<(event: MouseEvent) => void>();
            spectator.output('open').subscribe(open);
        });

        it('should open on a plain click on the card', () => {
            spectator
                .query(byTestId('grid-card-title'))
                ?.dispatchEvent(new MouseEvent('click', { bubbles: true, detail: 1 }));

            expect(open).toHaveBeenCalledTimes(1);
        });

        it.each(['grid-card-avatar', 'grid-card-lock'])(
            'should not open from a click on %s',
            (testId) => {
                spectator.click(byTestId(testId));

                expect(open).not.toHaveBeenCalled();
            }
        );

        it.each(['shiftKey', 'ctrlKey', 'metaKey', 'altKey'])(
            'should not open from a click with %s held',
            (modifier) => {
                spectator
                    .query(byTestId('grid-card-title'))
                    ?.dispatchEvent(
                        new MouseEvent('click', { bubbles: true, detail: 1, [modifier]: true })
                    );

                expect(open).not.toHaveBeenCalled();
            }
        );

        it('should not open twice for a double-click', () => {
            const title = spectator.query(byTestId('grid-card-title')) as HTMLElement;

            title.dispatchEvent(new MouseEvent('click', { bubbles: true, detail: 1 }));
            title.dispatchEvent(new MouseEvent('click', { bubbles: true, detail: 2 }));

            expect(open).toHaveBeenCalledTimes(1);
        });
    });

    describe('long text (US2 #1, FR-010)', () => {
        const tooltipOf = (testId: string) =>
            spectator.debugElement.query(By.css(`[data-testid="${testId}"]`)).injector.get(Tooltip);

        beforeEach(() => render(GRID_IMAGE));

        it('should keep the title on one line, cut with an ellipsis', () => {
            expect(spectator.query(byTestId('grid-card-title'))?.classList).toContain('truncate');
        });

        it('should show the full title on hover, only when it is cut', () => {
            const tooltip = tooltipOf('grid-card-title');

            expect(tooltip.content).toBe(GRID_IMAGE.title);
            expect(tooltip.showOnEllipsis).toBe(true);
        });

        it('should treat the content type the same way', () => {
            const tooltip = tooltipOf('grid-card-type');

            expect(spectator.query(byTestId('grid-card-type'))?.classList).toContain('truncate');
            expect(tooltip.content).toBe('Image');
            expect(tooltip.showOnEllipsis).toBe(true);
        });
    });
});
