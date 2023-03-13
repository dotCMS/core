import { Component, DebugElement, Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';

import { DotRelativeDateDirective } from './dot-relative-date.directive';

@Component({
    template: `<div *dotRelativeDate="'2023-03-11 00:04:47.802'; data as result">{{ result }}</div>`
})
class TestComponent {}

@Injectable()
class DotFormatDateServiceMock {
    getRelative() {
        return '6 hours ago';
    }
}

describe('DotRelativeDateDirective', () => {
    let fixture: ComponentFixture<TestComponent>;
    let element: DebugElement;
    let dotFormatDateService: DotFormatDateService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [DotRelativeDateDirective],
            declarations: [TestComponent],
            providers: [{ provide: DotFormatDateService, useClass: DotFormatDateServiceMock }]
        }).compileComponents();

        fixture = TestBed.createComponent(TestComponent);
        dotFormatDateService = fixture.debugElement.injector.get(DotFormatDateService);

        spyOn(dotFormatDateService, 'getRelative').and.callThrough();
        fixture.detectChanges();
        element = fixture.debugElement.query(By.css('div'));
    });

    it('should keep same text after event if max length is not reached', () => {
        expect(dotFormatDateService.getRelative).toHaveBeenCalledTimes(1);
        expect(element.nativeElement.textContent).toBe('6 hours ago');
    });
});
