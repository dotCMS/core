import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AssetTabviewFormComponent } from './asset-tabview-form.component';

describe('AssetTabviewFormComponent', () => {
    let component: AssetTabviewFormComponent;
    let fixture: ComponentFixture<AssetTabviewFormComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [AssetTabviewFormComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(AssetTabviewFormComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
