import { patchState } from '@ngrx/signals';

import { isPlatformBrowser } from '@angular/common';
import { Component, ElementRef, OnInit, PLATFORM_ID, ViewChild, inject } from '@angular/core';

import { OverlayPanel } from 'primeng/overlaypanel';

import { state } from './store';

interface OnboardingSubstepExplanation {
    title: string;
    description: string;
}

type SubstepType = 'file' | 'terminal' | 'config';

interface OnboardingSubstep {
    code: string;
    language: string;
    explanation: OnboardingSubstepExplanation;
    type: SubstepType;
    filePath?: string;
}

interface OnboardingStep {
    number: number;
    title: string;
    description: string;
    substeps?: OnboardingSubstep[];
}

interface OnboardingContent {
    title: string;
    description: string;
    steps: OnboardingStep[];
}

const STORAGE_KEY = 'dotcmsDeveloperOnboarding';

const ONBOARDING_CONTENT: OnboardingContent = {
    title: 'Build Your First dotCMS Headless Application',
    description:
        'Select your preferred framework to get started. Learn how to connect to dotCMS, handle authentication, and enable visual editing in under 30 minutes',
    steps: [
        {
            number: 1,
            title: 'Set up your Next.js app',
            description:
                'Set up a modern Next.js development environment with TypeScript and Tailwind CSS that will serve as the foundation for your headless CMS application.',
            substeps: [
                {
                    code: 'npx create-next-app@latest my-dotcms-app --yes',
                    language: 'bash',
                    type: 'terminal',
                    explanation: {
                        title: 'Create a new Next.js project with all defaults',
                        description: `The \`--yes\` flag creates a Next.js app with:
- TypeScript support
- Tailwind CSS for styling
- ESLint for code quality
- App Router (not Pages Router)
- Turbopack for faster builds
- Import alias \`@/*\` for cleaner imports
- \`src\` directory structure`
                    }
                },
                {
                    code: 'cd my-dotcms-app',
                    language: 'bash',
                    type: 'terminal',
                    explanation: {
                        title: 'Navigate into the project directory',
                        description:
                            'Change into the newly created project directory so you can run commands within it.'
                    }
                },
                {
                    code: 'npm run dev',
                    language: 'bash',
                    type: 'terminal',
                    explanation: {
                        title: 'Start the development server',
                        description: `The Next.js development server will start and the app will be available at http://localhost:3000. Keep this terminal running while developing.`
                    }
                }
            ]
        },
        {
            number: 2,
            title: 'Install dotCMS libraries',
            description:
                'Install the official dotCMS SDK packages that enable your Next.js app to communicate with dotCMS and render content with full TypeScript support.',
            substeps: [
                {
                    code: 'npm install @dotcms/client @dotcms/react @dotcms/types',
                    language: 'bash',
                    type: 'terminal',
                    explanation: {
                        title: 'Install dotCMS packages',
                        description: `- \`@dotcms/client\` - Handles authentication, API communication, and content fetching from dotCMS
- \`@dotcms/react\` - Provides React-specific hooks and components for rendering dotCMS content
- \`@dotcms/types\` - Provides TypeScript type definitions for dotCMS SDK`
                    }
                }
            ]
        },
        {
            number: 3,
            title: 'Authenticate dotCMS (create your API Key)',
            description:
                'Set up secure authentication between your Next.js app and dotCMS using a read-only API key and environment variables.',
            substeps: [
                {
                    code: 'touch .env.local',
                    language: 'bash',
                    type: 'terminal',
                    explanation: {
                        title: 'Create .env.local file in project root',
                        description: `To generate your API key:
- Navigate to [**System** → **Users**](https://minstarter.dotcms.com/c/users) in your dotCMS instance
- Select your user account (e.g., \`admin@dotcms.com\`)
- Scroll to the **API Access Key** section
- Click **Generate** to create a new key (read-only permissions recommended)
- Copy the generated key - it will look something like: \`abcd1234efgh5678ijkl9012mnop3456\`

**Important:** Save this key safely - you'll need it in the next step!

For detailed instructions, refer to: [dotCMS REST API Authentication](https://dev.dotcms.com/docs/rest-api-authentication#ReadOnlyToken)`
                    }
                },
                {
                    code: `NEXT_PUBLIC_DOTCMS_URL=https://minstarter.dotcms.com
DOTCMS_TOKEN=your-api-key-here`,
                    language: 'env',
                    type: 'file',
                    filePath: '.env.local',
                    explanation: {
                        title: 'Add your dotCMS credentials',
                        description: `Replace \`your-api-key-here\` with the actual API Key you copied from dotCMS.

- \`NEXT_PUBLIC_DOTCMS_URL\` - Available in both server and client components (needed for image URLs)

- \`DOTCMS_TOKEN\` - Server-only (keeps your API key secure, never exposed to browser)`
                    }
                }
            ]
        },
        {
            number: 4,
            title: 'Fetch content from dotCMS',
            description:
                'Test your dotCMS connection by fetching page data and displaying it as JSON. Seeing the raw data helps you understand the structure of dotCMS content before rendering it visually.',
            substeps: [
                {
                    code: `import { createDotCMSClient } from '@dotcms/client';

// Create dotCMS client
const client = createDotCMSClient({
  dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_URL!,
  authToken: process.env.DOTCMS_TOKEN,
});

export default async function Home() {
  // Fetch page content from dotCMS
  const { pageAsset } = await client.page.get('/');

  return (
    <div className="min-h-screen p-8">
      <h1 className="text-2xl font-bold mb-4">dotCMS Page Data</h1>
      <pre className="bg-gray-100 p-4 rounded overflow-auto text-xs">
        {JSON.stringify(pageAsset, null, 2)}
      </pre>
    </div>
  );
}`,
                    language: 'typescript',
                    type: 'file',
                    filePath: 'src/app/page.tsx',
                    explanation: {
                        title: 'Replace all content in the page component',
                        description: `- \`createDotCMSClient\` - Creates a configured client instance that can communicate with your dotCMS instance
- \`client.page.get('/')\` - Makes an API call to fetch the home page content (identified by path \`/\`)
- \`pageAsset\` - The complete page object containing all content, layout, and configuration data
- \`JSON.stringify\` - Converts the JavaScript object into readable JSON format for inspection`
                    }
                }
            ]
        },
        {
            number: 5,
            title: 'Render content with DotCMSLayoutBody',
            description:
                'Transform the raw JSON data into a beautiful, interactive page. You will create a client-side component that automatically maps dotCMS content types to React components.',
            substeps: [
                {
                    code: 'mkdir -p src/components && touch src/components/DotCMSPageClient.tsx',
                    language: 'bash',
                    type: 'terminal',
                    explanation: {
                        title: 'Create components directory',
                        description: `We need a dedicated place for our client-side components.`
                    }
                },
                {
                    code: `'use client';

import { DotCMSLayoutBody } from '@dotcms/react';
import type { DotCMSPageAsset } from '@dotcms/types';
import Image from 'next/image';

// 1. Define the Banner Component
// This component matches the fields defined in your dotCMS "Banner" Content Type
function Banner({ title, caption, image, link, target }: any) {
  const dotcmsUrl = process.env.NEXT_PUBLIC_DOTCMS_URL;
  const imageUrl = image?.idPath ? \`\${dotcmsUrl}\${image.idPath}\` : null;

  return (
    <div className="relative w-full bg-gray-900 text-white overflow-hidden min-h-[400px] flex items-center rounded-xl my-8">
      <div className="container mx-auto px-8 relative z-10">
        <div className="max-w-2xl">
          {title && <h1 className="text-5xl font-bold mb-6">{title}</h1>}
          {caption && <p className="text-xl mb-8 text-gray-200">{caption}</p>}
          {link && (
            <a href={link} target={target} className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-8 rounded transition-colors">
              Learn More
            </a>
          )}
        </div>
      </div>
      {imageUrl && (
        <div className="absolute inset-0 opacity-40">
          {/* 'unoptimized' allows loading images from external dotCMS domains without extra Next.js config */}
          <Image src={imageUrl} alt={title || 'Banner'} fill className="object-cover" unoptimized />
        </div>
      )}
    </div>
  );
}

// 2. Create the Component Map
// Keys must EXACTLY match the Content Type Variable Name in dotCMS
const COMPONENTS_MAP = {
  Banner: Banner,
};

// 3. Main Client Component
export function DotCMSPageClient({ pageAsset }: { pageAsset: DotCMSPageAsset }) {
  return (
    <DotCMSLayoutBody
      page={pageAsset}
      components={COMPONENTS_MAP}
    />
  );
}`,
                    language: 'typescript',
                    type: 'file',
                    filePath: 'src/components/DotCMSPageClient.tsx',
                    explanation: {
                        title: 'Create the Component Mapper',
                        description: `This file handles the logic of turning data into UI:

- **Banner Component**: A standard React component. Note the \`unoptimized\` prop on the Image—this allows Next.js to display images from your specific dotCMS instance URL without complex configuration changes.
- **COMPONENTS_MAP**: This object tells the SDK "When you see content of type 'Banner', render this React component."
- **DotCMSLayoutBody**: The magic component from the SDK that iterates over the page layout and renders the correct components automatically.`
                    }
                },
                {
                    code: `import { createDotCMSClient } from '@dotcms/client';
import { DotCMSPageClient } from '@/components/DotCMSPageClient';

const client = createDotCMSClient({
  dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_URL!,
  authToken: process.env.DOTCMS_TOKEN,
});

export default async function Home() {
  const { pageAsset } = await client.page.get('/');

  return (
    <main className="container mx-auto px-4">
      <DotCMSPageClient pageAsset={pageAsset} />
    </main>
  );
}`,
                    language: 'typescript',
                    type: 'file',
                    filePath: 'src/app/page.tsx',
                    explanation: {
                        title: 'Update the Home Page',
                        description: `We replace the raw JSON dump with our new \`<DotCMSPageClient />\`.

The server fetches the data securely, and the client component renders it. This "Hybrid" approach gives you the best of both worlds: SEO performance and interactive UI.`
                    }
                }
            ]
        },
        {
            number: 6,
            title: 'Configure Universal Visual Editor',
            description:
                'Now connect the two worlds. We need to tell dotCMS to load your local development environment (`localhost:3000`) inside its visual editor instead of the production site.',
            substeps: [
                {
                    code: `{
  "config": [
    {
      "pattern": ".*",
      "url": "http://localhost:3000"
    }
  ]
}`, // No code needed, purely visual
                    language: 'json',
                    type: 'config', // UI placeholder
                    explanation: {
                        title: 'Open the Visual Editor',
                        description: `1. In dotCMS, navigate to **Settings > Apps** and click on **UVE - Universal Visual Editor**
2. Click the plus button at the right of the site we're integrating (i.e., \`demo.dotcms.com\`).
3. In the Configuration field, add the JSON object.`
                    }
                }
            ]
        },
        {
            number: 7,
            title: 'Edit your page visually',
            description:
                'The moment of truth. You will now edit content in dotCMS and see it update live in your Next.js application without touching the code.',
            substeps: [
                {
                    code: '', // No code needed, purely visual
                    language: 'text',
                    type: 'terminal',
                    explanation: {
                        title: 'Open the Visual Editor',
                        description: `1. Go to **Site Browser** → **Pages** in the dotCMS sidebar.
2. Click on the **Home** page (index).
3. The screen will split: dotCMS controls on the right, your Next.js app on the left.
4. Click the **Edit** (pencil) icon on the top right.
5. Click directly on the **Banner** component in the preview.
6. Change the **Title** text and press **Save**.

Watch your Next.js app update instantly!`
                    }
                }
            ]
        },
        {
            number: 8,
            title: 'You Did It!',
            description:
                'Congratulations! You have successfully built a Headless Next.js app with full visual editing capabilities. You now have a workflow where developers build components in React, and editors manage content visually.'
        }
    ]
};

