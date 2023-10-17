import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotBinaryFieldPreviewComponent } from './dot-binary-field-preview.component';

describe('DotBinaryFieldPreviewComponent', () => {
    let component: DotBinaryFieldPreviewComponent;
    let fixture: ComponentFixture<DotBinaryFieldPreviewComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotBinaryFieldPreviewComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotBinaryFieldPreviewComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
