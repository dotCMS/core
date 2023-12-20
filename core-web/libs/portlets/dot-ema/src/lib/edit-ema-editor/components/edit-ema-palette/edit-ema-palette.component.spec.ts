import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditEmaPaletteComponent } from './edit-ema-palette.component';

describe('EditEmaPaletteComponent', () => {
    let component: EditEmaPaletteComponent;
    let fixture: ComponentFixture<EditEmaPaletteComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [EditEmaPaletteComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EditEmaPaletteComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
