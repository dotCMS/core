import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditEmaPaletteContentletsComponent } from './edit-ema-palette-contentlets.component';

describe('EditEmaPaletteContentletsComponent', () => {
    let component: EditEmaPaletteContentletsComponent;
    let fixture: ComponentFixture<EditEmaPaletteContentletsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [EditEmaPaletteContentletsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EditEmaPaletteContentletsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
