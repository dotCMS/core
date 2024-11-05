import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component, ViewChild } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';

import { DotEditContentSidebarSectionComponent } from './dot-edit-content-sidebar-section.component';

@Component({
    template: `
        <dot-edit-content-sidebar-section [title]="'Test Section'">
            <ng-template #sectionAction>
                <div data-testid="action-content">Action Content</div>
            </ng-template>
            <div data-testid="projected-content">Projected Content</div>
        </dot-edit-content-sidebar-section>
    `
})
class WrapperComponent {
    @ViewChild(DotEditContentSidebarSectionComponent)
    sectionComponent!: DotEditContentSidebarSectionComponent;
}

describe('DotEditContentSidebarSectionComponent', () => {
    let spectator: Spectator<WrapperComponent>;
    let component: DotEditContentSidebarSectionComponent;

    const createComponent = createComponentFactory({
        component: WrapperComponent,
        imports: [DotEditContentSidebarSectionComponent]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component.sectionComponent;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
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

    it('should render projected content', () => {
        const contentSection = spectator.query(byTestId('dot-section-content'));
        const projectedContent = spectator.query(byTestId('projected-content'));

        expect(contentSection).toBeTruthy();
        expect(projectedContent).toBeTruthy();
        expect(projectedContent).toHaveText('Projected Content');
    });

    it('should render action section', fakeAsync(() => {
        tick();
        spectator.detectChanges();

        const actionSection = spectator.query(byTestId('dot-section-action'));
        const actionContent = spectator.query(byTestId('action-content'));

        expect(actionSection).toBeTruthy();
        expect(actionContent).toBeTruthy();
        expect(actionContent).toHaveText('Action Content');
    }));
});
