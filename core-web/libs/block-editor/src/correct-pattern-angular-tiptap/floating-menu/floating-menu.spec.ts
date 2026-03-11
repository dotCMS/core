import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { FloatingMenu } from './floating-menu';

describe('FloatingMenuComponent', () => {
    let component: FloatingMenu;
    let fixture: ComponentFixture<FloatingMenu>;

    beforeEach(async () => {
        TestBed.configureTestingModule({
            imports: [FloatingMenu]
        });

        await TestBed.compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(FloatingMenu);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should render the editor', () => {
        expect(fixture.debugElement.query(By.css('.ProseMirror'))).toBeTruthy();
    });
});
