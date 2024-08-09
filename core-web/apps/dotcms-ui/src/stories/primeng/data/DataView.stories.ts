import { Meta, moduleMetadata, StoryObj, argsToTemplate } from '@storybook/angular';

import { DataView, DataViewModule } from 'primeng/dataview';

const data = [
    {
        id: '1000',
        code: 'f230fh0g3',
        name: 'Bamboo Watch',
        description: 'Product Description',
        image: 'bamboo-watch.jpg',
        price: 65,
        category: 'Accessories',
        quantity: 24,
        inventoryStatus: 'INSTOCK',
        rating: 5
    },
    {
        id: '1001',
        code: 'nvklal433',
        name: 'Black Watch',
        description: 'Product Description',
        image: 'bamboo-watch.jpg',
        price: 72,
        category: 'Accessories',
        quantity: 61,
        inventoryStatus: 'INSTOCK',
        rating: 4
    },
    {
        id: '1002',
        code: 'zz21cz3c1',
        name: 'Blue Band',
        description: 'Product Description',
        image: 'bamboo-watch.jpg',
        price: 79,
        category: 'Fitness',
        quantity: 2,
        inventoryStatus: 'LOWSTOCK',
        rating: 3
    },
    {
        id: '1003',
        code: '244wgerg2',
        name: 'Blue T-Shirt',
        description: 'Product Description',
        image: 'bamboo-watch.jpg',
        price: 29,
        category: 'Clothing',
        quantity: 25,
        inventoryStatus: 'INSTOCK',
        rating: 5
    }
];

const meta: Meta<DataView> = {
    title: 'PrimeNG/Data/DataView',
    decorators: [
        moduleMetadata({
            imports: [DataViewModule]
        })
    ],
    parameters: {
        docs: {
            description: {
                component: 'https://primeng.org/dataview'
            }
        }
    },
    render: (args) => {
        return {
            props: { ...args },
            template: `
            <p-dataView ${argsToTemplate(args)}>
                <ng-template pTemplate="list" let-products>
                    <div class="grid grid-nogutter">
                         <div class="col-12" *ngFor="let item of products">
                            <div class="flex flex-column sm:flex-row sm:align-items-center p-4 gap-3">
                                <div class="md:w-10rem relative">
                                    <img class="block xl:block mx-auto border-round w-full" [src]="'https://primefaces.org/cdn/primeng/images/demo/product/' + item.image" [alt]="item.name" />
                                </div>
                                <div>
                                    <span class="font-medium text-secondary text-sm">{{ item.category }}</span>
                                    <div class="text-lg font-medium text-900 mt-2">{{ item.name }}</div>
                                </div>
                            </div>
                         </div>
                    </div>
                </ng-template>
            </p-dataView>`
        };
    }
};
export default meta;

type Story = StoryObj<DataView>;

export const Primary: Story = {
    args: {
        value: [...data],
        rows: 3,
        paginator: true
    }
};
