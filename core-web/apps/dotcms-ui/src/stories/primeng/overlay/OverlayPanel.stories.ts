// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Story, Meta } from '@storybook/angular/types-6-0';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { TableModule } from 'primeng/table';
import { HttpClientModule } from '@angular/common/http';
import { ProductService } from './SharedProducts.service';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { moduleMetadata } from '@storybook/angular';

const OverlayPanelTemplate = `
  <p-button [label]="selectedProduct ? selectedProduct.name : 'Select a Product'" icon="pi pi-search" (click)="op.toggle($event)"></p-button>

  <p-overlayPanel #op [showCloseIcon]="true" [style]="{width: '450px'}">
      <ng-template pTemplate>
          <p-table [value]="products" selectionMode="single" [(selection)]="selectedProduct" [paginator]="true" [rows]="5">
              <ng-template pTemplate="header">
                  <tr>
                      <th pSortableColumn="name">Name<p-sortIcon field="name"></p-sortIcon></th>
                      <th pSortableColumn="price">Price <p-sortIcon field="price"></p-sortIcon></th>
                  </tr>
              </ng-template>
              <ng-template pTemplate="body" let-rowData let-product>
                  <tr [pSelectableRow]="rowData">
                      <td>{{product.name}}</td>
                      <td>{{product.price}}</td>
                  </tr>
              </ng-template>
          </p-table>
      </ng-template>
  </p-overlayPanel>
`;

export default {
    title: 'PrimeNG/Overlay/OverlayPanel',

    decorators: [
        moduleMetadata({
            imports: [
                BrowserModule,
                BrowserAnimationsModule,
                OverlayPanelModule,
                TableModule,
                ButtonModule,
                ToastModule,
                HttpClientModule
            ],
            providers: [ProductService]
        })
    ],
    parameters: {
        docs: {
            description: {
                component:
                    'OverlayPanel is a container component positioned as connected to its target.: https://primefaces.org/primeng/showcase/#/overlaypanel'
            }
        }
    }
} as Meta;

export const Basic: Story = () => {
    return {
        template: OverlayPanelTemplate,
        props: {
            on: () => {
                //
            },
            products: [
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
                    image: 'black-watch.jpg',
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
                    image: 'blue-band.jpg',
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
                    image: 'blue-t-shirt.jpg',
                    price: 29,
                    category: 'Clothing',
                    quantity: 25,
                    inventoryStatus: 'INSTOCK',
                    rating: 5
                },
                {
                    id: '1004',
                    code: 'h456wer53',
                    name: 'Bracelet',
                    description: 'Product Description',
                    image: 'bracelet.jpg',
                    price: 15,
                    category: 'Accessories',
                    quantity: 73,
                    inventoryStatus: 'INSTOCK',
                    rating: 4
                },
                {
                    id: '1005',
                    code: 'av2231fwg',
                    name: 'Brown Purse',
                    description: 'Product Description',
                    image: 'brown-purse.jpg',
                    price: 120,
                    category: 'Accessories',
                    quantity: 0,
                    inventoryStatus: 'OUTOFSTOCK',
                    rating: 4
                },
                {
                    id: '1006',
                    code: 'bib36pfvm',
                    name: 'Chakra Bracelet',
                    description: 'Product Description',
                    image: 'chakra-bracelet.jpg',
                    price: 32,
                    category: 'Accessories',
                    quantity: 5,
                    inventoryStatus: 'LOWSTOCK',
                    rating: 3
                },
                {
                    id: '1007',
                    code: 'mbvjkgip5',
                    name: 'Galaxy Earrings',
                    description: 'Product Description',
                    image: 'galaxy-earrings.jpg',
                    price: 34,
                    category: 'Accessories',
                    quantity: 23,
                    inventoryStatus: 'INSTOCK',
                    rating: 5
                },
                {
                    id: '1008',
                    code: 'vbb124btr',
                    name: 'Game Controller',
                    description: 'Product Description',
                    image: 'game-controller.jpg',
                    price: 99,
                    category: 'Electronics',
                    quantity: 2,
                    inventoryStatus: 'LOWSTOCK',
                    rating: 4
                },
                {
                    id: '1009',
                    code: 'cm230f032',
                    name: 'Gaming Set',
                    description: 'Product Description',
                    image: 'gaming-set.jpg',
                    price: 299,
                    category: 'Electronics',
                    quantity: 63,
                    inventoryStatus: 'INSTOCK',
                    rating: 3
                }
            ]
        }
    };
};

Basic.parameters = {
    docs: {
        source: {
            code: OverlayPanelTemplate
        },
        iframeHeight: 500
    }
};
