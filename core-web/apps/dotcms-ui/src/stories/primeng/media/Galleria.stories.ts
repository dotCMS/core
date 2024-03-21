import { Meta, StoryFn } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Galleria, GalleriaModule } from 'primeng/galleria';

export default {
    title: 'PrimeNG/Media/Galleria',
    component: Galleria,
    args: { activeIndex: 0, circular: true, showItemNavigators: true },
    parameters: {
        docs: {
            description: {
                component: 'Galleria is an advanced content gallery component.'
            }
        }
    }
} as Meta;

const Template = `
    <p-galleria [value]="images" 
    [activeIndex]="activeIndex" 
    [circular]="circular" 
    [showItemNavigators]="showItemNavigators" 
    [showThumbnails]="false"
    [containerStyle]="'width: 400px'">
        <ng-template pTemplate="item" let-item>
                <img [src]="item.url" style="max-height: 400px" alt="Image Description" />
        </ng-template>
    </p-galleria>`;

export const Main: StoryFn = (args) => {
    return {
        props: {
            images: [
                {
                    url: 'https://picsum.photos/600/900'
                },
                {
                    url: 'https://picsum.photos/900/600'
                },
                {
                    url: 'https://picsum.photos/600/600'
                }
            ],
            activeIndex: args.activeIndex,
            circular: args.circular,
            showItemNavigators: args.showItemNavigators
        },
        moduleMetadata: { imports: [GalleriaModule, BrowserAnimationsModule] },
        template: Template
    };
};
