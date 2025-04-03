import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';

import { DotEditContentSidebarSectionComponent } from './dot-edit-content-sidebar-section.component';

describe('DotEditContentSidebarSectionComponent', () => {
    let spectator: SpectatorHost<DotEditContentSidebarSectionComponent>;

    const createHost = createHostFactory({
        component: DotEditContentSidebarSectionComponent,
        template: `
            <dot-edit-content-sidebar-section [title]="title">
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
                title: 'Test Section'
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
            const header = spectator.query(byTestId('dot-section-header'));
            const title = spectator.query(byTestId('dot-section-title'));

            expect(section).toBeTruthy();
            expect(header).toBeTruthy();
            expect(title).toBeTruthy();
            expect(title).toHaveText('Test Section');
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
            spectator.setHostInput('title', null);
        });

        it('should not render header section', () => {
            const header = spectator.query(byTestId('dot-section-header'));
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
});
