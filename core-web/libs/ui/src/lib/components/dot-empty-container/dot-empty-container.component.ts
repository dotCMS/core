import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

export interface PrincipalConfiguration {
    title: string;
    subtitle?: string;
    icon: string;
}

/**
 * Component to show an empty container with a title, subtitle and a button
 * @export DotEmptyContainerComponent
 */
@Component({
    selector: 'dot-empty-container',
    standalone: true,
    imports: [ButtonModule, NgClass, DotMessagePipe],
    templateUrl: './dot-empty-container.component.html',
    styleUrls: ['./dot-empty-container.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmptyContainerComponent {
    //Todo: change to input signal when ui migrated to jest
    /**
     * Principal configuration of the component
     */
    @Input()
    configuration: PrincipalConfiguration;

    /**
     * Button label to show in the container
     */
    @Input()
    buttonLabel: string;

    /**
     * Change the button type to secondary
     */
    @Input()
    secondaryButton = false;

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
