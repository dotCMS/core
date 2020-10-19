import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotIconModule } from '../dot-icon/dot-icon.module';
import { DotIconButtonComponent } from './dot-icon-button.component';
import { By } from '@angular/platform-browser';

describe('DotIconButtonComponent', () => {
    let comp: DotIconButtonComponent;
    let fixture: ComponentFixture<DotIconButtonComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotIconButtonComponent],
            imports: [DotIconModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotIconButtonComponent);
        comp = fixture.componentInstance;
    });

    it('should render component with dot-icon component and css classes', () => {
        comp.icon = 'test';
        fixture.detectChanges();

        const icon = fixture.debugElement.query(By.css('dot-icon'));
        expect(icon.componentInstance.name).toBe('test');
    });


    it('should have type button', () => {
        const button = fixture.debugElement.query(By.css('button'));
        expect(button.attributes.type).toBe('button');
    });

    it('should set size', () => {
        comp.size = 32;
        fixture.detectChanges();
        const button = fixture.debugElement.query(By.css('button'));
        expect(button.styles.cssText).toEqual('width: 32px; height: 32px;');
    });
});
