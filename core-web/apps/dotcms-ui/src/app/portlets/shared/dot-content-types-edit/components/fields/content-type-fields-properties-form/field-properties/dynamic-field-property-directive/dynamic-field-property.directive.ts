import {
    Directive,
    ViewContainerRef,
    Input,
    ComponentFactoryResolver,
    ComponentRef,
    SimpleChanges,
    OnChanges
} from '@angular/core';
import { DotCMSContentTypeField, DotDynamicFieldComponent } from '@dotcms/dotcms-models';
import { UntypedFormGroup } from '@angular/forms';
import { FieldPropertyService } from '../../../service';

@Directive({
    selector: '[dotDynamicFieldProperty]'
})
export class DynamicFieldPropertyDirective implements OnChanges {
    @Input() propertyName: string;
    @Input() field: DotCMSContentTypeField;
    @Input() group: UntypedFormGroup;

    constructor(
        private viewContainerRef: ViewContainerRef,
        private resolver: ComponentFactoryResolver,
        private fieldPropertyService: FieldPropertyService
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.field.currentValue) {
            this.createComponent(this.propertyName);
        }
    }

    private createComponent(property): void {
        const component = this.fieldPropertyService.getComponent(property);
        const componentFactory = this.resolver.resolveComponentFactory(component);
        const componentRef: ComponentRef<DotDynamicFieldComponent> =
            this.viewContainerRef.createComponent(componentFactory);

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