@Component({
    selector: 'dot-starter',
    templateUrl: './dot-starter.component.html',
    styleUrls: ['./dot-starter.component.scss'],
    standalone: false
})
export class DotStarterComponent implements OnInit {
    readonly state = state;
    readonly content = ONBOARDING_CONTENT;

    selectedFramework = 'nextjs';
    frameworks = [
        {
            id: 'nextjs',
            label: 'Next.js',
            logo: 'assets/logos/nextjs.svg'
        },
        {
            id: 'angular',
            label: 'Angular',
            logo: 'assets/logos/angular.png',
            disabled: true,
            githubUrl: 'https://github.com/dotCMS/core/tree/main/examples/angular'
        },
        {
            id: 'angular-ssr',
            label: 'Angular SSR',
            logo: 'assets/logos/angular.png',
            disabled: true,
            githubUrl: 'https://github.com/dotCMS/core/tree/main/examples/angular-ssr'
        },
        {
            id: 'astro',
            label: 'Astro',
            logo: 'assets/logos/astro.svg',
            disabled: true,
            githubUrl: 'https://github.com/dotCMS/core/tree/main/examples/astro'
        },
        {
            id: 'php',
            label: 'PHP',
            logo: 'assets/logos/php.png',
            disabled: true,
            githubUrl: 'https://github.com/dotCMS/dotnet-starter-example'
        },
        {
            id: 'dotnet',
            label: '.NET',
            logo: 'assets/logos/dot-net.png',
            disabled: true,
            githubUrl: 'https://github.com/dotCMS/dotnet-starter-example'
        }
    ];

