import { describe, expect } from '@jest/globals';
import { byTestId, byText, createRoutingFactory, SpectatorRouting } from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { EditEmaNavigationBarComponent } from './edit-ema-navigation-bar.component';

const messages = {
    'editema.editor.navbar.content': 'Content',
    'editema.editor.navbar.layout': 'Layout',
    'editema.editor.navbar.rules': 'Rules',
    'editema.editor.navbar.experiments': 'Experiments',
    'editema.editor.navbar.action': 'Action'
};

const messageServiceMock = new MockDotMessageService(messages);

describe('EditEmaNavigationBarComponent', () => {
    let spectator: SpectatorRouting<EditEmaNavigationBarComponent>;

    const createComponent = createRoutingFactory({
        component: EditEmaNavigationBarComponent,
        stubsEnabled: false,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
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
                        label: 'editema.editor.navbar.content',
                        id: 'content',
                        href: 'content'
                    },
                    {
                        icon: 'pi-table',
                        label: 'editema.editor.navbar.layout',
                        href: 'layout',
                        id: 'layout',
                        isDisabled: true
                    },
                    {
                        icon: 'pi-sliders-h',
                        label: 'editema.editor.navbar.rules',
                        href: 'rules',
                        id: 'rules'
                    },
                    {
                        iconURL: 'assets/images/experiments.svg',
                        label: 'editema.editor.navbar.experiments',
                        href: 'experiments',
                        id: 'experiments'
                    },
                    {
                        icon: 'pi-sliders-h',
                        label: 'editema.editor.navbar.action',
                        id: 'action'
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
                expect(links[2].getAttribute('ng-reflect-router-link')).toBe('rules');
                expect(links[3].getAttribute('ng-reflect-router-link')).toBe('experiments');
                expect(links[4].getAttribute('ng-reflect-router-link')).toBeNull();
            });

            it("should be a button if action is defined on last item 'Action'", () => {
                const actionLink = spectator.query('button[data-testId="nav-bar-item"]');

                expect(actionLink).not.toBeNull();
            });

            it("should emit action on clicking last item 'Action'", () => {
                const actionLink = spectator.query(byText('Action'));
                const mockedAction = jest.spyOn(spectator.component.action, 'emit');

                spectator.click(actionLink);

                expect(mockedAction).toHaveBeenCalledWith('action');
            });

            describe('NavBar with disabled', () => {
                it('should render disabled item without router link', () => {
                    const links = spectator.queryAll(byTestId('nav-bar-item'));
                    expect(links[1].textContent.trim()).toBe('Layout');
                    expect(links[1].getAttribute('ng-reflect-router-link')).toBeNull();
                });
            });
        });

        describe('item', () => {
            it('should have Content as selected', () => {
                const contentButton = spectator.query(byTestId('nav-bar-item'));

                spectator.click(contentButton);

                expect(spectator.query(byTestId('nav-bar-item')).classList).toContain(
                    'edit-ema-nav-bar__item--active'
                );
            });

            it('should have Layout as disabled', () => {
                const links = spectator.queryAll(byTestId('nav-bar-item'));
                expect(links[1].classList).toContain('edit-ema-nav-bar__item--disabled');
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
