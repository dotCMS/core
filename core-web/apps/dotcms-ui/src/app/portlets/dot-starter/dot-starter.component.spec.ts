import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { MarkdownModule } from 'ngx-markdown';

import { PLATFORM_ID } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { KnobModule } from 'primeng/knob';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { ProgressBarModule } from 'primeng/progressbar';
import { RadioButtonModule } from 'primeng/radiobutton';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { ButtonCopyComponent } from '@dotcms/ui';

import { DotStarterComponent } from './dot-starter.component';
import { state } from './store';

describe('DotStarterComponent', () => {
    let spectator: Spectator<DotStarterComponent>;
    let component: DotStarterComponent;

    const createComponent = createComponentFactory({
        component: DotStarterComponent,
        imports: [
            AccordionModule,
            ButtonCopyComponent,
            ButtonModule,
            KnobModule,
            MarkdownModule.forRoot(),
            NoopAnimationsModule,
            OverlayPanelModule,
            ProgressBarModule,
            RadioButtonModule,
            TagModule,
            TooltipModule
        ],
        providers: [
            {
                provide: PLATFORM_ID,
                useValue: 'browser'
            }
        ]
    });

    beforeEach(() => {
        localStorage.clear();
        jest.restoreAllMocks();
        // Reset state before each test
        patchState(state, {
            progress: 0,
            activeAccordionIndex: 0,
            currentStateLabel: ''
        });
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
    });

    it('should render the onboarding title and description', () => {
        spectator.detectChanges();

        expect(spectator.query('header h1')?.textContent).toContain(
            'Build Your First dotCMS Headless Application'
        );
        expect(spectator.query('header p')?.textContent).toBeTruthy();
    });

    it('should render framework options', () => {
        spectator.detectChanges();

        const frameworkLabels = spectator.queryAll('.logo-item label span');
        const labelTexts = frameworkLabels.map((label) => label.textContent?.trim());

        expect(frameworkLabels.length).toBe(6);
        expect(labelTexts).toContain('Next.js');
        expect(labelTexts).toContain('Angular');
        expect(labelTexts).toContain('Angular SSR');
        expect(labelTexts).toContain('Astro');
        expect(labelTexts).toContain('PHP');
        expect(labelTexts).toContain('.NET');

        // Verify all framework labels are present in the HTML
        const logoItems = spectator.queryAll('.logo-item');
        expect(logoItems.length).toBe(6);
        logoItems.forEach((item) => {
            const label = item.querySelector('label span');
            expect(label).toBeTruthy();
            expect(label?.textContent?.trim()).toBeTruthy();
        });
    });

    it('should initialize with first step active', () => {
        spectator.detectChanges();

        expect(component.state().activeAccordionIndex).toBe(0);
        expect(component.state().progress).toBe(0);
    });

    it('should update progress when activeIndexChange is called', () => {
        const setItemSpy = jest.spyOn(Storage.prototype, 'setItem');
        spectator.detectChanges();

        component.activeIndexChange(1);

        expect(component.state().activeAccordionIndex).toBe(1);
        expect(component.state().progress).toBeGreaterThan(0);
        expect(setItemSpy).toHaveBeenCalledWith(
            'dotcmsDeveloperOnboarding',
            '1'
        );
    });

    it('should calculate progress correctly for last step', () => {
        spectator.detectChanges();

        const totalSteps = component.content.steps.length;
        const lastIndex = totalSteps - 1;
        component.activeIndexChange(lastIndex);

        expect(component.state().progress).toBe(100);
        expect(component.state().currentStateLabel).toBe(
            component.content.steps[lastIndex].title
        );
    });

    it('should mark steps as completed correctly', () => {
        spectator.detectChanges();

        component.activeIndexChange(2);
        spectator.detectChanges();

        // Verify HTML shows correct icons for completed steps (0, 1, 2)
        const completedIcons = spectator.queryAll('.pi-circle-on.dot-onboarding__step-completed');
        expect(completedIcons.length).toBe(3);

        // Verify HTML shows correct icons for incomplete steps (3+)
        const incompleteIcons = spectator.queryAll('.pi-circle-off');
        expect(incompleteIcons.length).toBe(5);

        // Verify markdown components receive correct step descriptions
        const stepsWithDescriptions = component.content.steps.filter((step) => step.description);
        const stepDescriptionMarkdowns = spectator.queryAll('markdown.dot-step__description');

        expect(stepDescriptionMarkdowns.length).toBe(stepsWithDescriptions.length);

        // Verify each step with description has a markdown component
        stepsWithDescriptions.forEach((step) => {
            const markdownExists = stepDescriptionMarkdowns.some((markdownEl) => {
                // Verify markdown component exists and has data binding
                return markdownEl.hasAttribute('ng-reflect-data') || markdownEl.hasAttribute('data');
            });
            expect(markdownExists).toBe(true);
        });
    });

    it('should restore progress from localStorage on init', () => {
        localStorage.setItem('dotcmsDeveloperOnboarding', '3');
        const setItemSpy = jest.spyOn(Storage.prototype, 'setItem');

        spectator = createComponent();
        spectator.detectChanges();

        expect(component.state().activeAccordionIndex).toBe(3);
        expect(setItemSpy).toHaveBeenCalled();
    });

    it('should initialize to step 0 if no saved progress exists', () => {
        localStorage.removeItem('dotcmsDeveloperOnboarding');
        const setItemSpy = jest.spyOn(Storage.prototype, 'setItem');

        spectator = createComponent();
        spectator.detectChanges();

        expect(component.state().activeAccordionIndex).toBe(0);
        expect(setItemSpy).toHaveBeenCalledWith('dotcmsDeveloperOnboarding', '0');
    });

    it('should handle invalid localStorage data gracefully', () => {
        localStorage.setItem('dotcmsDeveloperOnboarding', 'invalid');
        // Mock localStorage.getItem to throw an error to test the catch block
        const getItemSpy = jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
            throw new Error('Storage error');
        });
        const removeItemSpy = jest.spyOn(Storage.prototype, 'removeItem');

        spectator = createComponent();
        spectator.detectChanges();

        expect(removeItemSpy).toHaveBeenCalledWith('dotcmsDeveloperOnboarding');
        getItemSpy.mockRestore();
    });

    it('should show framework info overlay when framework is disabled', () => {
        spectator.detectChanges();

        const disabledFramework = component.frameworks.find((f) => f.disabled);
        expect(disabledFramework).toBeDefined();

        if (disabledFramework) {
            const mockEvent = new MouseEvent('click');
            component.showFrameworkInfo(mockEvent, disabledFramework);

            expect(component.selectedFrameworkInfo).toBe(disabledFramework);
        }
    });

    it('should set selectedFramework correctly', () => {
        spectator.detectChanges();

        expect(component.selectedFramework).toBe('nextjs');
    });
});

