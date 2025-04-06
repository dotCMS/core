import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotContentletThumbnailComponent } from './dot-contentlet-thumbnail.component';

describe('DotContentletThumbnailComponent', () => {
    let component: DotContentletThumbnailComponent;
    let fixture: ComponentFixture<DotContentletThumbnailComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotContentletThumbnailComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContentletThumbnailComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
