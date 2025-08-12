import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentDriveToolbarComponent } from './dot-content-drive-toolbar.component';

describe('DotContentDriveToolbarComponent', () => {
    let component: DotContentDriveToolbarComponent;
    let fixture: ComponentFixture<DotContentDriveToolbarComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotContentDriveToolbarComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContentDriveToolbarComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
