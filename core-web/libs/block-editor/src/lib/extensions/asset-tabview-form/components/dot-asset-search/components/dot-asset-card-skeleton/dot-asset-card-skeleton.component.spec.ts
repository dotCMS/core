import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotAssetCardSkeletonComponent } from './dot-asset-card-skeleton.component';

describe('DotAssetCardSkeletonComponent', () => {
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
