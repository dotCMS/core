import { patchState } from '@ngrx/signals';
import { MarkdownModule } from 'ngx-markdown';

import { isPlatformBrowser, CommonModule } from '@angular/common';
import {
    Component,
    ElementRef,
    OnInit,
    PLATFORM_ID,
    ViewChild,
    inject,
    Output,
    EventEmitter
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { KnobModule } from 'primeng/knob';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';
import { ProgressBarModule } from 'primeng/progressbar';
import { RadioButtonModule } from 'primeng/radiobutton';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { ButtonCopyComponent } from '@dotcms/ui';

import { getOnboardingContent, STORAGE_KEY } from './content';
import { OnboardingFramework, OnboardingSubstep, SupportedFrameworks } from './models';
import { state } from './store';

@Component({
    selector: 'dot-onboarding-dev',
    templateUrl: './onboarding-dev.component.html',
    styleUrls: ['./onboarding-dev.component.scss'],
    imports: [
        AccordionModule,
        ButtonCopyComponent,
        ButtonModule,
        CommonModule,
        FormsModule,
        KnobModule,
        MarkdownModule,
        OverlayPanelModule,
        ProgressBarModule,
        RadioButtonModule,
        RouterModule,
        TagModule,
        TooltipModule
    ]
})
export class DotOnboardingDevComponent implements OnInit {
    readonly state = state;

    @Output() eventEmitter = new EventEmitter<'reset-user-profile'>();

    selectedFramework: SupportedFrameworks = 'nextjs';
    content = getOnboardingContent(this.selectedFramework);
    frameworks: OnboardingFramework[] = [
        {
            id: 'nextjs',
            label: 'Next.js',
            logo: '/dotAdmin/assets/logos/nextjs.svg'
        },
        {
            id: 'angular',
            label: 'Angular',
            logo: '/dotAdmin/assets/logos/angular.png',
            disabled: false
            // githubUrl: 'https://github.com/dotCMS/core/tree/main/examples/angular'
        },
        {
            id: 'angular-ssr',
            label: 'Angular SSR',
            logo: '/dotAdmin/assets/logos/angular.png',
            disabled: false
            // githubUrl: 'https://github.com/dotCMS/core/tree/main/examples/angular-ssr'
        },
        {
            id: 'astro',
            label: 'Astro',
            logo: '/dotAdmin/assets/logos/astro.svg',
            disabled: false
            // githubUrl: 'https://github.com/dotCMS/core/tree/main/examples/astro'
        }
    ];

    @ViewChild('onboardingContainer', { read: ElementRef })
    onboardingContainer?: ElementRef<HTMLElement>;
    @ViewChild('frameworkInfoOverlay') frameworkInfoOverlay?: OverlayPanel;
    selectedFrameworkInfo?: OnboardingFramework;
    private readonly platformId = inject(PLATFORM_ID);

    ngOnInit(): void {
        this.loadProgress();
    }

    formatSubstep(substep: OnboardingSubstep): string {
        const language = substep.language || '';

        return `\`\`\`${language}
${substep.code}
\`\`\``;
    }

    activeIndexChange(newIndex: number) {
        const totalSteps = this.content.steps.length;

        // newIndex is 0-based, so we need to subtract 1 to get the correct progress
        const progress = Math.round((newIndex / (totalSteps - 1)) * 100);

        let title = 'All steps complete';

        if (!totalSteps) {
            title = 'No steps available';
        }

        if (this.content.steps[newIndex]) {
            title = this.content.steps[newIndex].title;
        }

        patchState(state, (state) => ({
            ...state,
            activeAccordionIndex: newIndex,
            progress,
            currentStateLabel: title
        }));

        this.persistProgress();

        setTimeout(() => {
            this.scrollToActiveTab();
        }, 500);
    }

    showFrameworkInfo(event: Event, framework: OnboardingFramework): void {
        this.selectedFrameworkInfo = framework;
        this.frameworkInfoOverlay?.toggle(event);
    }

    isStepCompleted(index: number): boolean {
        return index <= state.activeAccordionIndex();
    }

    private loadProgress(): void {
        if (!isPlatformBrowser(this.platformId)) {
            return;
        }

        try {
            const saved = localStorage.getItem(STORAGE_KEY);

            if (!saved) {
                this.activeIndexChange(0);
                return;
            }

            this.activeIndexChange(parseInt(saved));
        } catch {
            localStorage.removeItem(STORAGE_KEY);
        }
    }

    private persistProgress(): void {
        if (!isPlatformBrowser(this.platformId)) {
            return;
        }

        localStorage.setItem(STORAGE_KEY, state.activeAccordionIndex().toString());
    }

    private scrollToActiveTab(): void {
        if (!isPlatformBrowser(this.platformId) || !this.onboardingContainer?.nativeElement) {
            return;
        }
        const container = this.onboardingContainer.nativeElement;
        const activeTab = container.querySelector(
            '.p-accordion-tab-active .p-accordion-header'
        ) as HTMLElement;
        activeTab.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
        });
    }

    public resetUserProfile(): void {
        localStorage.removeItem('user_profile');
        this.eventEmitter.emit('reset-user-profile');
    }

    public changeSelectedFramework(event: Event): void {
        const value: SupportedFrameworks = (event.target as HTMLInputElement)
            .value as SupportedFrameworks;
        this.content = getOnboardingContent(value);
    }
}