describe('DotStarterComponent - Server Platform', () => {
    let spectator: Spectator<DotStarterComponent>;
    let component: DotStarterComponent;

    const createServerComponent = createComponentFactory({
        component: DotStarterComponent,
        imports: [
            AccordionModule,
            ButtonCopyComponent,
            ButtonModule,
            KnobModule,
            MarkdownModule.forRoot(),
            NoopAnimationsModule,
            OverlayPanelModule,
            ProgressBarModule,
            RadioButtonModule,
            TagModule,
            TooltipModule
        ],
        providers: [
            {
                provide: PLATFORM_ID,
                useValue: 'server'
            }
        ]
    });

    beforeEach(() => {
        localStorage.clear();
        jest.restoreAllMocks();
        patchState(state, {
            progress: 0,
            activeAccordionIndex: 0,
            currentStateLabel: ''
        });
    });

    beforeEach(() => {
        spectator = createServerComponent({
            detectChanges: false
        });
        component = spectator.component;
    });

    it('should not persist progress when platform is not browser', () => {
        const setItemSpy = jest.spyOn(Storage.prototype, 'setItem');

        // Verify platform ID is server
        expect(spectator.inject(PLATFORM_ID)).toBe('server');

        spectator.detectChanges();

        // Clear any calls from ngOnInit/loadProgress
        setItemSpy.mockClear();

        component.activeIndexChange(1);

        expect(setItemSpy).not.toHaveBeenCalled();
    });
});
