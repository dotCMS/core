import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotAssetCardListComponent } from '../../../../../asset-form/components/dot-asset-search/components/dot-asset-card-list/dot-asset-card-list.component';

describe('DotAssetCardListComponent', () => {
    let component: DotAssetCardListComponent;
    let fixture: ComponentFixture<DotAssetCardListComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotAssetCardListComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAssetCardListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
