interface OnboardingSubstepExplanation {
    title: string;
    description: string;
}

export type SupportedFrameworks = 'nextjs' | 'angular' | 'angular-ssr' | 'astro';

type SubstepType = 'file' | 'terminal' | 'config';

interface OnboardingStep {
    number: number;
    title: string;
    description: string;
    videoPath?: string;
    substeps?: OnboardingSubstep[];
}

export interface OnboardingSubstep {
    code: string;
    language: string;
    explanation: OnboardingSubstepExplanation;
    type: SubstepType;
    filePath?: string;
}

export interface OnboardingContent {
    title: string;
    description: string;
    steps: OnboardingStep[];
}

export interface OnboardingFramework {
    id: string;
    label: string;
    logo: string;
    disabled?: boolean;
    githubUrl?: string;
}
