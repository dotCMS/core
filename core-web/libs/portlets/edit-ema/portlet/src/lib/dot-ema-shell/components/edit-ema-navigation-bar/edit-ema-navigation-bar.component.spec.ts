import { describe, expect } from '@jest/globals';
import { byTestId, byText, createRoutingFactory, SpectatorRouting } from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';

import { EditEmaNavigationBarComponent } from './edit-ema-navigation-bar.component';

describe('EditEmaNavigationBarComponent', () => {
    let spectator: SpectatorRouting<EditEmaNavigationBarComponent>;
    const mockedAction = jest.fn();

    const createComponent = createRoutingFactory({
        component: EditEmaNavigationBarComponent,
        stubsEnabled: false,
        routes: [
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
                        iconURL: 'assets/images/experiments.svg',
                        label: 'Experiments',
                        href: 'experiments'
                    },
                    {
                        icon: 'pi-sliders-h',
                        label: 'Action',
                        action: mockedAction
                    }
                ]
            }
        });
    });

    describe('DOM', () => {
        describe('Nav Bar', () => {
            it('should have 5 items', () => {
                const links = spectator.queryAll(byTestId('nav-bar-item'));

                expect(links.length).toBe(5);
                expect(links[0].textContent.trim()).toBe('Content');
                expect(links[1].textContent.trim()).toBe('Layout');
                expect(links[2].textContent.trim()).toBe('Rules');
                expect(links[3].textContent.trim()).toBe('Experiments');
                expect(links[4].textContent.trim()).toBe('Action');

                expect(links[0].getAttribute('ng-reflect-router-link')).toBe('content');
                expect(links[1].getAttribute('ng-reflect-router-link')).toBe('layout');
                expect(links[2].getAttribute('ng-reflect-router-link')).toBe('rules');
                expect(links[3].getAttribute('ng-reflect-router-link')).toBe('experiments');
                expect(links[4].getAttribute('ng-reflect-router-link')).toBeNull();
            });

            it("should be a button if action is defined on last item 'Action'", () => {
                const actionLink = spectator.query('button[data-testId="nav-bar-item"]');

                expect(actionLink).not.toBeNull();
            });

            it("should trigger mockedAction on clicking last item 'Action'", () => {
                const actionLink = spectator.query(byText('Action'));

                spectator.click(actionLink);

                expect(mockedAction).toHaveBeenCalled();
            });
        });

        describe('item', () => {
            it('should have Content as selected', () => {
                const contentButton = spectator.query(byTestId('nav-bar-item'));

                spectator.click(contentButton);

                expect(spectator.query(byTestId('nav-bar-item')).classList[1]).toBe(
                    'edit-ema-nav-bar__item--active'
                );
            });

            it('should have an icon', () => {
                expect(spectator.query(byTestId('nav-bar-item-icon'))).not.toBeNull();
            });

            it('should have a label', () => {
                expect(spectator.query(byTestId('nav-bar-item-label'))).not.toBeNull();
            });

            describe('item without icon', () => {
                it("should have an image with href 'assets/images/experiments.svg'", () => {
                    const image = spectator.debugElement.query(
                        By.css('[data-testId="nav-bar-item-image"]')
                    );

                    expect(image.nativeElement.getAttribute('href')).toBe(
                        './assets/edit-ema/assets/images/experiments.svg.svg#assets/images/experiments.svg'
                    );
                });
            });
        });
    });
});
