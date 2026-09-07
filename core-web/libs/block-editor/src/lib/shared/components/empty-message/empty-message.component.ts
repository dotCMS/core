import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'dot-empty-message',
    templateUrl: './empty-message.component.html',
    styleUrls: ['./empty-message.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class EmptyMessageComponent {
    @Input() title = 'No Results';
    @Input() showBackBtn = false;

    @Output() back: EventEmitter<boolean> = new EventEmitter<boolean>();
}
