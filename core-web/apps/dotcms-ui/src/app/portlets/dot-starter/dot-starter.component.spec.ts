import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { ProgressBarModule } from 'primeng/progressbar';
import { MarkdownModule } from 'ngx-markdown';

import { DotStarterComponent } from './dot-starter.component';

describe('DotStarterComponent', () => {
    let fixture: ComponentFixture<DotStarterComponent>;
    let component: DotStarterComponent;

    const createComponent = () => {
        fixture = TestBed.createComponent(DotStarterComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();

        return { fixture, component };
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotStarterComponent],
            imports: [
                AccordionModule,
                ProgressBarModule,
                ButtonModule,
                MarkdownModule.forRoot(),
                NoopAnimationsModule
            ]
        }).compileComponents();
    });

    beforeEach(() => {
        localStorage.clear();
        jest.restoreAllMocks();
    });

    it('should render the onboarding title and prerequisites', () => {
        const { fixture } = createComponent();
        const element = fixture.nativeElement as HTMLElement;

        expect(element.querySelector('.dot-onboarding__title')?.textContent).toContain(
            'dotCMS Headless Integration Onboarding'
        );
        expect(element.querySelectorAll('.dot-onboarding__prerequisites-list li').length).toBeGreaterThan(
            0
        );
    });

    it('should mark a step as completed and update progress', () => {
        const setItemSpy = jest.spyOn(Storage.prototype, 'setItem');
        const { component, fixture } = createComponent();

        component.toggleStepCompletion('step-1');
        fixture.detectChanges();

        expect(component.isStepCompleted('step-1')).toBe(true);
        expect(component.progress).toBe(13);
        expect(setItemSpy).toHaveBeenCalledWith(
            'dotcmsDeveloperOnboarding',
            JSON.stringify(['step-1'])
        );

        const element = fixture.nativeElement as HTMLElement;
        expect(element.querySelector('.dot-step__status')?.textContent).toContain('Completed');
    });

    it('should restore completed steps from local storage', () => {
        localStorage.setItem('dotcmsDeveloperOnboarding', JSON.stringify(['step-1', 'step-2']));

        const { component, fixture } = createComponent();

        expect(component.isStepCompleted('step-1')).toBe(true);
        expect(component.isStepCompleted('step-2')).toBe(true);
        expect(component.hasProgress).toBe(true);

        const element = fixture.nativeElement as HTMLElement;
        const statuses = Array.from(element.querySelectorAll('.dot-step__status')).map((node) =>
            node.textContent?.trim()
        );

        expect(statuses.filter((text) => text === 'Completed').length).toBeGreaterThan(0);
    });

    it('should clear stored progress when reset is invoked', () => {
        const removeSpy = jest.spyOn(Storage.prototype, 'removeItem');
        const { component, fixture } = createComponent();

        component.toggleStepCompletion('step-1');
        fixture.detectChanges();

        component.resetProgress();
        fixture.detectChanges();

        expect(component.isStepCompleted('step-1')).toBe(false);
        expect(removeSpy).toHaveBeenCalledWith('dotcmsDeveloperOnboarding');

        const element = fixture.nativeElement as HTMLElement;
        expect(element.querySelector('.dot-step__status')?.textContent).toContain('In Progress');
    });
});

