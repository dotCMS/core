import { Component, Directive, Input, OnInit, TemplateRef, ViewContainerRef } from '@angular/core';

import { DotPropertiesService } from '@dotcms/data-access';

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

    constructor(
        private templateRef: TemplateRef<Component>,
        private viewContainer: ViewContainerRef,
        private dotPropertiesService: DotPropertiesService
    ) {}

    ngOnInit() {
        this.dotPropertiesService.getKey(this._featureFlag).subscribe((value) => {
            const isEnabled = value && value === 'true';
            this.viewContainer.clear();
            this.viewContainer?.createEmbeddedView(
                isEnabled ? this.templateRef : this.alternateTemplateRef
            );
        });
    }

    get alternateTemplateRef(): TemplateRef<Component> {
        return this._alternateTemplateRef;
    }
}
