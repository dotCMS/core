import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImageCardSkeletonComponent } from './image-card-skeleton.component';

describe('ImageCardSkeletonComponent', () => {
    let component: ImageCardSkeletonComponent;
    let fixture: ComponentFixture<ImageCardSkeletonComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [ImageCardSkeletonComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(ImageCardSkeletonComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
