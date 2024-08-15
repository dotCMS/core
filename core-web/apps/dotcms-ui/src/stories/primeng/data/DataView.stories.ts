import { Meta, moduleMetadata, StoryObj, argsToTemplate } from '@storybook/angular';

import { DataView, DataViewModule } from 'primeng/dataview';

type Data = {
    id: string;
    name: string;
    description: string;
    image: string;
    price: string;
    category: string;
};

const data: Data[] = [
    {
        id: '1000',
        name: 'Apple iPhone 12 Pro',
        description: 'Apple iPhone 12 Pro 128GB Graphite',
        image: 'assets/demo/images/product/iphone.jpg',
        price: '$999.99',
        category: 'Cell Phones'
    },
    {
        id: '1001',
        name: 'Apple iPhone 12 Pro',
        description: 'Apple iPhone 12 Pro 256GB Graphite',
        image: 'assets/demo/images/product/iphone.jpg',
        price: '$1099.99',
        category: 'Cell Phones'
    },
    {
        id: '1002',
        name: 'Apple iPhone 12 Pro',
        description: 'Apple iPhone 12 Pro 512GB Graphite',
        image: 'assets/demo/images/product/iphone.jpg',
        price: '$1299.99',
        category: 'Cell Phones'
    },
    {
        id: '1003',
        name: 'Samsung Galaxy S21 Ultra',
        description: 'Samsung Galaxy S21 Ultra 128GB Phantom Black',
        image: 'assets/demo/images/product/samsung.jpg',
        price: '$1199.99',
        category: 'Cell Phones'
    },
    {
        id: '1004',
        name: 'Samsung Galaxy S21 Ultra',
        description: 'Samsung Galaxy S21 Ultra 256GB Phantom Black',
        image: 'assets/demo/images/product/samsung.jpg',
        price: '$1299.99',
        category: 'Cell Phones'
    },
    {
        id: '1005',
        name: 'Samsung Galaxy S21 Ultra',
        description: 'Samsung Galaxy S21 Ultra 512GB Phantom Black',
        image: 'assets/demo/images/product/samsung.jpg',
        price: '$1499.99',
        category: 'Cell Phones'
    },
    {
        id: '1006',
        name: 'Apple MacBook Pro 13',
        description: 'Apple MacBook Pro 13-inch 256GB Space Gray',
        image: 'assets/demo/images/product/macbook.jpg',
        price: '$1299.99',
        category: 'Laptops'
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
                                    <img class="block xl:block mx-auto border-round w-full" [src]="item.image" [alt]="item.name" />
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
