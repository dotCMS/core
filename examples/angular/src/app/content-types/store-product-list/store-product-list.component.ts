import { Component, computed, input } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';
import { Product, StoreProductList } from '../../shared/contentlet.model';

@Component({
    selector: 'app-store-product-list',
    standalone: true,
    imports: [NgOptimizedImage],
    templateUrl: './store-product-list.component.html'
})
export class StoreProductListComponent {
    contentlet = input.required<StoreProductList>();

    products = computed(() => this.contentlet().widgetCodeJSON?.products || []);

    hasProducts = computed(() => this.products() && this.products().length > 0);

    hasDiscount(product: Product): boolean {
        const { salePrice, retailPrice } = product;
        return Boolean(salePrice && retailPrice && Number(salePrice) < Number(retailPrice));
    }

    discountPercentage(product: Product): number {
        const { salePrice, retailPrice } = product;
        return Math.round((1 - Number(salePrice) / Number(retailPrice)) * 100);
    }

    onProductClick(title: string): void {
        alert('Selected Product: ' + title);
    }
}
