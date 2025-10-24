import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotUvePaletteContentletComponent } from './dot-uve-palette-contentlet.component';

describe('DotUvePaletteContentletComponent', () => {
    let component: DotUvePaletteContentletComponent;
    let fixture: ComponentFixture<DotUvePaletteContentletComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUvePaletteContentletComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUvePaletteContentletComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
