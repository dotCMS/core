import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UploadPlaceholderComponent } from './upload-placeholder.component';

describe('UploadPlaceholderComponent', () => {
    let component: UploadPlaceholderComponent;
    let fixture: ComponentFixture<UploadPlaceholderComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [UploadPlaceholderComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(UploadPlaceholderComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
