import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DotIconModule } from '../dot-icon/dot-icon.module';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotIconButtonComponent } from './dot-icon-button.component';
import { By } from '@angular/platform-browser';

describe('DotIconButtonComponent', () => {
    let comp: DotIconButtonComponent;
    let fixture: ComponentFixture<DotIconButtonComponent>;
    let de: DebugElement;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotIconButtonComponent],
            imports: [DotIconModule]
        });

        fixture = TestBed.createComponent(DotIconButtonComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
    });

    it('should render component with dot-icon component and css classes', () => {
        comp.icon = 'test';
        fixture.detectChanges();

        const icon = fixture.debugElement.query(By.css('dot-icon'));
        expect(icon.componentInstance.name).toBe('test');
    });

    it('should emit event on button click', () => {
        let res;

        comp.click.subscribe((event) => {
            res = event;
        });
        fixture.detectChanges();

        const button = fixture.debugElement.query(By.css('button'));
        button.nativeElement.click();
        expect(res).toBeDefined();
    });

    it('should set button to disabled state', () => {
        comp.disabled = true;
        comp.icon = 'test';

        let res;

        comp.click.subscribe((event) => {
            res = event;
        });
        fixture.detectChanges();

        const button = fixture.debugElement.query(By.css('button'));
        button.nativeElement.click();
        expect(res).not.toBeDefined();
    });
});
