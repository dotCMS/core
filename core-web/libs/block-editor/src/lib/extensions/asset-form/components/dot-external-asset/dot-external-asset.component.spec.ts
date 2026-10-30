import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { DotExternalAssetComponent } from './dot-external-asset.component';

describe('DotExternalAssetComponent', () => {
    let component: DotExternalAssetComponent;
    let fixture: ComponentFixture<DotExternalAssetComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotExternalAssetComponent],
            imports: [ReactiveFormsModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotExternalAssetComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
