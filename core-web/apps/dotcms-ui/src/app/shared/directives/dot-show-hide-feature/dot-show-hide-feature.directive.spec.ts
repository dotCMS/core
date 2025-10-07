import { of } from 'rxjs';

import { Component, TemplateRef, ViewContainerRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

import { DotShowHideFeatureDirective } from './dot-show-hide-feature.directive';

@Component({
    selector: 'dot-test',
    template: `
        <ng-container *dotShowHideFeature="featureFlag">
            <div data-testId="enabled-component">test</div>
        </ng-container>
    `,
    standalone: false
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
            providers: [
                ViewContainerRef,
                TemplateRef,
                { provide: DotPropertiesService, useValue: { getFeatureFlag: () => of(true) } }
            ]
        });

        fixture = TestBed.createComponent(TestComponent);
        dotPropertiesService = TestBed.inject(DotPropertiesService);
    });

    describe('with feature flag enabled', () => {
        beforeEach(() => fixture.detectChanges());

        it('should render enabled component', () => {
            const componentEl = fixture.debugElement.query(
                By.css('[data-testId="enabled-component"]')
            );

            expect(componentEl).toBeTruthy();
        });
    });

    describe('with feature flag disabled', () => {
        beforeEach(() => {
            jest.spyOn(dotPropertiesService, 'getFeatureFlag').mockReturnValue(of(false));
            fixture.detectChanges();
        });

        it('should not render enabled component', () => {
            const componentEl = fixture.debugElement.query(
                By.css('[data-testId="enabled-component"]')
            );

            expect(componentEl).not.toBeTruthy();
        });
    });
});

@Component({
    selector: 'dot-test',
    template: `
        <ng-template #enabledComponent>
            <div data-testId="enabled-component">test</div>
        </ng-template>
        <ng-template #disabledComponent>
            <div data-testId="disabled-component">alternate</div>
        </ng-template>
        <ng-container
            *dotShowHideFeature="featureFlag; alternate: disabledComponent"
            [ngTemplateOutlet]="enabledComponent"></ng-container>
    `,
    standalone: false
})
class TestWithAlternateTemplateComponent {
    featureFlag = FeaturedFlags.FEATURE_FLAG_TEMPLATE_BUILDER;
}

describe('DotShowHideFeatureDirective with alternate template', () => {
    let fixture: ComponentFixture<TestWithAlternateTemplateComponent>;
    let dotPropertiesService: DotPropertiesService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [TestWithAlternateTemplateComponent],
            imports: [DotShowHideFeatureDirective],
            providers: [
                ViewContainerRef,
                TemplateRef,
                {
                    provide: DotPropertiesService,
                    useValue: { getFeatureFlag: () => of(true) }
                }
            ]
        });

        fixture = TestBed.createComponent(TestWithAlternateTemplateComponent);
        dotPropertiesService = TestBed.inject(DotPropertiesService);
    });

    describe('with feature flag enabled', () => {
        beforeEach(() => fixture.detectChanges());

        it('should render enabled component', () => {
            const componentEl = fixture.debugElement.query(
                By.css('[data-testId="enabled-component"]')
            );

            expect(componentEl).toBeTruthy();
        });

        it('should not render disabled component', () => {
            const componentEl = fixture.debugElement.query(
                By.css('[data-testId="disabled-component"]')
            );

            expect(componentEl).not.toBeTruthy();
        });
    });

    describe('with feature flag disabled', () => {
        beforeEach(() => {
            jest.spyOn(dotPropertiesService, 'getFeatureFlag').mockReturnValue(of(false));
            fixture.detectChanges();
        });

        it('should render disabled component', () => {
            const componentEl = fixture.debugElement.query(
                By.css('[data-testId="disabled-component"]')
            );

            expect(componentEl).toBeTruthy();
        });

        it('should not render enabled component', () => {
            const componentEl = fixture.debugElement.query(
                By.css('[data-testId="enabled-component"]')
            );

            expect(componentEl).not.toBeTruthy();
        });
    });
});
