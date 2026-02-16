import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { Component, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { KnobModule } from 'primeng/knob';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { ProgressBarModule } from 'primeng/progressbar';
import { RadioButtonModule } from 'primeng/radiobutton';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { DotCopyButtonComponent } from '@dotcms/ui';

import { OnboardingFramework } from './models';

@Component({
    selector: 'dot-onboarding-dev',
    templateUrl: './onboarding-dev.component.html',
    styleUrls: ['./onboarding-dev.component.scss'],
    imports: [
        AccordionModule,
        DotCopyButtonComponent,
        ButtonModule,
        CommonModule,
        ButtonModule,
        FormsModule,
        KnobModule,
        MarkdownModule,
        OverlayPanelModule,
        ProgressBarModule,
        RadioButtonModule,
        RouterModule,
        TagModule,
        TooltipModule,
        TabViewModule
    ]
})
export class DotOnboardingDevComponent {
    @Output() eventEmitter = new EventEmitter<'reset-user-profile'>();
    frameworks: OnboardingFramework[] = [
        {
            id: 'nextjs',
            label: 'Next.js',
            copied: false,
            logo: '/dotAdmin/assets/logos/nextjs.svg',
            type: 'interactive',
            cliCommand: 'npx @dotcms/create-app --framework=nextjs'
        },
        {
            id: 'angular',
            label: 'Angular',
            type: 'interactive',
            logo: '/dotAdmin/assets/logos/angular.png',
            copied: false,
            cliCommand: 'npx @dotcms/create-app --framework=angular',
            githubUrl: 'https://github.com/dotCMS/core/tree/main/examples/angular'
        },
        {
            id: 'angular-ssr',
            label: 'Angular SSR',
            type: 'interactive',
            logo: '/dotAdmin/assets/logos/angular.png',
            copied: false,
            cliCommand: 'npx @dotcms/create-app --framework=angular-ssr',
            githubUrl: 'https://github.com/dotCMS/core/tree/main/examples/angular-ssr'
        },
        {
            id: 'astro',
            label: 'Astro',
            copied: false,
            type: 'interactive',
            logo: '/dotAdmin/assets/logos/astro.svg',
            cliCommand: 'npx @dotcms/create-app --framework=astro',
            githubUrl: 'https://github.com/dotCMS/core/tree/main/examples/astro'
        },
        {
            id: '.net',
            label: '.Net',
            logo: '/dotAdmin/assets/logos/dot-net.png',
            type: 'starter',
            copied: false,
            cliCommand: '',
            githubUrl: 'https://github.com/dotCMS/dotnet-starter-example'
        },
        {
            id: 'php',
            label: 'PHP',
            type: 'starter',
            logo: '/dotAdmin/assets/logos/php.png',
            copied: false,
            cliCommand: '',
            githubUrl: 'https://github.com/dotCMS/dotnet-starter-example'
        },
        {
            id: 'vtl',
            label: 'VTL',
            logo: '/dotAdmin/assets/logos/vtl.svg',
            copied: false,
            type: 'doc',
            cliCommand: '',
            githubUrl: 'https://dev.dotcms.com/docs/velocity-viewtools'
        }
    ];

    public resetUserProfile(): void {
        localStorage.removeItem('user_profile');
        this.eventEmitter.emit('reset-user-profile');
    }
    public openExternalLink(url: string): void {
        window.open(url, '_blank');
    }

    public copyToClipboard(framework: OnboardingFramework): void {
        if (!framework.cliCommand) return;
        navigator.clipboard
            .writeText(framework.cliCommand)
            .catch((err) => console.error('Failed to copy:', err));
    }
}
