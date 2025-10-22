import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotUvePaletteComponent } from './dot-uve-palette.component';

describe('DotUvePaletteComponent', () => {
    let component: DotUvePaletteComponent;
    let fixture: ComponentFixture<DotUvePaletteComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUvePaletteComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUvePaletteComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
