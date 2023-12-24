import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditEmaPaletteContentTypeComponent } from './edit-ema-palette-content-type.component';

describe('EditEmaPaletteContentTypeComponent', () => {
    let component: EditEmaPaletteContentTypeComponent;
    let fixture: ComponentFixture<EditEmaPaletteContentTypeComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [EditEmaPaletteContentTypeComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EditEmaPaletteContentTypeComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
