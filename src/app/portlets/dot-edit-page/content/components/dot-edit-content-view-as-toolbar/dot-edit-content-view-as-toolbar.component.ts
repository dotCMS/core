import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DotEditPageViewAs } from '../../../../../shared/models/dot-edit-page-view-as/dot-edit-page-view-as.model';
import { DotPersona } from '../../../../../shared/models/dot-persona/dot-persona.model';
import { DotLanguage } from '../../../../../shared/models/dot-language/dot-language.model';
import { DotDevice } from '../../../../../shared/models/dot-device/dot-device.model';

@Component({
    selector: 'dot-edit-content-view-as-toolbar',
    templateUrl: './dot-edit-content-view-as-toolbar.component.html',
    styleUrls: ['./dot-edit-content-view-as-toolbar.component.scss']
})
export class DotEditContentViewAsToolbarComponent {
    @Input() value: DotEditPageViewAs;
    @Output() changeViewAs = new EventEmitter<DotEditPageViewAs>();

    constructor() {}

    /**
     * Handle the changes in Persona Selector.
     *
     * @param {DotPersona} persona
     * @memberof DotEditContentViewAsToolbarComponent
     */
    changePersonaHandler(persona: DotPersona): void {
        this.value.persona = persona;
        this.changeViewAs.emit(this.value);
    }

    /**
     * Handle changes in Language Selector.
     *
     * @param {DotLanguage} language
     * @memberof DotEditContentViewAsToolbarComponent
     */
    changeLanguageHandler(language: DotLanguage): void {
        this.value.language = language;
        this.changeViewAs.emit(this.value);
    }

    /**
     * Handle changes in Device Selector.
     *
     * @param {DotDevice} device
     * @memberof DotEditContentViewAsToolbarComponent
     */
    changeDeviceHandler(device: DotDevice): void {
        this.value.device = device;
        this.changeViewAs.emit(this.value);
    }
}
