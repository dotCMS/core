import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotAssetSkeletonComponent } from './dot-asset-card-skeleton.component';

describe('DotAssetSkeletonComponent', () => {
    let component: DotAssetSkeletonComponent;
    let fixture: ComponentFixture<DotAssetSkeletonComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotAssetSkeletonComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAssetSkeletonComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