    @ViewChild('onboardingContainer', { read: ElementRef })
    onboardingContainer?: ElementRef<HTMLElement>;
    @ViewChild('frameworkInfoOverlay') frameworkInfoOverlay?: OverlayPanel;
    selectedFrameworkInfo?: {
        id: string;
        label: string;
        logo: string;
        disabled?: boolean;
        githubUrl?: string;
    };
    private readonly platformId = inject(PLATFORM_ID);

    ngOnInit(): void {
        this.loadProgress();
    }

    activeIndexChange(index: number) {
        patchState(state, (state) => ({
            ...state,
            activeAccordionIndex: index
        }));

        this.persistProgress();
        this.updateProgress();

        setTimeout(() => {
            if (!isPlatformBrowser(this.platformId) || !this.onboardingContainer?.nativeElement) {
                return;
            }
            const container = this.onboardingContainer.nativeElement;
            const activeTab = container.querySelector('.p-accordion-tab-active .p-accordion-header') as HTMLElement;

            if (activeTab) {
                activeTab.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        }, 500);
    }

    showFrameworkInfo(
        event: Event,
        framework: {
            id: string;
            label: string;
            logo: string;
            disabled?: boolean;
            githubUrl?: string;
        }
    ): void {
        this.selectedFrameworkInfo = framework;
        this.frameworkInfoOverlay?.toggle(event);
    }

    isStepCompleted(index: number): boolean {
        return index <= state.activeAccordionIndex();
    }

    completeStepAndOpenNext(index: number): void {
        const nextIndex = index + 1;
        this.activeIndexChange(nextIndex);
    }

    resetProgress(): void {
        this.activeIndexChange(0);
    }

    get progressPercentage(): number {
        if (!this.totalSteps) {
            return 0;
        }

        return ((state.activeAccordionIndex() + 1) / (this.totalSteps)) * 100;
    }

    get totalSteps(): number {
        return this.content.steps.length;
    }

    get currentStep(): OnboardingStep | null {
        if (!this.totalSteps) {
            return null;
        }

        return this.content.steps[state.activeAccordionIndex()];
    }

    get currentStepLabel(): string {
        if (!this.totalSteps) {
            return 'No steps available';
        }

        if (state.activeAccordionIndex() >= this.totalSteps) {
            return 'All steps complete';
        }

        const step = this.currentStep;

        if (!step) {
            return 'All steps complete';
        }

        return step.title;
    }

    get currentStepPosition(): number {
        return state.activeAccordionIndex();
    }

    get hasProgress(): boolean {
        return state.activeAccordionIndex() > 0;
    }

    formatSubstep(substep: OnboardingSubstep): string {
        const language = substep.language || '';

        return `\`\`\`${language}
${substep.code}
\`\`\``;
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

    private updateProgress(): void {
        patchState(state, (state) => ({
            ...state,
            progress: Math.round(this.progressPercentage)
        }));
    }
}
