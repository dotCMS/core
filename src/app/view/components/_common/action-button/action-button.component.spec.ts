import { ComponentFixture, TestBed, async } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { SplitButtonModule, MenuItem } from 'primeng/primeng';
import { ActionButtonComponent } from './action-button.component';
import { DOTTestBed } from '../../../../test/dot-test-bed';

describe('ActionButtonComponent', () => {
    let comp: ActionButtonComponent;
    let fixture: ComponentFixture<ActionButtonComponent>;
    let de: DebugElement;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [ActionButtonComponent],
            imports: [
                SplitButtonModule,
                RouterTestingModule.withRoutes([{
                    component: ActionButtonComponent,
                    path: 'test'
                }])
            ]
        });

        fixture = TestBed.createComponent(ActionButtonComponent);
        comp = fixture.componentInstance;
    }));

    it('should display an action button with opctions', () => {
        let fakeButtonItems: MenuItem[] = [{
            command: () => {},
            icon: 'fa-refresh',
            label: 'Update'
        },
        {
            command: () => {},
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
        comp.model = fakeButtonItems;
        fixture.detectChanges();
        de = fixture.debugElement.query(By.css('p-splitButton'));
        expect(de.nativeElement.className).toContain('multiple-options');
    });

    it('should display a primary action button', () => {
        comp.command = () => {};
        fixture.detectChanges();
        de = fixture.debugElement.query(By.css('p-splitButton'));
        expect(de.nativeElement.className).not.toContain('multiple-options');
    });

});
