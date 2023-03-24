import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VideoPlaceholderComponent } from './video-placeholder.component';

describe('VideoPlaceholderComponent', () => {
    let component: VideoPlaceholderComponent;
    let fixture: ComponentFixture<VideoPlaceholderComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [VideoPlaceholderComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(VideoPlaceholderComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
