/* tslint:disable:no-unused-variable */
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { DotNavItemComponent } from './dot-nav-item.component';
import { DotNavIconModule } from '../dot-nav-icon/dot-nav-icon.module';
import { DotIconModule } from '../../../_common/dot-icon/dot-icon.module';
import { DotSubNavComponent } from '../dot-sub-nav/dot-sub-nav.component';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { dotMenuMock } from '../../dot-navigation.component.spec';


describe('DotNavItemComponent', () => {
    let component: DotNavItemComponent;
    let fixture: ComponentFixture<DotNavItemComponent>;
    let de: DebugElement;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [DotNavItemComponent, DotSubNavComponent],
            imports: [DotNavIconModule, DotIconModule, RouterTestingModule, BrowserAnimationsModule]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotNavItemComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
        component.data = dotMenuMock();
        fixture.detectChanges();
    });

    it('should set classes', () => {
        const label = de.query(By.css('.dot-nav__item'));
        expect(label.nativeElement.classList.contains('dot-nav__item-wrapper--active')).toBe(true);
    });

    it('should have icons set', () => {
        const icon: DebugElement = de.query(By.css('dot-nav-icon'));
        const arrow: DebugElement = de.query(By.css('.dot-nav__item-arrow'));

        expect(icon.componentInstance.icon).toBe('icon');
        expect(arrow.componentInstance.name).toBe('arrow_drop_up');
    });

    it('should set label correctly', () => {
        const label: DebugElement = de.query(By.css('.dot-nav__item-label'));
        expect(label.nativeElement.textContent.trim()).toBe('Name');
    });

    it('should have dot-sub-nav', () => {
        const subNav: DebugElement = de.query(By.css('dot-sub-nav'));
        expect(subNav.componentInstance.data).toEqual(dotMenuMock());
    });
});
