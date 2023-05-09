import { Component, Directive, Input, OnInit, TemplateRef, ViewContainerRef } from '@angular/core';

import { DotPropertiesService } from '@dotcms/data-access';

/**
 * Structural directive to give the ability of show or hide a component
 * based on a feature flag. This directive can receive an alternative template
 * to show when the feature is disabled.
 *
 * @example
 * with default template
 *  ```
 *  </ng-container *dotShowHideFeature="featureFlag">
 *       <feature-component></feature-component>
 *   </ng-container>
 *  ```
 *
 * @example
 * with default template and alternate template
 * ```
 * <ng-template #enabledComponent>
 *       <feature-component></feature-component>
 * </ng-template>
 *
 * <ng-template #disabledComponent>
 *       <alternate-component></alternate-component>
 * </ng-template>
 *
 * <ng-container
 *      *dotShowHideFeature="featureFlag; alternate: disabledComponent"
 *      [ngTemplateOutlet]="enabledComponent"
 *  ></ng-container>
 * ```
 *
 * @export
 * @class DotShowHideFeatureDirective
 * @implements {OnInit}
 */
@Directive({
    selector: '[dotShowHideFeature]',
    standalone: true
})
export class DotShowHideFeatureDirective implements OnInit {
    private _featureFlag: string;
    @Input() set dotShowHideFeature(featureFlag: string) {
        this._featureFlag = featureFlag;
    }

    private _alternateTemplateRef: TemplateRef<Component>;
    @Input() set dotShowHideFeatureAlternate(alternateTemplateRef: TemplateRef<Component>) {
        this._alternateTemplateRef = alternateTemplateRef;
    }

    get alternateTemplateRef(): TemplateRef<Component> {
        return this._alternateTemplateRef;
    }

    constructor(
        private templateRef: TemplateRef<Component>,
        private viewContainer: ViewContainerRef,
        private dotPropertiesService: DotPropertiesService
    ) {}

    ngOnInit() {
        this.dotPropertiesService.getKey(this._featureFlag).subscribe((value) => {
            const isEnabled = value && value === 'true';
            this.viewContainer.clear();

            if (isEnabled) {
                this.viewContainer.createEmbeddedView(this.templateRef);
            } else if (this.alternateTemplateRef) {
                this.viewContainer.createEmbeddedView(this.alternateTemplateRef);
            }
        });
    }
}
