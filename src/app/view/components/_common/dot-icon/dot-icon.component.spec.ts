import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DotIconComponent } from './dot-icon.component';
import { DOTTestBed } from '../../../../test/dot-test-bed';

describe('DotIconComponent', () => {
    let comp: DotIconComponent;
    let fixture: ComponentFixture<DotIconComponent>;
    let de: DebugElement;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotIconComponent]
        });

        fixture = TestBed.createComponent(DotIconComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
    });

    it('should have css classes based on attributes', () => {
        comp.name = 'test';
        fixture.detectChanges();
        expect(de.nativeElement.childNodes[0].classList).toContain('material-icons');
        expect(de.nativeElement.childNodes[0].innerText).toBe(comp.name);
    });
});
