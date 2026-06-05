import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@ngneat/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';

import { DotLocalstorageService } from '@dotcms/data-access';

import { DotEditContentSidebarSectionComponent } from './dot-edit-content-sidebar-section.component';

const STORAGE_KEY = 'dot-edit-content.section.workflow';

describe('DotEditContentSidebarSectionComponent', () => {
    let spectator: SpectatorHost<DotEditContentSidebarSectionComponent>;

    const createHost = createHostFactory({
        component: DotEditContentSidebarSectionComponent,
        providers: [mockProvider(DotLocalstorageService)],
        template: `
            <dot-edit-content-sidebar-section [title]="title" [key]="key">
                <ng-template #sectionAction>
                    <div data-testid="action-content">Action Content</div>
                </ng-template>
                <div data-testid="projected-content">Projected Content</div>
            </dot-edit-content-sidebar-section>
        `
    });

    beforeEach(() => {
        spectator = createHost(null, {
            hostProps: {
                title: 'Test Section',
                key: ''
            }
        });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('With title', () => {
        beforeEach(() => {
            spectator.setHostInput('title', 'Test Section');
        });

        it('should render main structure and title', () => {
            const section = spectator.query(byTestId('dot-section'));
            const header = spectator.query(byTestId('dot-section-toggle'));
            const title = spectator.query(byTestId('dot-section-title'));

            expect(section).toBeTruthy();
            expect(header).toBeTruthy();
            expect(title).toBeTruthy();
            expect(title).toHaveText('Test Section');
        });

        it('should render the chevron icon', () => {
            expect(spectator.query(byTestId('dot-section-chevron'))).toBeTruthy();
        });

        it('should render action section', fakeAsync(() => {
            tick();

            const actionSection = spectator.query(byTestId('dot-section-action'));
            const actionContent = spectator.query(byTestId('action-content'));

            expect(actionSection).toBeTruthy();
            expect(actionContent).toBeTruthy();
            expect(actionContent).toHaveText('Action Content');
        }));
    });

    describe('Without title', () => {
        beforeEach(() => {
            // Create fresh host with title: null to avoid ExpressionChangedAfterItHasBeenCheckedError
            spectator = createHost(null, {
                hostProps: { title: null, key: '' }
            });
        });

        it('should not render header section', () => {
            const header = spectator.query(byTestId('dot-section-toggle'));
            expect(header).toBeFalsy();
        });

        it('should still render projected content', () => {
            const contentSection = spectator.query(byTestId('dot-section-content'));
            const projectedContent = spectator.query(byTestId('projected-content'));

            expect(contentSection).toBeTruthy();
            expect(projectedContent).toBeTruthy();
            expect(projectedContent).toHaveText('Projected Content');
        });
    });

    it('should render projected content', () => {
        const contentSection = spectator.query(byTestId('dot-section-content'));
        const projectedContent = spectator.query(byTestId('projected-content'));

        expect(contentSection).toBeTruthy();
        expect(projectedContent).toBeTruthy();
        expect(projectedContent).toHaveText('Projected Content');
    });

    describe('Without key (backward-compatible)', () => {
        let localStorageService: jest.Mocked<DotLocalstorageService>;

        beforeEach(() => {
            localStorageService = spectator.inject(DotLocalstorageService);
        });

        it('should not read from storage on init', () => {
            expect(localStorageService.getItem).not.toHaveBeenCalled();
        });

        it('should collapse in-memory but not persist on toggle', () => {
            // Starts expanded
            expect(spectator.query(byTestId('dot-section-content'))).toBeTruthy();

            // Collapses in-memory...
            spectator.click(byTestId('dot-section-toggle'));
            expect(spectator.query(byTestId('dot-section-content'))).toBeFalsy();

            // ...but never writes to storage
            expect(localStorageService.setItem).not.toHaveBeenCalled();
        });
    });

    describe('With key (collapsible + persistent)', () => {
        let localStorageService: jest.Mocked<DotLocalstorageService>;

        beforeEach(() => {
            spectator = createHost(null, {
                hostProps: { title: 'Workflow', key: 'workflow' },
                detectChanges: false
            });
            localStorageService = spectator.inject(DotLocalstorageService);
        });

        it('should toggle content visibility on header click', () => {
            localStorageService.getItem.mockReturnValue(false);
            spectator.detectChanges();

            // Starts expanded
            expect(spectator.query(byTestId('dot-section-content'))).toBeTruthy();

            // Collapse
            spectator.click(byTestId('dot-section-toggle'));
            expect(spectator.query(byTestId('dot-section-content'))).toBeFalsy();

            // Expand again
            spectator.click(byTestId('dot-section-toggle'));
            expect(spectator.query(byTestId('dot-section-content'))).toBeTruthy();
        });

        it('should toggle on Enter key', () => {
            localStorageService.getItem.mockReturnValue(false);
            spectator.detectChanges();

            const header = spectator.query(byTestId('dot-section-toggle')) as HTMLElement;
            header.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
            spectator.detectChanges();

            expect(spectator.query(byTestId('dot-section-content'))).toBeFalsy();
        });

        it('should persist the collapsed state on toggle', () => {
            localStorageService.getItem.mockReturnValue(false);
            spectator.detectChanges();

            spectator.click(byTestId('dot-section-toggle'));

            expect(localStorageService.setItem).toHaveBeenCalledWith(STORAGE_KEY, true);
        });

        it('should seed the initial collapsed state from storage', () => {
            localStorageService.getItem.mockReturnValue(true);
            spectator.detectChanges();

            expect(localStorageService.getItem).toHaveBeenCalledWith(STORAGE_KEY);
            expect(spectator.query(byTestId('dot-section-content'))).toBeFalsy();
        });

        it('should rotate the chevron up when expanded (collapsed has it pointing down)', () => {
            localStorageService.getItem.mockReturnValue(false);
            spectator.detectChanges();

            const chevron = spectator.query(byTestId('dot-section-chevron'));
            expect(chevron).toHaveClass('rotate-180');
        });

        it('should not rotate the chevron when collapsed', () => {
            localStorageService.getItem.mockReturnValue(true);
            spectator.detectChanges();

            const chevron = spectator.query(byTestId('dot-section-chevron'));
            expect(chevron).not.toHaveClass('rotate-180');
        });

        it('should not collapse when clicking the action slot', () => {
            localStorageService.getItem.mockReturnValue(false);
            spectator.detectChanges();

            spectator.click(byTestId('dot-section-action'));

            expect(spectator.query(byTestId('dot-section-content'))).toBeTruthy();
            expect(localStorageService.setItem).not.toHaveBeenCalled();
        });
    });
});
