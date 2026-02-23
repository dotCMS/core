import {
    ComponentRef,
    Directive,
    Input,
    OnChanges,
    OnDestroy,
    SimpleChanges,
    ViewContainerRef,
    inject
} from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

import { DotCMSContentTypeField, DotDynamicFieldComponent } from '@dotcms/dotcms-models';
import { isEqual } from '@dotcms/utils';

import { FieldPropertyService } from '../../../service';

@Directive({
    selector: '[dotDynamicFieldProperty]',
    standalone: false
})
export class DynamicFieldPropertyDirective implements OnChanges, OnDestroy {
    private viewContainerRef = inject(ViewContainerRef);
    private fieldPropertyService = inject(FieldPropertyService);
    private componentRef: ComponentRef<DotDynamicFieldComponent> | null = null;
    private previousFieldId: string | null = null;
    private previousPropertyName: string | null = null;

    @Input() propertyName: string;
    @Input() field: DotCMSContentTypeField;
    @Input() group: UntypedFormGroup;

    ngOnChanges(changes: SimpleChanges): void {
        const fieldChanged = changes.field;
        const propertyNameChanged = changes.propertyName;
        const groupChanged = changes.group;

        // Only create component if field, propertyName or group actually changed
        if (
            fieldChanged?.currentValue &&
            (fieldChanged.firstChange ||
                !isEqual(fieldChanged.previousValue, fieldChanged.currentValue) ||
                propertyNameChanged?.firstChange ||
                propertyNameChanged?.previousValue !== propertyNameChanged?.currentValue ||
                groupChanged?.firstChange ||
                groupChanged?.previousValue !== groupChanged?.currentValue)
        ) {
            const currentFieldId = this.field?.id || null;
            const currentPropertyName = this.propertyName;

            // Check if we need to recreate the component
            const shouldRecreate =
                !this.componentRef ||
                this.previousFieldId !== currentFieldId ||
                this.previousPropertyName !== currentPropertyName;

            if (shouldRecreate) {
                this.destroyComponent();
                this.createComponent(this.propertyName);
                this.previousFieldId = currentFieldId;
                this.previousPropertyName = currentPropertyName;
            } else {
                // Update existing component instance if field or group changed but same field/property
                this.updateComponent();
            }
        }
    }

    ngOnDestroy(): void {
        this.destroyComponent();
    }

    private createComponent(property: string): void {
        const component = this.fieldPropertyService.getComponent(property);
        this.componentRef = this.viewContainerRef.createComponent(component);

        this.updateComponent();
    }

    private updateComponent(): void {
        if (!this.componentRef || !this.field) {
            return;
        }

        this.componentRef.instance.property = {
            field: this.field,
            name: this.propertyName,
            value: this.field[this.propertyName]
        };

        this.componentRef.instance.group = this.group;
        this.componentRef.instance.helpText = this.fieldPropertyService.getFieldType(
            this.field.clazz
        ).helpText;
    }

    private destroyComponent(): void {
        if (this.componentRef) {
            this.componentRef.destroy();
            this.componentRef = null;
        }
        this.viewContainerRef.clear();
    }
}
