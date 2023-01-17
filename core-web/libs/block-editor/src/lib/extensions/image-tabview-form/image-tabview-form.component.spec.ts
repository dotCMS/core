import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImageTabviewFormComponent } from './image-tabview-form.component';

describe('ImageTabviewFormComponent', () => {
    let component: ImageTabviewFormComponent;
    let fixture: ComponentFixture<ImageTabviewFormComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [ImageTabviewFormComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(ImageTabviewFormComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
