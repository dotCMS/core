import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotUVEPaletteContenttypeComponent } from './dot-uve-palette-contenttype.component';

describe('DotUVEPaletteContenttypeComponent', () => {
    let component: DotUVEPaletteContenttypeComponent;
    let fixture: ComponentFixture<DotUVEPaletteContenttypeComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUVEPaletteContenttypeComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUVEPaletteContenttypeComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
