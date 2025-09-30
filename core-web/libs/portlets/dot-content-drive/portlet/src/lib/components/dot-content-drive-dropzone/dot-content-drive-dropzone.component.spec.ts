import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentDriveDropzoneComponent } from './dot-content-drive-dropzone.component';

describe('DotContentDriveDropzoneComponent', () => {
    let component: DotContentDriveDropzoneComponent;
    let fixture: ComponentFixture<DotContentDriveDropzoneComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotContentDriveDropzoneComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContentDriveDropzoneComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
