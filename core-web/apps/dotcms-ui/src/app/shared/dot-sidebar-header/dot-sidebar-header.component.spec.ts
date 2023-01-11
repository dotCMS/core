import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { Sidebar } from 'primeng/sidebar';

import { UiDotIconButtonComponent } from '@components/_common/dot-icon-button/dot-icon-button.component';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

import { DotSidebarHeaderComponent } from './dot-sidebar-header.component';

describe('DotSidebarHeaderComponent', () => {
    let spectator: Spectator<DotSidebarHeaderComponent>;
    let uiDotIconButtonComponent: UiDotIconButtonComponent;
    const createComponent = createComponentFactory({
        component: DotSidebarHeaderComponent,
        imports: [UiDotIconButtonModule],
        componentMocks: [Sidebar]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should title', () => {
        const title = 'My title';
        spectator.setInput('dotTitle', title);
        expect(spectator.query(byTestId('header-title'))).toContainText(title);
    });
    it('should close icon', () => {
        uiDotIconButtonComponent = spectator.query(UiDotIconButtonComponent);
        expect(uiDotIconButtonComponent).toExist();
        expect(uiDotIconButtonComponent.icon).toBe('close');
    });
});
