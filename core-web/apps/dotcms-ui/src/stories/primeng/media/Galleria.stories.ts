import { Meta, StoryObj, moduleMetadata } from '@storybook/angular';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Galleria, GalleriaModule } from 'primeng/galleria';

const images = [
    { url: 'https://primefaces.org/cdn/primeng/images/demo/product/bamboo-watch.jpg' },
    { url: 'https://primefaces.org/cdn/primeng/images/demo/product/black-watch.jpg' },
    { url: 'https://primefaces.org/cdn/primeng/images/demo/product/blue-band.jpg' },
    { url: 'https://primefaces.org/cdn/primeng/images/demo/product/blue-t-shirt.jpg' },
    { url: 'https://primefaces.org/cdn/primeng/images/demo/product/bracelet.jpg' }
];

const templateGalleria = `
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

const meta: Meta<Galleria> = {
    title: 'PrimeNG/Media/Galleria',
    component: Galleria,
    decorators: [
        moduleMetadata({
            imports: [GalleriaModule, BrowserAnimationsModule]
        })
    ],
    args: {
        activeIndex: 0,
        circular: true,
        showItemNavigators: true,
        value: [...images]
    },
    parameters: {
        docs: {
            description: {
                component: 'Galleria is an advanced content gallery component.'
            }
        }
    },
    render: (args) => ({
        props: args,
        template: templateGalleria
    })
};
export default meta;

type Story = StoryObj<Galleria>;

export const Main: Story = {};
