import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotUvePaletteItemComponent } from './dot-uve-palette-item.component';

describe('DotUvePaletteItemComponent', () => {
    let component: DotUvePaletteItemComponent;
    let fixture: ComponentFixture<DotUvePaletteItemComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUvePaletteItemComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUvePaletteItemComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
