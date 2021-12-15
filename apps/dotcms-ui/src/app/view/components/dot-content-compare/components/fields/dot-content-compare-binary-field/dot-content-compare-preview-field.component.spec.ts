import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentComparePreviewFieldComponent } from './dot-content-compare-preview-field.component';

describe('DotContentCompareBinaryFieldComponent', () => {
    let component: DotContentComparePreviewFieldComponent;
    let fixture: ComponentFixture<DotContentComparePreviewFieldComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotContentComparePreviewFieldComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotContentComparePreviewFieldComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
