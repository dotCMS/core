import { of } from 'rxjs';

import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { DotShowHideFeatureDirective } from './dot-show-hide-feature.directive';

@Component({
    selector: 'dot-test',
    template: `
        <ng-template #enabledComponent>
            <div data-testId="ensabled-component">test</div>
        </ng-template>
        <ng-template #disabledComponent>
            <div data-testId="disabled-component">alternate</div>
        </ng-template>
        <ng-container
            *dotShowHideFeature="featureFlag; alternate: disabledComponent"
            [ngTemplateOutlet]="enabledComponent"
        ></ng-container>
    `
})
class TestComponent {
    featureFlag = FeaturedFlags.FEATURE_FLAG_TEMPLATE_BUILDER;
}

describe('DotShowHideFeatureDirective', () => {
    let fixture: ComponentFixture<TestComponent>;
    let dotPropertiesService: DotPropertiesService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestComponent],
            imports: [DotShowHideFeatureDirective],
            providers: [{ provide: DotPropertiesService, useValue: { getKey: () => of('true') } }]
        });

        fixture = TestBed.createComponent(TestComponent);
        dotPropertiesService = TestBed.inject(DotPropertiesService);
    });

    describe('with feature flag enabled', () => {
        beforeEach(() => fixture.detectChanges());

        it('should render enabled component', () => {
            const componentEl = fixture.debugElement.query(
                By.css('[data-testId="ensabled-component"]')
            );

            expect(componentEl).toBeTruthy();
        });
    });

    describe('with feature flag disabled', () => {
        beforeEach(() => {
            spyOn(dotPropertiesService, 'getKey').and.returnValue(of('false'));
            fixture.detectChanges();
        });

        it('should render disabled component', () => {
            const componentEl = fixture.debugElement.query(
                By.css('[data-testId="disabled-component"]')
            );

            expect(componentEl).toBeTruthy();
        });
    });

    describe('with feature flag enabled', () => {
        beforeEach(() => {
            spyOn(dotPropertiesService, 'getKey').and.returnValue(of('false'));
            fixture.detectChanges();
        });

        it('should render disabled component', () => {
            const componentEl = fixture.debugElement.query(
                By.css('[data-testId="disabled-component"]')
            );

            expect(componentEl).toBeTruthy();
        });
    });
});
