import { NgClass, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

/**
 * Component to show a empty container with a title, subtitle and a button
 * @export DotEmptyContainerComponent
 */
@Component({
    selector: 'dot-empty-container',
    standalone: true,
    imports: [ButtonModule, NgIf, NgClass, DotMessagePipe],
    templateUrl: './dot-empty-container.component.html',
    styleUrls: ['./dot-empty-container.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmptyContainerComponent {
    /**
     * Icon to show in the container
     */
    @Input()
    icon: string;

    /**
     * Title to show in the container
     *
     */
    //Todo: make required when Angular 16 updated
    @Input()
    title: string;

    /**
     * Subtitle to show in the container
     */
    @Input()
    subtitle: string;

    /**
     * Button label to show in the container
     */
    @Input()
    buttonLabel: string;

    /**
     * Hide the contact us link
     */
    @Input()
    hideContactUsLink = false;

    /**
     * Button action to show in the container
     */
    @Output()
    buttonAction = new EventEmitter<void>();
}
