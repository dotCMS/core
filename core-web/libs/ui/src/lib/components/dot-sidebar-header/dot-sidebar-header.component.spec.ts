import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotSidebarHeaderComponent } from './dot-sidebar-header.component';

describe('DotSidebarHeaderComponent', () => {
    let spectator: Spectator<DotSidebarHeaderComponent>;
    const createComponent = createComponentFactory({
        component: DotSidebarHeaderComponent
    });

    it('should title', async () => {
        const title = 'My title';
        spectator = createComponent({
            detectChanges: false
        });

        spectator.setInput('dotTitle', title);
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        const titleElement = spectator.query(byTestId('header-title'));
        console.log(titleElement);
        expect(titleElement).toHaveText(title);
    });

    it('should close icon', () => {
        spectator.detectChanges();
        expect(spectator.query(byTestId('header-close-icon'))).toExist();
    });
});
