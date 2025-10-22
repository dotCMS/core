import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotUvePaletteListComponent } from './dot-uve-palette-list.component';

describe('DotUvePaletteListComponent', () => {
    let component: DotUvePaletteListComponent;
    let fixture: ComponentFixture<DotUvePaletteListComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUvePaletteListComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUvePaletteListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
