export interface OnboardingFramework {
    id: string;
    label: string;
    logo: string;
    cliCommand: string;
    disabled?: boolean;
    copied: boolean;
    githubUrl?: string;
}
