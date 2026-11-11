import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';

import { CdkMenuTrigger } from '@angular/cdk/menu';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DotMessageService, DotSystemConfigService } from '@dotcms/data-access';
import { DotMenu } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DOT_SYSTEM_CONFIG_SERVICE_MOCK, MockDotMessageService } from '@dotcms/utils-testing';

import { DotSubNavComponent } from './dot-sub-nav.component';

import { dotMenuMock } from '../../services/dot-navigation.service.spec';

const data: DotMenu = {
    ...dotMenuMock(),
    menuItems: [
        {
            ...dotMenuMock().menuItems[0]
        },
        {
            ...dotMenuMock().menuItems[1],
            active: true
        }
    ]
};

describe('DotSubNavComponent', () => {
    let spectator: Spectator<DotSubNavComponent>;

    const createComponent = createComponentFactory({
        component: DotSubNavComponent,
        detectChanges: false,
        imports: [RouterTestingModule, BrowserAnimationsModule],
        providers: [
            {
                provide: DotSystemConfigService,
                useValue: DOT_SYSTEM_CONFIG_SERVICE_MOCK
            },
            mockProvider(GlobalStore),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({ 'Identity-Access': 'Identity Access' })
            },
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('data', data);
    });

    it('should have two menu links when expanded', () => {
        spectator.setInput('collapsed', false);
        spectator.detectChanges();
        expect(spectator.debugElement.queryAll(By.css('.dot-nav-sub li')).length).toBe(2);
    });

    it('should have three list items when collapsed (group header + 2 menu items)', () => {
        spectator.setInput('collapsed', true);
        spectator.detectChanges();
        expect(spectator.debugElement.queryAll(By.css('.dot-nav-sub li')).length).toBe(3);
    });

    it('should NOT show group name when expanded', () => {
        spectator.setInput('collapsed', false);
        spectator.detectChanges();
        const groupName = spectator.debugElement.query(
            By.css('[data-testid="nav-sub-group-name"]')
        );
        expect(groupName).toBeNull();
    });

    it('should show group name when collapsed', () => {
        spectator.setInput('collapsed', true);
        spectator.detectChanges();
        const groupName = spectator.debugElement.query(
            By.css('[data-testid="nav-sub-group-name"]')
        );
        expect(groupName).not.toBeNull();
        expect(groupName.nativeElement.textContent.trim()).toBe(data.label);
    });

    it('should have group name element with proper styling when collapsed', () => {
        spectator.setInput('collapsed', true);
        spectator.detectChanges();
        const groupName = spectator.debugElement.query(
            By.css('[data-testid="nav-sub-group-name"]')
        );
        expect(groupName).not.toBeNull();
        expect(groupName.nativeElement.textContent.trim()).toBe(data.label);
        expect(groupName.nativeElement.tagName.toLowerCase()).toBe('span');
    });

    it('should set <li> correctly when expanded', () => {
        spectator.setInput('collapsed', false);
        spectator.detectChanges();
        const items = spectator.debugElement.queryAll(By.css('.dot-nav-sub li'));

        items.forEach((item) => {
            expect(item.nativeElement.classList.contains('dot-nav-sub__item')).toBe(true);
        });
    });

    it('should have group header when collapsed', () => {
        spectator.setInput('collapsed', true);
        spectator.detectChanges();
        const items = spectator.debugElement.queryAll(By.css('.dot-nav-sub li'));
        const groupHeader = items.find((item) =>
            item.nativeElement.classList.contains('dot-nav-sub__group-header')
        );
        expect(groupHeader).not.toBeNull();
    });

    it('should set <a> correctly', () => {
        spectator.setInput('collapsed', false);
        spectator.detectChanges();
        const links = spectator.debugElement.queryAll(By.css('.dot-nav-sub li a'));

        links.forEach((link, index) => {
            expect(link.nativeElement.classList.contains('dot-nav-sub__link')).toBe(true);
            expect(link.nativeElement.textContent.trim()).toBe(`Label ${index + 1}`);
            expect(link.properties['href']).toContain(`/url/${index === 0 ? 'one' : 'two'}`);

            if (index === 1) {
                expect(link.nativeElement.classList.contains('dot-nav-sub__link--active')).toBe(
                    true
                );
            }
        });
    });

    it('should emit event on link click', () => {
        spectator.setInput('collapsed', false);
        spectator.detectChanges();
        const link = spectator.debugElement.query(By.css('.dot-nav-sub li a'));

        spectator.component.itemClick.subscribe((event) => {
            expect(event).toEqual({
                originalEvent: { hello: 'world' } as unknown as MouseEvent,
                data: data.menuItems[0]
            });
        });

        link.triggerEventHandler('click', { hello: 'world' });
    });

    it('should NOT have collapsed class', () => {
        spectator.setInput('collapsed', false);
        spectator.detectChanges();
        expect(spectator.debugElement.query(By.css('.dot-nav-sub__collapsed'))).toBeNull();
    });

    describe('parent groups', () => {
        const users = { ...data.menuItems[0], initParams: { parent: 'Identity-Access' } };
        const roles = { ...data.menuItems[1], initParams: { parent: 'Identity-Access' } };
        const other = { ...data.menuItems[0], id: 'other', label: 'Other' };
        const parentSelector = byTestId('nav-sub-parent-Identity-Access');

        beforeEach(() => {
            vi.spyOn(spectator.inject(Router), 'navigateByUrl').mockResolvedValue(true);
            spectator.setInput('data', {
                ...data,
                isOpen: true,
                menuItems: [users, other, roles]
            });
        });

        it('groups non-adjacent siblings at the first child and translates the parent', () => {
            const parent = spectator.query<HTMLAnchorElement>(parentSelector);
            const link = spectator.query(byTestId('nav-sub-link-other'));
            expect(parent.textContent).toContain('Identity Access');
            expect(
                parent.compareDocumentPosition(link) & Node.DOCUMENT_POSITION_FOLLOWING
            ).toBeTruthy();
            expect(spectator.query(byTestId(`nav-sub-link-${users.id}`))).toBeNull();
            expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBeNull();
            expect(spectator.component.$data().menuItems).toEqual([users, other, roles]);
        });

        it('highlights a parent containing the active portlet', () => {
            expect(spectator.query(parentSelector)).toHaveClass('dot-nav-sub__link--active');
        });

        it('opens on hover and retains child order, routes and active state', () => {
            const parent = spectator.query<HTMLAnchorElement>(parentSelector);
            parent.dispatchEvent(new MouseEvent('mouseenter'));
            spectator.detectChanges();

            const flyout = spectator.query(byTestId('nav-sub-flyout'), { root: true });
            expect(parent.getAttribute('aria-expanded')).toBe('true');
            expect(parent.getAttribute('aria-controls')).toBe(flyout.id);
            expect(flyout.getAttribute('aria-label')).toBe('Identity Access');
            const links = spectator.queryAll<HTMLAnchorElement>(
                byTestId(`nav-sub-link-${users.id}`),
                { root: true }
            );
            const roleLink = spectator.query<HTMLAnchorElement>(
                byTestId(`nav-sub-link-${roles.id}`),
                { root: true }
            );
            expect(links).toHaveLength(1);
            expect(
                links[0].compareDocumentPosition(roleLink) & Node.DOCUMENT_POSITION_FOLLOWING
            ).toBeTruthy();
            expect(links[0].getAttribute('href')).toContain(users.menuLink);
            expect(roleLink.getAttribute('aria-current')).toBe('page');
        });

        it.each([false, true])(
            'does not toggle a hovered parent closed on click (collapsed: %s)',
            (collapsed) => {
                spectator.setInput('collapsed', collapsed);
                const parent = spectator.query(parentSelector);
                const emit = vi.spyOn(spectator.component.itemClick, 'emit');
                parent.dispatchEvent(new MouseEvent('mouseenter'));
                spectator.detectChanges();
                const flyout = spectator.query(byTestId('nav-sub-flyout'), { root: true });
                spectator.click(parent);
                spectator.click(parent);
                expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBe(flyout);
                expect(parent.getAttribute('aria-expanded')).toBe('true');
                expect(emit).toHaveBeenCalledTimes(2);
                expect(emit).toHaveBeenLastCalledWith({
                    originalEvent: expect.any(MouseEvent),
                    data: users
                });
            }
        );

        it.each([false, true])(
            'navigates to the first child with the layout state (collapsed: %s)',
            (collapsed) => {
                spectator.setInput('collapsed', collapsed);
                const emit = vi.spyOn(spectator.component.itemClick, 'emit');
                const navigate = vi.mocked(spectator.inject(Router).navigateByUrl);
                const parent = spectator.query<HTMLAnchorElement>(parentSelector);
                expect(parent.tagName).toBe('A');
                expect(parent.getAttribute('href')).toContain(users.menuLink);
                parent.dispatchEvent(new MouseEvent('mouseenter'));
                spectator.detectChanges();
                expect(navigate).not.toHaveBeenCalled();
                spectator.click(parent);
                expect(emit).toHaveBeenCalledWith({
                    originalEvent: expect.any(MouseEvent),
                    data: users
                });
                expect(navigate).toHaveBeenCalledTimes(1);
                expect(navigate.mock.calls[0][0].toString()).toContain(users.menuLink);
                expect(navigate.mock.calls[0][1]?.state).toEqual({ menuId: data.id });
                expect(parent.getAttribute('aria-expanded')).toBe('true');
            }
        );

        it('follows the current first child after reordering or permission filtering', () => {
            const emit = vi.spyOn(spectator.component.itemClick, 'emit');
            const navigate = vi.mocked(spectator.inject(Router).navigateByUrl);
            spectator.setInput('data', { ...data, isOpen: true, menuItems: [roles, other, users] });
            spectator.click(parentSelector);
            expect(emit).toHaveBeenLastCalledWith({
                originalEvent: expect.any(MouseEvent),
                data: roles
            });
            expect(navigate.mock.calls.at(-1)?.[0].toString()).toContain(roles.menuLink);
            spectator.setInput('data', { ...data, isOpen: true, menuItems: [users] });
            spectator.click(parentSelector);
            expect(emit).toHaveBeenLastCalledWith({
                originalEvent: expect.any(MouseEvent),
                data: users
            });
            expect(navigate.mock.calls.at(-1)?.[0].toString()).toContain(users.menuLink);
        });

        it.each([{ ctrlKey: true }, { metaKey: true }])(
            'preserves native modified clicks on parent links (modifiers: %j)',
            (modifiers) => {
                const event = new MouseEvent('click', {
                    bubbles: true,
                    cancelable: true,
                    ...modifiers
                });
                event.preventDefault();
                const parent = spectator.query<HTMLAnchorElement>(parentSelector);
                parent.dispatchEvent(event);
                spectator.detectChanges();
                expect(spectator.inject(Router).navigateByUrl).not.toHaveBeenCalled();
                expect(parent.getAttribute('href')).toContain(users.menuLink);
                expect(parent.getAttribute('aria-expanded')).toBe('true');
            }
        );

        it('opens a closed parent on click and leaves it open on subsequent clicks', () => {
            spectator.click(parentSelector);
            const flyout = spectator.query(byTestId('nav-sub-flyout'), { root: true });
            expect(flyout).not.toBeNull();
            spectator.click(parentSelector);
            expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBe(flyout);
        });

        it.each([{}, { ctrlKey: true }, { metaKey: true }])(
            'emits the original child without closing the flyout (modifiers: %j)',
            (modifiers) => {
                const emit = vi.spyOn(spectator.component.itemClick, 'emit');
                const navigate = vi.mocked(spectator.inject(Router).navigateByUrl);
                spectator.click(parentSelector);
                const flyout = spectator.query(byTestId('nav-sub-flyout'), { root: true });
                navigate.mockClear();
                emit.mockClear();
                const event = new MouseEvent('click', {
                    bubbles: true,
                    cancelable: true,
                    ...modifiers
                });
                event.preventDefault();
                spectator
                    .query(byTestId(`nav-sub-link-${users.id}`), { root: true })
                    .dispatchEvent(event);
                spectator.detectChanges();

                expect(emit).toHaveBeenCalledWith({ originalEvent: event, data: users });
                expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBe(flyout);
                if (event.ctrlKey || event.metaKey) {
                    expect(navigate).not.toHaveBeenCalled();
                } else {
                    expect(navigate).toHaveBeenCalled();
                }
            }
        );

        it('supports keyboard opening, Escape dismissal and focus restoration', () => {
            const parent = spectator.query<HTMLAnchorElement>(parentSelector);
            parent.focus();
            parent.dispatchEvent(
                new KeyboardEvent('keydown', {
                    key: 'ArrowRight',
                    keyCode: 39,
                    bubbles: true
                })
            );
            spectator.detectChanges();
            const firstLink = spectator.query<HTMLAnchorElement>(
                byTestId(`nav-sub-link-${users.id}`),
                { root: true }
            );
            expect(document.activeElement).toBe(firstLink);
            expect(spectator.inject(Router).navigateByUrl).not.toHaveBeenCalled();
            firstLink.dispatchEvent(
                new KeyboardEvent('keydown', {
                    key: 'Escape',
                    keyCode: 27,
                    bubbles: true
                })
            );
            spectator.detectChanges();

            expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBeNull();
            expect(document.activeElement).toBe(parent);
        });

        it('dismisses a hover-opened flyout with Escape without moving focus into it', () => {
            spectator.query(parentSelector).dispatchEvent(new MouseEvent('mouseenter'));
            spectator.detectChanges();
            document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
            spectator.detectChanges();
            expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBeNull();
        });

        describe('delayed pointer dismissal', () => {
            beforeEach(() => vi.useFakeTimers());
            afterEach(() => {
                spectator.fixture.destroy();
                vi.useRealTimers();
            });

            it.each([false, true])(
                'closes 300ms after leaving the parent (collapsed: %s)',
                (collapsed) => {
                    spectator.setInput('collapsed', collapsed);
                    const parent = spectator.query(parentSelector);
                    parent.dispatchEvent(new MouseEvent('mouseenter'));
                    spectator.detectChanges();
                    parent.dispatchEvent(new MouseEvent('mouseleave'));
                    vi.advanceTimersByTime(299);
                    spectator.detectChanges();
                    expect(
                        spectator.query(byTestId('nav-sub-flyout'), { root: true })
                    ).not.toBeNull();
                    vi.advanceTimersByTime(1);
                    spectator.detectChanges();
                    expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBeNull();
                    expect(spectator.element).not.toHaveClass('dot-nav-sub--flyout-open');
                }
            );

            it('allows crossing into the flyout and closes 300ms after leaving it', () => {
                const parent = spectator.query(parentSelector);
                parent.dispatchEvent(new MouseEvent('mouseenter'));
                spectator.detectChanges();
                const flyout = spectator.query(byTestId('nav-sub-flyout'), { root: true });
                parent.dispatchEvent(new MouseEvent('mouseleave'));
                vi.advanceTimersByTime(250);
                flyout.dispatchEvent(new MouseEvent('mouseenter'));
                vi.advanceTimersByTime(300);
                spectator.detectChanges();
                expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBe(flyout);
                flyout.dispatchEvent(new MouseEvent('mouseleave'));
                vi.advanceTimersByTime(299);
                spectator.detectChanges();
                expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBe(flyout);
                vi.advanceTimersByTime(1);
                spectator.detectChanges();
                expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBeNull();
            });

            it('cancels closure when returning from the flyout to its parent', () => {
                const parent = spectator.query(parentSelector);
                parent.dispatchEvent(new MouseEvent('mouseenter'));
                spectator.detectChanges();
                const flyout = spectator.query(byTestId('nav-sub-flyout'), { root: true });
                flyout.dispatchEvent(new MouseEvent('mouseleave'));
                vi.advanceTimersByTime(250);
                parent.dispatchEvent(new MouseEvent('mouseenter'));
                vi.advanceTimersByTime(300);
                spectator.detectChanges();
                expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBe(flyout);
            });

            it('does not let a previous parent timeout close a newly opened group', () => {
                spectator.setInput('data', {
                    ...data,
                    isOpen: true,
                    menuItems: [users, { ...roles, initParams: { parent: 'Other-parent' } }]
                });
                const parent = spectator.query(parentSelector);
                parent.dispatchEvent(new MouseEvent('mouseenter'));
                spectator.detectChanges();
                parent.dispatchEvent(new MouseEvent('mouseleave'));
                vi.advanceTimersByTime(250);
                const otherParent = spectator.query(byTestId('nav-sub-parent-Other-parent'));
                otherParent.dispatchEvent(new MouseEvent('mouseenter'));
                spectator.detectChanges();
                vi.advanceTimersByTime(300);
                spectator.detectChanges();
                expect(otherParent.getAttribute('aria-expanded')).toBe('true');
                expect(spectator.queryAll(byTestId('nav-sub-flyout'), { root: true })).toHaveLength(
                    1
                );
            });

            it('clears the pending close when destroyed', () => {
                const parent = spectator.query(parentSelector);
                parent.dispatchEvent(new MouseEvent('mouseenter'));
                spectator.detectChanges();
                const close = vi.spyOn(spectator.query(CdkMenuTrigger), 'close');
                parent.dispatchEvent(new MouseEvent('mouseleave'));
                spectator.fixture.destroy();
                vi.advanceTimersByTime(300);
                expect(close).not.toHaveBeenCalled();
            });
        });

        it('dismisses on outside click', () => {
            spectator.click(parentSelector);
            document.body.dispatchEvent(new MouseEvent('click', { bubbles: true }));
            spectator.detectChanges();
            expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBeNull();
        });

        it('also opens in the collapsed sidebar', () => {
            spectator.setInput('collapsed', true);
            spectator.click(parentSelector);
            expect(spectator.query(byTestId('nav-sub-group-name')).textContent.trim()).toBe(
                data.label
            );
            expect(spectator.element).toHaveClass('dot-nav-sub--flyout-open');
            expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).not.toBeNull();
        });

        it('does not invent absent children and keeps single-child groups', () => {
            spectator.setInput('data', { ...data, isOpen: true, menuItems: [users] });
            spectator.click(parentSelector);
            expect(
                spectator.query(byTestId(`nav-sub-link-${users.id}`), { root: true })
            ).not.toBeNull();
            expect(
                spectator.query(byTestId(`nav-sub-link-${roles.id}`), { root: true })
            ).toBeNull();
        });

        it('removes unavailable children when an open menu is refreshed', () => {
            spectator.click(parentSelector);
            spectator.setInput('data', { ...data, isOpen: true, menuItems: [users] });
            expect(
                spectator.query(byTestId(`nav-sub-link-${users.id}`), { root: true })
            ).not.toBeNull();
            expect(
                spectator.query(byTestId(`nav-sub-link-${roles.id}`), { root: true })
            ).toBeNull();
        });

        it('treats blank parents as ungrouped portlets', () => {
            spectator.setInput('data', {
                ...data,
                isOpen: true,
                menuItems: [{ ...users, initParams: { parent: '   ' } }]
            });
            expect(spectator.query(parentSelector)).toBeNull();
            expect(spectator.query(byTestId(`nav-sub-link-${users.id}`))).not.toBeNull();
        });

        it('uses the key when no translation exists', () => {
            spectator.setInput('data', {
                ...data,
                isOpen: true,
                menuItems: [{ ...users, initParams: { parent: 'Custom-parent' } }]
            });
            expect(spectator.query(byTestId('nav-sub-parent-Custom-parent')).textContent).toContain(
                'Custom-parent'
            );
        });

        it('keeps the same parent key separate across layouts', () => {
            const secondLayout = createComponent();
            secondLayout.setInput('data', {
                ...data,
                id: 'another-layout',
                isOpen: true,
                menuItems: [roles]
            });
            secondLayout.click(parentSelector);
            expect(
                spectator.query(byTestId(`nav-sub-link-${users.id}`), { root: true })
            ).toBeNull();
            expect(
                spectator.query(byTestId(`nav-sub-link-${roles.id}`), { root: true })
            ).not.toBeNull();
        });

        it('closes the previous group when another parent is hovered', () => {
            spectator.setInput('data', {
                ...data,
                isOpen: true,
                menuItems: [users, { ...roles, initParams: { parent: 'Other-parent' } }]
            });
            spectator.query(parentSelector).dispatchEvent(new MouseEvent('mouseenter'));
            spectator.detectChanges();
            spectator
                .query(byTestId('nav-sub-parent-Other-parent'))
                .dispatchEvent(new MouseEvent('mouseenter'));
            spectator.detectChanges();
            expect(spectator.queryAll(byTestId('nav-sub-flyout'), { root: true })).toHaveLength(1);
            expect(spectator.query(parentSelector).getAttribute('aria-expanded')).toBe('false');
            expect(spectator.element).toHaveClass('dot-nav-sub--flyout-open');
        });

        it('does not retain an open parent when the menu becomes empty', () => {
            spectator.click(parentSelector);
            spectator.setInput('data', { ...data, isOpen: true, menuItems: [] });
            expect(spectator.query(parentSelector)).toBeNull();
            expect(spectator.query(byTestId('nav-sub-flyout'), { root: true })).toBeNull();
            expect(spectator.element).not.toHaveClass('dot-nav-sub--flyout-open');
        });
    });

    describe('dot-sub-nav', () => {
        describe('is Open', () => {
            beforeEach(() => {
                spectator.setInput('data', { ...data, isOpen: true });
            });

            describe('menu collapsed', () => {
                beforeEach(() => {
                    spectator.setInput('collapsed', true);
                    spectator.detectChanges();
                });

                it('should set expandAnimation collapsed', () => {
                    expect(spectator.component.getAnimation).toEqual('collapsed');
                });

                it('should have collapsed class', () => {
                    const el = spectator.debugElement.query(By.css('.dot-nav-sub__collapsed'));
                    expect(el).not.toBeNull();
                });

                it('should show group name when collapsed', () => {
                    const groupName = spectator.debugElement.query(
                        By.css('.dot-nav-sub__group-name')
                    );
                    expect(groupName).not.toBeNull();
                    expect(groupName.nativeElement.textContent.trim()).toBe(data.label);
                });
            });

            describe('menu expanded', () => {
                beforeEach(() => {
                    spectator.setInput('collapsed', false);
                    spectator.detectChanges();
                });

                it('should set expandAnimation expanded', () => {
                    expect(spectator.component.getAnimation).toEqual('expanded');
                });
            });
        });

        describe('is Close', () => {
            beforeEach(() => {
                spectator.setInput('data', { ...data, isOpen: false });
            });

            describe('menu collapsed', () => {
                beforeEach(() => {
                    spectator.setInput('collapsed', true);
                    spectator.detectChanges();
                });

                it('should set expandAnimation collapsed', () => {
                    expect(spectator.component.getAnimation).toEqual('collapsed');
                });
            });

            describe('menu expanded', () => {
                beforeEach(() => {
                    spectator.setInput('collapsed', false);
                    spectator.detectChanges();
                });

                it('should set expandAnimation expanded', () => {
                    expect(spectator.component.getAnimation).toEqual('collapsed');
                });
            });
        });
    });
});
