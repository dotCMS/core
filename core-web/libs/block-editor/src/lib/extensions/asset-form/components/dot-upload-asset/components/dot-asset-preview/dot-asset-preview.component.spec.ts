import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotAssetPreviewComponent } from './dot-asset-preview.component';

describe('DotAssetPreviewComponent', () => {
    let component: DotAssetPreviewComponent;
    let fixture: ComponentFixture<DotAssetPreviewComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotAssetPreviewComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAssetPreviewComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
