import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotPaletteInputFilterComponent } from './dot-palette-input-filter.component';

@Component({
    selector: 'dot-icon',
    template: '',
    standalone: false
})
class MockDotIconComponent {
    @Input() name: string;
    @Input() size: string;
}

describe('DotPaletteInputFilterComponent', () => {
    let comp: DotPaletteInputFilterComponent;
    let fixture: ComponentFixture<DotPaletteInputFilterComponent>;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotPaletteInputFilterComponent, MockDotIconComponent],
            imports: [DotSafeHtmlPipe, DotMessagePipe, HttpClientTestingModule, FormsModule],
            providers: [{ provide: CoreWebService, useClass: CoreWebServiceMock }]
        });

        fixture = TestBed.createComponent(DotPaletteInputFilterComponent);
        de = fixture.debugElement;
        comp = fixture.componentInstance;
        comp.goBackBtn = true;
        fixture.detectChanges();
    });

    it('should show Go Back button', () => {
        const goBackBtn = fixture.debugElement.query(By.css('[data-testid="goBack"]'));
        expect(goBackBtn.componentInstance).toBeTruthy();
    });

    it('should go Back when Go Back button clicked', async () => {
        jest.spyOn(comp.filter, 'emit');
        const input = de.query(By.css('[data-testId="searchInput"]')).nativeElement;
        comp.value = 'hello';
        const event = new KeyboardEvent('keyup');
        input.dispatchEvent(event);
        await fixture.whenStable();
        expect(comp.filter.emit).toHaveBeenCalledWith('hello');
        expect(comp.filter.emit).toHaveBeenCalledTimes(1);
    });
});
