import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentDriveContentTypeFieldComponent } from './dot-content-drive-content-type-field.component';

describe('DotContentDriveContentTypeFieldComponent', () => {
    let component: DotContentDriveContentTypeFieldComponent;
    let fixture: ComponentFixture<DotContentDriveContentTypeFieldComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotContentDriveContentTypeFieldComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContentDriveContentTypeFieldComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
