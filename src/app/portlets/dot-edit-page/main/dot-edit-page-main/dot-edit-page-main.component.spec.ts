import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditPageMainComponent } from './dot-edit-page-main.component';
import { DotEditPageNavModule } from '../dot-edit-page-nav/dot-edit-page-nav.module';
import { Observable } from 'rxjs/Observable';
import { RouterTestingModule } from '@angular/router/testing';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

describe('DotEditPageMainComponent', () => {
    let component: DotEditPageMainComponent;
    let fixture: ComponentFixture<DotEditPageMainComponent>;

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                declarations: [DotEditPageMainComponent],
                imports: [RouterTestingModule, DotEditPageNavModule],
            }).compileComponents();
        }),
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditPageMainComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should have router-outlet', () => {
        expect(fixture.debugElement.query(By.css('router-outlet'))).not.toBeNull();
    });

    it('should have dot-edit-page-nav', () => {
        expect(fixture.debugElement.query(By.css('dot-edit-page-nav'))).not.toBeNull();
    });
});
