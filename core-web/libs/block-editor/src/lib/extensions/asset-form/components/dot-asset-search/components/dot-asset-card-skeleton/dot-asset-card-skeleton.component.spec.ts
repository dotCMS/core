import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotAssetCardSkeletonComponent } from '../../../../../asset-form/components/dot-asset-search/components/dot-asset-card-skeleton/dot-asset-card-skeleton.component';

describe('DotAssetSkeletonComponent', () => {
    let component: DotAssetCardSkeletonComponent;
    let fixture: ComponentFixture<DotAssetCardSkeletonComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotAssetCardSkeletonComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAssetCardSkeletonComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
