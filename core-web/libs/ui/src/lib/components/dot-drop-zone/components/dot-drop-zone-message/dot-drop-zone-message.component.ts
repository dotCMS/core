import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    Output
} from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

import { DotMessagePipe } from '../../../../dot-message/dot-message.pipe';

@Component({
    selector: 'dot-drop-zone-message',
    standalone: true,
    imports: [CommonModule, DotMessagePipe],
    providers: [DotMessagePipe],
    templateUrl: './dot-drop-zone-message.component.html',
    styleUrls: ['./dot-drop-zone-message.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotDropZoneMessageComponent implements AfterViewInit {
    @Output() chooseFile = new EventEmitter();

    @Input() icon: string;
    @Input() severity: string;
    @Input() messageArgs: string[] = [];

    safeHTML: SafeHtml;
    @Input() set message(value: string) {
        const html = this.dm.transform(value, this.messageArgs);
        this.safeHTML = this.sanitizeHTML(html);
    }

    constructor(
        private readonly element: ElementRef,
        private readonly sanitizer: DomSanitizer,
        private readonly dm: DotMessagePipe
    ) {}

    ngAfterViewInit(): void {
        const observer = new MutationObserver(() => this.bindChooseFileBtn());
        observer.observe(this.element.nativeElement, {
            childList: true,
            subtree: true // Listens to changes in the children of the element
        });
        this.bindChooseFileBtn();
    }

    private bindChooseFileBtn() {
        const chooseFileBtn: HTMLAnchorElement =
            this.element.nativeElement.querySelector('[data-id="choose-file"]');
        chooseFileBtn?.addEventListener('click', () => this.chooseFile.emit());
    }

    sanitizeHTML(html: string): SafeHtml {
        return this.sanitizer.bypassSecurityTrustHtml(html);
    }
}
