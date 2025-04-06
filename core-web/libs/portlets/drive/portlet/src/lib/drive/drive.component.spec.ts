import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DriveComponent } from './drive.component';

describe('DriveComponent', () => {
    let component: DriveComponent;
    let fixture: ComponentFixture<DriveComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DriveComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DriveComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
