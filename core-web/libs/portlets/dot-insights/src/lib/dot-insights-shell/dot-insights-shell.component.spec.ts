import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotInsightsShellComponent } from './dot-insights-shell.component';

describe('DotInsightsShellComponent', () => {
    let component: DotInsightsShellComponent;
    let fixture: ComponentFixture<DotInsightsShellComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotInsightsShellComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotInsightsShellComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
