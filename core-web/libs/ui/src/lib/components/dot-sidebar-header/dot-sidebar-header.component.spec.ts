import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { Sidebar } from 'primeng/sidebar';

import { DotSidebarHeaderComponent } from './dot-sidebar-header.component';

describe('DotSidebarHeaderComponent', () => {
    let spectator: Spectator<DotSidebarHeaderComponent>;
    const createComponent = createComponentFactory({
        component: DotSidebarHeaderComponent,
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
        expect(spectator.query(byTestId('header-close-icon'))).toExist();
    });
});
