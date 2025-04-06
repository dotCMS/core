import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotContentletIconComponent } from './dot-contentlet-icon.component';

describe('DotContentletIconComponent', () => {
    let component: DotContentletIconComponent;
    let fixture: ComponentFixture<DotContentletIconComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotContentletIconComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContentletIconComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
