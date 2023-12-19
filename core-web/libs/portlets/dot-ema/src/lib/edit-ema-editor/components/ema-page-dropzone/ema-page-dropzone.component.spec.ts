import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EmaPageDropzoneComponent } from './ema-page-dropzone.component';

describe('EmaPageDropzoneComponent', () => {
    let component: EmaPageDropzoneComponent;
    let fixture: ComponentFixture<EmaPageDropzoneComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EmaPageDropzoneComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EmaPageDropzoneComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
