import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DotIconComponent } from './dot-icon.component';
import { By } from '@angular/platform-browser';

describe('DotIconComponent', () => {
    let comp: DotIconComponent;
    let fixture: ComponentFixture<DotIconComponent>;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotIconComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotIconComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
        comp.name = 'test';
    });

    it('should have css classes based on attributes', () => {
        fixture.detectChanges();
        const icon = de.query(By.css('i')).nativeElement;
        expect(icon.classList).toContain('material-icons');
        expect(icon.innerText).toBe(comp.name);
        expect(icon.style.fontSize).toBe('');
    });

    it('should set inline font size', () => {
        comp.size = 120;
        fixture.detectChanges();
        const icon = de.query(By.css('i')).nativeElement;
        expect(icon.style.fontSize).toBe('120px');
    });
});
