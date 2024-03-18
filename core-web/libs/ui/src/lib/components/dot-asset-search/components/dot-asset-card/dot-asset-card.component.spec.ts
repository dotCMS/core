import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotAssetCardComponent } from './dot-asset-card.component';

describe('DotAssetCardComponent', () => {
    let component: DotAssetCardComponent;
    let fixture: ComponentFixture<DotAssetCardComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotAssetCardComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAssetCardComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
