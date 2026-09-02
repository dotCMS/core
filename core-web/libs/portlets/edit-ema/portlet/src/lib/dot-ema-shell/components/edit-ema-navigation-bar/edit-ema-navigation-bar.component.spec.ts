import { describe, expect } from '@jest/globals';
import { byTestId, createRoutingFactory, SpectatorRouting } from '@openng/spectator/jest';

import { Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { EditEmaNavigationBarComponent } from './edit-ema-navigation-bar.component';

import { UVEStore } from '../../../store/dot-uve.store';

const messages = {
    'editema.editor.navbar.content': 'Content',
    'editema.editor.navbar.layout': 'Layout',
    'editema.editor.navbar.rules': 'Rules',
    'editema.editor.navbar.experiments': 'Experiments',
    'editema.editor.navbar.action': 'Action',
    'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template':
        'Cannot edit advanced template'
};

const store = {
    paletteOpen: () => false,
    setPaletteOpen: jest.fn(),
    pageParams: () => ({
        language_id: '3',
        personaId: '123'
    })
};

const messageServiceMock = new MockDotMessageService(messages);

const MOCK_ITEMS = [
    {
        materialIcon: 'description',
        label: 'editema.editor.navbar.content',
        id: 'content',
        href: 'content'
    },
    {
        materialIcon: 'space_dashboard',
        label: 'editema.editor.navbar.layout',
        href: 'layout',
        id: 'layout',
        isDisabled: true,
        tooltip: 'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template'
    },
    {
        materialIcon: 'rule',
        label: 'editema.editor.navbar.rules',
        href: 'rules',
        id: 'rules'
    },
    {
        materialIcon: 'call_split',
        label: 'editema.editor.navbar.experiments',
        href: 'experiments',
        id: 'experiments'
    },
    {
        materialIcon: 'bar_chart',
        label: 'editema.editor.navbar.action',
        id: 'action'
    }
];

describe('EditEmaNavigationBarComponent', () => {
    let spectator: SpectatorRouting<EditEmaNavigationBarComponent>;

    const createComponent = createRoutingFactory({
        component: EditEmaNavigationBarComponent,
        stubsEnabled: false,
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: UVEStore, useValue: store }
        ],
        routes: [
            { path: 'content', component: EditEmaNavigationBarComponent },
            { path: 'layout', component: EditEmaNavigationBarComponent },
            { path: 'rules', component: EditEmaNavigationBarComponent },
            { path: 'experiments', component: EditEmaNavigationBarComponent }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ props: { items: MOCK_ITEMS } });
    });

    describe('DOM', () => {
        describe('Nav Bar', () => {
            it('should render 5 items', () => {
                const buttons = spectator.queryAll(byTestId('nav-bar-item'));
                expect(buttons.length).toBe(5);
            });

            it('should have a <nav> with aria-label "Page editor navigation"', () => {
                const nav = spectator.query('nav');
                expect(nav.getAttribute('aria-label')).toBe('Page editor navigation');
            });
        });

        describe('Navigation', () => {
            it('should navigate with query params when clicking a link item', () => {
                const router = spectator.inject(Router);
                const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);

                const contentHost = spectator.queryAll(byTestId('nav-bar-item'))[0];
                const innerButton = contentHost.querySelector('button');
                spectator.click(innerButton);

                expect(navigateSpy).toHaveBeenCalledWith(
                    ['edit-page', 'content'],
                    expect.objectContaining({ queryParams: store.pageParams() })
                );
            });

            /**
             * The switch-off guarantee for the Experiments entry point (#37005, US2, FR-016).
             *
             * `navigate()` prefixes `edit-page` because every item it has ever had is a child of
             * that route. The switch-on destination is `/experiments`, which is not — so the
             * prefix has to become conditional. The rule chosen is the smallest one that cannot
             * disturb an existing item: a leading `/` means absolute.
             *
             * FR-016 and FR-019 then hold **by construction** rather than by inspection, because
             * none of `content`, `layout`, `rules/{id}` or `experiments/{id}` starts with `/`.
             * That is what these two tests pin.
             */
            describe('absolute vs relative hrefs', () => {
                const clickItemAt = (index: number) => {
                    const host = spectator.queryAll(byTestId('nav-bar-item'))[index];
                    spectator.click(host.querySelector('button'));
                };

                it.each([
                    [0, 'content'],
                    [2, 'rules'],
                    [3, 'experiments']
                ])(
                    'should keep the edit-page prefix for the relative item at %i (%s)',
                    (index, segment) => {
                        const navigateSpy = jest
                            .spyOn(spectator.inject(Router), 'navigate')
                            .mockResolvedValue(true);

                        clickItemAt(index);

                        expect(navigateSpy).toHaveBeenCalledWith(
                            ['edit-page', segment],
                            expect.objectContaining({ queryParams: store.pageParams() })
                        );
                    }
                );

                it('should not prefix an href that is already absolute', () => {
                    const navigateSpy = jest
                        .spyOn(spectator.inject(Router), 'navigate')
                        .mockResolvedValue(true);

                    spectator.setInput('items', [
                        {
                            materialIcon: 'science',
                            label: 'editema.editor.navbar.experiments',
                            href: '/experiments',
                            id: 'experiments'
                        }
                    ]);
                    spectator.detectChanges();

                    clickItemAt(0);

                    const [commands] = navigateSpy.mock.calls[0];
                    expect(commands).toEqual(['', 'experiments']);
                    expect(commands).not.toContain('edit-page');
                });
            });

            it('should not navigate when item is disabled', () => {
                const router = spectator.inject(Router);
                const navigateSpy = jest.spyOn(router, 'navigate');

                const layoutHost = spectator.queryAll(byTestId('nav-bar-item'))[1];
                const innerButton = layoutHost.querySelector('button');
                spectator.click(innerButton);

                expect(navigateSpy).not.toHaveBeenCalled();
            });

            it('should emit action when clicking an action item (no href)', () => {
                const emitSpy = jest.spyOn(spectator.component.action, 'emit');

                const actionHost = spectator.queryAll(byTestId('nav-bar-item'))[4];
                const innerButton = actionHost.querySelector('button');
                spectator.click(innerButton);

                expect(emitSpy).toHaveBeenCalledWith('action');
            });
        });

        describe('Active state', () => {
            it('should set opacity 1 on the indicator span of the active item after navigation', async () => {
                await spectator.router.navigate(['content']);
                await spectator.fixture.whenStable();
                spectator.detectChanges();

                const contentHost = spectator.queryAll(byTestId('nav-bar-item'))[0];
                const indicator = contentHost.parentElement.querySelector('span');
                expect(indicator.style.opacity).toBe('1');
            });

            it('should set opacity 0 on the indicator span of inactive items', () => {
                const rulesHost = spectator.queryAll(byTestId('nav-bar-item'))[2];
                const indicator = rulesHost.parentElement.querySelector('span');
                expect(indicator.style.opacity).toBe('0');
            });

            it('should set opacity 0 on the indicator span of action items (no href)', () => {
                const actionHost = spectator.queryAll(byTestId('nav-bar-item'))[4];
                const indicator = actionHost.parentElement.querySelector('span');
                expect(indicator.style.opacity).toBe('0');
            });
        });

        describe('Disabled state', () => {
            it('should mark disabled items with aria-disabled="true"', () => {
                const buttons = spectator.queryAll(byTestId('nav-bar-item'));
                expect(buttons[1].getAttribute('aria-disabled')).toBe('true');
            });

            it('should have disabled attribute on the inner button of a disabled item', () => {
                const layoutHost = spectator.queryAll(byTestId('nav-bar-item'))[1];
                const innerButton = layoutHost.querySelector('button') as HTMLButtonElement;
                expect(innerButton.disabled).toBe(true);
            });
        });

        describe('Icons', () => {
            it('should render material icon for each item', () => {
                const icons = spectator.queryAll(byTestId('nav-bar-item-icon'));
                expect(icons.length).toBe(5);
                expect(icons[0].textContent.trim()).toBe('description');
                expect(icons[1].textContent.trim()).toBe('space_dashboard');
                expect(icons[2].textContent.trim()).toBe('rule');
                expect(icons[3].textContent.trim()).toBe('call_split');
                expect(icons[4].textContent.trim()).toBe('bar_chart');
            });
        });

        describe('Accessibility', () => {
            it('should have aria-label on all interactive elements', () => {
                const items = spectator.queryAll(byTestId('nav-bar-item'));
                items.forEach((item) => {
                    expect(item.getAttribute('aria-label')).toBeTruthy();
                });
            });

            it('should show translated label as aria-label', () => {
                const buttons = spectator.queryAll(byTestId('nav-bar-item'));
                expect(buttons[0].getAttribute('aria-label')).toBe('Content');
                expect(buttons[2].getAttribute('aria-label')).toBe('Rules');
            });

            it('should show item label as aria-label for disabled items', () => {
                const layoutButton = spectator.queryAll(byTestId('nav-bar-item'))[1];
                expect(layoutButton.getAttribute('aria-label')).toBe('Layout');
            });
        });
    });
});
