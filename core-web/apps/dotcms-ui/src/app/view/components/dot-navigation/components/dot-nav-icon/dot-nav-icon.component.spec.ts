import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotIconComponent } from '@dotcms/ui';

import { DotNavIconComponent } from './dot-nav-icon.component';

describe('DotNavIconComponent', () => {
    let comp: DotNavIconComponent;
    let fixture: ComponentFixture<DotNavIconComponent>;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [DotNavIconComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotNavIconComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
    });

    it('should have dot-icon', () => {
        comp.icon = 'test';
        fixture.detectChanges();
        const dotIconComponent: DotIconComponent = de.query(By.css('dot-icon')).componentInstance;
        expect(dotIconComponent).toBeDefined();
        expect(dotIconComponent.size).toBe(18);
    });

    it('should have font awesome icon', () => {
        comp.icon = 'fa-test';
        fixture.detectChanges();
        const faIcon = de.query(By.css('.fa'));
        expect(faIcon.componentInstance).toBeDefined();
        expect(faIcon.nativeElement.style['font-size']).toBe('18px');
    });
});
