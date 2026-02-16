export type OnboardingFrameworkType = 'interactive' | 'starter' | 'doc';

export interface OnboardingFramework {
    id: string;
    label: string;
    logo: string;
    cliCommand: string;
    type: OnboardingFrameworkType;
    copied: boolean;
    githubUrl?: string;
}
