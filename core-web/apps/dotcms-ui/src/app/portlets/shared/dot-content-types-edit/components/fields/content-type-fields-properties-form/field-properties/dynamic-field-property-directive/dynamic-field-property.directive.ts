import {
    ComponentRef,
    Directive,
    Input,
    OnChanges,
    SimpleChanges,
    ViewContainerRef,
    inject
} from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

import { DotCMSContentTypeField, DotDynamicFieldComponent } from '@dotcms/dotcms-models';

import { FieldPropertyService } from '../../../service';

@Directive({
    selector: '[dotDynamicFieldProperty]',
    standalone: false
})
export class DynamicFieldPropertyDirective implements OnChanges {
    private viewContainerRef = inject(ViewContainerRef);
    private fieldPropertyService = inject(FieldPropertyService);

    @Input() propertyName: string;
    @Input() field: DotCMSContentTypeField;
    @Input() group: UntypedFormGroup;

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.field.currentValue) {
            this.createComponent(this.propertyName);
        }
    }

    private createComponent(property): void {
        const component = this.fieldPropertyService.getComponent(property);
        const componentRef: ComponentRef<DotDynamicFieldComponent> =
            this.viewContainerRef.createComponent(component);

        componentRef.instance.property = {
            field: this.field,
            name: this.propertyName,
            value: this.field[this.propertyName]
        };

        componentRef.instance.group = this.group;
        componentRef.instance.helpText = this.fieldPropertyService.getFieldType(
            this.field.clazz
        ).helpText;
    }
}
