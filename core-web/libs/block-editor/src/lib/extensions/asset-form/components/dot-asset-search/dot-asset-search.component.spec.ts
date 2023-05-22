import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotAssetSearchComponent } from './dot-asset-search.component';

describe('DotImageSearchComponent', () => {
    let component: DotAssetSearchComponent;
    let fixture: ComponentFixture<DotAssetSearchComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotAssetSearchComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAssetSearchComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
