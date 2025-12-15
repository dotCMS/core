import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotThemeComponent } from './dot-theme.component';

describe('DotThemeComponent', () => {
    let component: DotThemeComponent;
    let fixture: ComponentFixture<DotThemeComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotThemeComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotThemeComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
