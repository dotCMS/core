import { Component, OnInit, inject } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { Product } from './Product.interface';
import { ProductService } from './SharedProducts.service';

export const ProductsTableTemplate = `
  <p-table [value]="products" [paginator]="true" [rows]="5" [responsive]="true">
      <ng-template pTemplate="header">
          <tr>
              <th pSortableColumn="name">Name <p-sortIcon field="vin"></p-sortIcon></th>
              <th pSortableColumn="price">Brand <p-sortIcon field="price"></p-sortIcon></th>
              <th pSortableColumn="inventoryStatus">Status <p-sortIcon field="inventoryStatus"></p-sortIcon></th>
              <th style="width:4em"></th>
          </tr>
      </ng-template>
      <ng-template pTemplate="body" let-product>
          <tr>
              <td>{{product.name}}</td>
              <td>{{product.price}}</td>
              <td><span [class]="'product-badge status-'+product.inventoryStatus.toLowerCase()">{{product.inventoryStatus}}</span></td>
              <td>
                  <button type="button" pButton icon="pi pi-search" (click)="selectProduct(product)"></button>
              </td>
          </tr>
      </ng-template>
  </p-table>
`;

@Component({
    selector: 'dot-p-dynamic-dialog',
    template: ProductsTableTemplate
})
export class DynamicDialogProductsComponent implements OnInit {
    private productService = inject(ProductService);
    ref = inject(DynamicDialogRef);
    config = inject(DynamicDialogConfig);

    products: Product[];

    ngOnInit() {
        this.productService.getProductsSmall().then((products) => (this.products = products));
    }

    selectProduct(product: Product) {
        this.ref.close(product);
    }
}
