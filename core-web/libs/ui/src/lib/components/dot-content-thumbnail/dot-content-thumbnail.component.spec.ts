import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentThumbnailComponent } from './dot-content-thumbnail.component';

describe('DotContentThumbnailComponent', () => {
    let component: DotContentThumbnailComponent;
    let fixture: ComponentFixture<DotContentThumbnailComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotContentThumbnailComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContentThumbnailComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
