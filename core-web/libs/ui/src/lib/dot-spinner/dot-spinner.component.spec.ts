import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotSpinnerComponent } from './dot-spinner.component';

describe('DotSpinnerComponent', () => {
    let component: DotSpinnerComponent;
    let fixture: ComponentFixture<DotSpinnerComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [DotSpinnerComponent]
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
        const nativeDiv: HTMLElement = fixture.debugElement.query(By.css('div')).nativeElement;
        expect(nativeDiv.style.borderWidth).toEqual('2px');
        expect(nativeDiv.style.width).toEqual('20px');
        expect(nativeDiv.style.height).toEqual('20px');
    });
});
