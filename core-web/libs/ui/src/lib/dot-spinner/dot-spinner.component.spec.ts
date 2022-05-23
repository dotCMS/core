import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotSpinnerComponent } from './dot-spinner.component';
import { By } from '@angular/platform-browser';
import { DotSpinnerModule } from './dot-spinner.module';

describe('DotSpinnerComponent', () => {
    let component: DotSpinnerComponent;
    let fixture: ComponentFixture<DotSpinnerComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [],
            imports: [DotSpinnerModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotSpinnerComponent);
        component = fixture.componentInstance;
    });

    it('should render component without inline styles', () => {
        fixture.detectChanges();
        const innerElement = fixture.debugElement.query(By.css('div'));
        expect(innerElement.styles.cssText).toBe('');
    });

    it('should render component with inline styles', () => {
        component.borderSize = '2px';
        component.size = '20px';
        fixture.detectChanges();
        const innerElement = fixture.debugElement.query(By.css('div'));
        expect(innerElement.styles.cssText).toEqual(
            'border-width: 2px; width: 20px; height: 20px;'
        );
    });
});
