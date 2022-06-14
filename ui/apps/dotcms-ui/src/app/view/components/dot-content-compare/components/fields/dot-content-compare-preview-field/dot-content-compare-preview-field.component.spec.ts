import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotContentComparePreviewFieldComponent } from './dot-content-compare-preview-field.component';
import { DebugElement } from '@angular/core';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';

describe('DotContentComparePreviewFieldComponent', () => {
    let component: DotContentComparePreviewFieldComponent;
    let fixture: ComponentFixture<DotContentComparePreviewFieldComponent>;
    let de: DebugElement;
    const messageServiceMock = new MockDotMessageService({
        Download: 'Download'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotContentComparePreviewFieldComponent],
            imports: [DotMessagePipeModule],
            providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
        });
        fixture = TestBed.createComponent(DotContentComparePreviewFieldComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;

        component.fileURL = '/test/';
        component.label = 'Field Label';
        fixture.detectChanges();
    });

    it('should show image', () => {
        const img: HTMLImageElement = de.nativeElement.querySelector('[data-testId="image"]');
        expect(img.src).toContain('/test/');
    });

    it('should show label and button on error', () => {
        const img = de.nativeElement.querySelector('[data-testId="image"]');
        img.dispatchEvent(new Event('error'));
        fixture.detectChanges();

        const label = de.nativeElement.querySelector('[data-testId="label"]');
        const anchor: HTMLAnchorElement = de.nativeElement.querySelector('[data-testId="button"]');

        expect(label.innerHTML).toEqual('Field Label');
        expect(anchor.href).toContain('/test/');
    });
});
