import {
    Component,
    Directive,
    Input,
    OnInit,
    TemplateRef,
    ViewContainerRef,
    inject
} from '@angular/core';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

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
    private templateRef = inject<TemplateRef<Component>>(TemplateRef);
    private viewContainer = inject(ViewContainerRef);
    private dotPropertiesService = inject(DotPropertiesService);

    private _featureFlag: FeaturedFlags;
    @Input() set dotShowHideFeature(featureFlag: FeaturedFlags) {
        this._featureFlag = featureFlag;
    }

    private _alternateTemplateRef: TemplateRef<Component>;
    @Input() set dotShowHideFeatureAlternate(alternateTemplateRef: TemplateRef<Component>) {
        this._alternateTemplateRef = alternateTemplateRef;
    }

    @Input() dotShowOnNotFound: boolean;

    get alternateTemplateRef(): TemplateRef<Component> {
        return this._alternateTemplateRef;
    }

    ngOnInit() {
        this.dotPropertiesService.getFeatureFlag(this._featureFlag).subscribe((isEnabled) => {
            this.viewContainer.clear();

            if (isEnabled) {
                this.viewContainer.createEmbeddedView(this.templateRef);
            } else if (this.alternateTemplateRef) {
                this.viewContainer.createEmbeddedView(this.alternateTemplateRef);
            } else {
                console.warn(
                    `Feature flag "${this._featureFlag}" doesn't exist or is disabled and no alternate template was provided`
                );
            }
        });
    }
}
