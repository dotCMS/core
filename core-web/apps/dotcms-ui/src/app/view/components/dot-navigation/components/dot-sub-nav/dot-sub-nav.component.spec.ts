import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { DotSystemConfigService } from '@dotcms/data-access';
import { DotMenu } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

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
                useValue: { getSystemConfig: () => ({ of: jest.fn() }) }
            },
            GlobalStore,
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.component.data = data;
    });

    it('should have two menu links when expanded', () => {
        spectator.component.collapsed = false;
        spectator.detectChanges();
        expect(spectator.debugElement.queryAll(By.css('.dot-nav-sub li')).length).toBe(2);
    });

    it('should have three list items when collapsed (group header + 2 menu items)', () => {
        spectator.component.collapsed = true;
        spectator.detectChanges();
        expect(spectator.debugElement.queryAll(By.css('.dot-nav-sub li')).length).toBe(3);
    });

    it('should NOT show group name when expanded', () => {
        spectator.component.collapsed = false;
        spectator.detectChanges();
        const groupName = spectator.debugElement.query(
            By.css('[data-testid="nav-sub-group-name"]')
        );
        expect(groupName).toBeNull();
    });

    it('should show group name when collapsed', () => {
        spectator.component.collapsed = true;
        spectator.detectChanges();
        const groupName = spectator.debugElement.query(
            By.css('[data-testid="nav-sub-group-name"]')
        );
        expect(groupName).not.toBeNull();
        expect(groupName.nativeElement.textContent.trim()).toBe(data.label);
    });

    it('should have group name element with proper styling when collapsed', () => {
        spectator.component.collapsed = true;
        spectator.detectChanges();
        const groupName = spectator.debugElement.query(
            By.css('[data-testid="nav-sub-group-name"]')
        );
        expect(groupName).not.toBeNull();
        expect(groupName.nativeElement.textContent.trim()).toBe(data.label);
        expect(groupName.nativeElement.tagName.toLowerCase()).toBe('span');
    });

    it('should set <li> correctly when expanded', () => {
        spectator.component.collapsed = false;
        spectator.detectChanges();
        const items = spectator.debugElement.queryAll(By.css('.dot-nav-sub li'));

        items.forEach((item) => {
            expect(item.nativeElement.classList.contains('dot-nav-sub__item')).toBe(true);
        });
    });

    it('should have group header when collapsed', () => {
        spectator.component.collapsed = true;
        spectator.detectChanges();
        const items = spectator.debugElement.queryAll(By.css('.dot-nav-sub li'));
        const groupHeader = items.find((item) =>
            item.nativeElement.classList.contains('dot-nav-sub__group-header')
        );
        expect(groupHeader).not.toBeNull();
    });

    it('should set <a> correctly', () => {
        spectator.component.collapsed = false;
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
        spectator.component.collapsed = false;
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
        spectator.component.collapsed = false;
        spectator.detectChanges();
        expect(spectator.debugElement.query(By.css('.dot-nav-sub__collapsed'))).toBeNull();
    });

    describe('dot-sub-nav', () => {
        describe('is Open', () => {
            beforeEach(() => {
                spectator.component.data = { ...data, isOpen: true };
            });

            describe('menu collapsed', () => {
                beforeEach(() => {
                    spectator.component.collapsed = true;
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
                    spectator.component.collapsed = false;
                    spectator.detectChanges();
                });

                it('should set expandAnimation expanded', () => {
                    expect(spectator.component.getAnimation).toEqual('expanded');
                });
            });
        });

        describe('is Close', () => {
            beforeEach(() => {
                spectator.component.data = { ...data, isOpen: false };
            });

            describe('menu collapsed', () => {
                beforeEach(() => {
                    spectator.component.collapsed = true;
                    spectator.detectChanges();
                });

                it('should set expandAnimation collapsed', () => {
                    expect(spectator.component.getAnimation).toEqual('collapsed');
                });
            });

            describe('menu expanded', () => {
                beforeEach(() => {
                    spectator.component.collapsed = false;
                    spectator.detectChanges();
                });

                it('should set expandAnimation expanded', () => {
                    expect(spectator.component.getAnimation).toEqual('collapsed');
                });
            });
        });
    });
});
