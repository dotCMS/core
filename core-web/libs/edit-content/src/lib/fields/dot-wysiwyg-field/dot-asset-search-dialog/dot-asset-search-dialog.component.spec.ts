import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotAssetSearchDialogComponent } from './dot-asset-search-dialog.component';

describe('DotAssetSearchDialogComponent', () => {
    let component: DotAssetSearchDialogComponent;
    let fixture: ComponentFixture<DotAssetSearchDialogComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotAssetSearchDialogComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAssetSearchDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
