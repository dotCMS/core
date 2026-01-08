import { Component, signal } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

import { DotRenderModes } from '@dotcms/dotcms-models';

import { FieldProperty } from '../field-properties.model';

interface RenderMode {
    value: typeof DotRenderModes.COMPONENT | typeof DotRenderModes.IFRAME;
    label: string;
    tooltip: string;
}

@Component({
    selector: 'dot-render-mode-property',
    templateUrl: './render-mode-property.component.html',
    styleUrls: ['./render-mode-property.component.scss'],
    standalone: false
})
export class NewRenderModePropertyComponent {
    property: FieldProperty;
    group: UntypedFormGroup;

    /**
     * Signals the render modes available for the field
     * @type {Signal<RenderMode[]>}
     */
    $renderModes = signal<RenderMode[]>([
        {
            value: DotRenderModes.COMPONENT,
            label: 'contenttypes.field.properties.newRenderMode.component.label',
            tooltip: 'contenttypes.field.properties.newRenderMode.component.tooltip'
        },
        {
            value: DotRenderModes.IFRAME,
            label: 'contenttypes.field.properties.newRenderMode.iframe.label',
            tooltip: 'contenttypes.field.properties.newRenderMode.iframe.tooltip'
        }
    ]);

    /**
     * Returns the field control from the form group
     * @type {FormControl}
     */
    get field() {
        return this.group.get(this.property.name);
    }

    /**
     * Returns the value of the field control
     * @type {unknown}
     */
    get value() {
        return this.field?.value;
    }

    /**
     * Sets the value of the field control
     * @param value {RenderMode['value']}
     */
    choose(value: RenderMode['value']) {
        this.field?.setValue(value);
    }
}
