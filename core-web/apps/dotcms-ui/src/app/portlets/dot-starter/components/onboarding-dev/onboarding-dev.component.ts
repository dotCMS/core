import { MarkdownModule } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { KnobModule } from 'primeng/knob';
import { ProgressBarModule } from 'primeng/progressbar';
import { RadioButtonModule } from 'primeng/radiobutton';
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
        FormsModule,
        KnobModule,
        MarkdownModule,
        ProgressBarModule,
        RadioButtonModule,
        RouterModule,
        TagModule,
        TooltipModule
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
            cliCommand: 'npx @dotcms/create-app --framework=nextjs'
        },
        {
            id: 'angular',
            label: 'Angular',
            logo: '/dotAdmin/assets/logos/angular.png',
            copied: false,
            disabled: false,
            cliCommand: 'npx @dotcms/create-app --framework=angular',
            githubUrl: 'https://github.com/dotCMS/core/tree/main/examples/angular'
        },
        {
            id: 'angular-ssr',
            label: 'Angular SSR',
            logo: '/dotAdmin/assets/logos/angular.png',
            copied: false,
            cliCommand: 'npx @dotcms/create-app --framework=angular-ssr',
            disabled: false,
            githubUrl: 'https://github.com/dotCMS/core/tree/main/examples/angular-ssr'
        },
        {
            id: 'astro',
            label: 'Astro',
            copied: false,
            logo: '/dotAdmin/assets/logos/astro.svg',
            disabled: false,
            cliCommand: 'npx @dotcms/create-app --framework=astro',
            githubUrl: 'https://github.com/dotCMS/core/tree/main/examples/astro'
        },
        {
            id: '.net',
            label: '.Net',
            logo: '/dotAdmin/assets/logos/dot-net.png',
            disabled: true,
            copied: false,
            cliCommand: '',
            githubUrl: 'https://github.com/dotCMS/dotnet-starter-example'
        },
        {
            id: 'php',
            label: 'PHP',
            logo: '/dotAdmin/assets/logos/php.png',
            copied: false,
            cliCommand: '',
            disabled: true,
            githubUrl: 'https://github.com/dotCMS/dotnet-starter-example'
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
