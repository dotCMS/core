import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Drawer } from 'primeng/drawer';

import { DotSidebarHeaderComponent } from './dot-sidebar-header.component';

describe('DotSidebarHeaderComponent', () => {
    let spectator: Spectator<DotSidebarHeaderComponent>;

    const createComponent = createComponentFactory({
        component: DotSidebarHeaderComponent,
        providers: [
            {
                provide: Drawer,
                useValue: {
                    hide: jest.fn()
                }
            }
        ]
    });

    it('should show title', () => {
        const title = 'My title';
        spectator = createComponent({
            props: {
                dotTitle: title
            }
        });

        spectator.detectChanges();

        const titleElement = spectator.query(byTestId('header-title'));
        expect(titleElement?.textContent?.trim()).toBe(title);
    });

    it('should close icon', () => {
        spectator = createComponent();
        expect(spectator.query(byTestId('header-close-icon'))).toExist();
    });
});
