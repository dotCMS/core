import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotSiteComponent } from './dot-site.component';

describe('DotSiteComponent', () => {
    let component: DotSiteComponent;
    let fixture: ComponentFixture<DotSiteComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotSiteComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotSiteComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
