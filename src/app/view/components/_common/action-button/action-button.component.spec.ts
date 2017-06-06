import { ComponentFixture, TestBed, async } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { SplitButtonModule, ButtonModule, MenuItem } from 'primeng/primeng';
import { ActionButtonComponent } from './action-button.component';
import { DOTTestBed } from '../../../../test/dot-test-bed';

class RouterMock {
    navigate(): string {
        return null;
    }
}

describe('ActionButtonComponent (inline template)', () => {
    let comp: ActionButtonComponent;
    let fixture: ComponentFixture<ActionButtonComponent>;
    let de: DebugElement;

    beforeEach(async(() => {

        DOTTestBed.configureTestingModule({
            declarations: [ActionButtonComponent],
            imports: [SplitButtonModule, ButtonModule, RouterTestingModule.withRoutes([
                { path: 'test', component: ActionButtonComponent }
            ])]
        });

        fixture = TestBed.createComponent(ActionButtonComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement.query(By.css('div'));

    }));

    it('should display an action button with opctions', () => {
        let fakeButtonItems: MenuItem[] = [{
            command: () => {
                console.log('action update');
            },
            icon: 'fa-refresh',
            label: 'Update'
        },
        {
            command: () => {
                console.log('action delete');
            },
            icon: 'fa-close',
            label: 'Delete'
        },
        {
            icon: 'fa-link',
            label: 'Angular.io'
        },
        {
            icon: 'fa-paint-brush',
            label: 'Theming'
        }];

        comp.primaryAction = null;
        comp.options = fakeButtonItems;
        fixture.detectChanges();
        expect(comp.options.length).not.toBeLessThan(0);
    });

    it('should display a primary action button', () => {
        comp.primaryAction = Function;
        comp.options = null;
        fixture.detectChanges();
        expect(comp.primaryAction).toBe(Function);
    });

});