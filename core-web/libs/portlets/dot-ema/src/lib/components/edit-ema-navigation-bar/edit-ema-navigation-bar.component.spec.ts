import { describe, expect } from '@jest/globals';
import { byTestId, createRoutingFactory, SpectatorRouting } from '@ngneat/spectator/jest';

import { EditEmaNavigationBarComponent } from './edit-ema-navigation-bar.component';

describe('EditEmaNavigationBarComponent', () => {
    let spectator: SpectatorRouting<EditEmaNavigationBarComponent>;

    const createComponent = createRoutingFactory({
        component: EditEmaNavigationBarComponent,
        stubsEnabled: false,
        routes: [
            {
                path: '',
                component: EditEmaNavigationBarComponent
            },
            {
                path: 'content',
                component: EditEmaNavigationBarComponent
            },
            {
                path: 'layout',
                component: EditEmaNavigationBarComponent
            },
            {
                path: 'rules',
                component: EditEmaNavigationBarComponent
            },
            {
                path: 'experiments',
                component: EditEmaNavigationBarComponent
            },
            {
                path: '**',
                redirectTo: 'content',
                pathMatch: 'full'
            },
            {
                path: '',
                redirectTo: 'content',
                pathMatch: 'full'
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                items: [
                    {
                        icon: 'pi-file',
                        label: 'Content',
                        href: 'content'
                    },
                    {
                        icon: 'pi-table',
                        label: 'Layout',
                        href: 'layout'
                    },
                    {
                        icon: 'pi-sliders-h',
                        label: 'Rules',
                        href: 'rules'
                    },
                    {
                        iconURLActive: 'assets/images/experiments-active.svg',
                        iconURL: 'assets/images/experiments.svg',
                        label: 'Experiments',
                        href: 'experiments'
                    }
                ]
            }
        });
    });

    describe('DOM', () => {
        describe('Nav Bar', () => {
            it('should have 4 items', () => {
                expect(spectator.queryAll('a').length).toBe(4);
            });
        });

        describe('item', () => {
            it('should have Content as selected', async () => {
                await spectator.fixture.whenStable();
                const otherButton = spectator.queryAll(byTestId('nav-bar-item'))[0];

                spectator.click(otherButton);

                await spectator.fixture.whenStable();

                expect(spectator.query(byTestId('nav-bar-item-active')).textContent.trim()).toBe(
                    'Content'
                );
            });

            it('should change the active one when clicking on it', async () => {
                await spectator.fixture.whenStable();
                const otherButton = spectator.queryAll(byTestId('nav-bar-item'))[1];

                spectator.click(otherButton);

                await spectator.fixture.whenStable();

                expect(spectator.query(byTestId('nav-bar-item-active')).textContent.trim()).toBe(
                    'Layout'
                );
            });

            describe('item with icon', () => {
                it('should have an icon', () => {
                    expect(spectator.query(byTestId('nav-bar-item-icon'))).not.toBeNull();
                });
            });

            describe('item without icon', () => {
                it("should have an image with src 'assets/images/experiments.svg'", () => {
                    expect(
                        spectator.query(byTestId('nav-bar-item-image')).getAttribute('src')
                    ).toBe('assets/images/experiments.svg');
                });

                it("should switch the image to 'assets/images/experiments-active.svg' when selected", async () => {
                    const otherButton = spectator.queryAll(byTestId('nav-bar-item')).at(-1);

                    spectator.click(otherButton);
                    await spectator.fixture.whenStable();
                    expect(
                        spectator.query(byTestId('nav-bar-item-image')).getAttribute('src')
                    ).toBe('assets/images/experiments-active.svg');
                });
            });
        });
    });
});
