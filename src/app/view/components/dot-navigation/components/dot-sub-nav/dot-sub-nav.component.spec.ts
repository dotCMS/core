import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotSubNavComponent } from './dot-sub-nav.component';
import { dotMenuMock } from '../../services/dot-navigation.service.spec';
import { DotMenu } from '@models/navigation';

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
    let component: DotSubNavComponent;
    let fixture: ComponentFixture<DotSubNavComponent>;
    let de: DebugElement;

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                declarations: [DotSubNavComponent],
                imports: [RouterTestingModule, BrowserAnimationsModule]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotSubNavComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
        component.data = data;
        fixture.detectChanges();
    });

    it('should have two menu links', () => {
        expect(de.queryAll(By.css('.dot-nav-sub li')).length).toBe(2);
    });

    it('should set <li> correctly', () => {
        const items: DebugElement[] = de.queryAll(By.css('.dot-nav-sub li'));

        items.forEach((item: DebugElement) => {
            expect(item.nativeElement.classList.contains('dot-nav-sub__item')).toBe(true);
        });
    });

    it('should set <a> correctly', () => {
        const links: DebugElement[] = de.queryAll(By.css('.dot-nav-sub li a'));

        links.forEach((link: DebugElement, index) => {
            expect(link.nativeElement.classList.contains('dot-nav-sub__link')).toBe(true);
            expect(link.nativeElement.textContent.trim()).toBe(`Label ${index + 1}`);
            expect(link.properties.href).toBe(`/url/link${index + 1}`);

            if (index === 1) {
                expect(link.nativeElement.classList.contains('dot-nav-sub__link--active')).toBe(
                    true
                );
            }
        });
    });

    it('should emit event on link click', () => {
        const link: DebugElement = de.query(By.css('.dot-nav-sub li a'));

        component.itemClick.subscribe(event => {
            expect(event).toEqual({
                originalEvent: { hello: 'world' },
                data: data.menuItems[0]
            });
        });

        link.triggerEventHandler('click', { hello: 'world' });
    });

    it('should set contextmenu class when is true', () => {
        const list: DebugElement = de.query(By.css('.dot-nav-sub'));
        expect(list.nativeElement.classList.contains('contextmenu')).toBe(false);
        component.contextmenu = true;
        fixture.detectChanges();
        expect(list.nativeElement.classList.contains('contextmenu')).toBe(true);
    });
});
