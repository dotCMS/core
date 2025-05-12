import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, OnInit } from '@angular/core';

import { RouterLink } from '@angular/router';
import { DotCMSBasicContentlet } from '@dotcms/types';

interface ProductContentlet extends DotCMSBasicContentlet {
    image?: {
        versionPath: string;
    };
    salePrice: number;
    retailPrice: number;
    urlTitle: string;
}

@Component({
    selector: 'app-product',
    standalone: true,
    imports: [RouterLink, NgOptimizedImage],
    template: ` <div class="overflow-hidden bg-white rounded shadow-lg">
        <div class="p-4">
            @if (contentlet().image?.versionPath; as imageVersionPath) {
            <img
                class="w-full"
                [ngSrc]="imageVersionPath"
                width="100"
                height="100"
                alt="Product Image" />
            }
        </div>
        <div class="px-6 py-4 bg-slate-100">
            <div class="mb-2 text-xl font-bold">{{ contentlet().title }}</div>
            @if (contentlet().retailPrice && contentlet().salePrice) {
            <div class="text-gray-500 line-through">{{ retailPrice }}</div>
            <div class="text-3xl font-bold">{{ salePrice }}</div>
            } @else {
            <div class="text-3xl font-bold">
                {{ contentlet()['retailPrice'] ? retailPrice : salePrice }}
            </div>
            }
            <a
                [routerLink]="'/store/products/' + contentlet().urlTitle || '#'"
                class="inline-block px-4 py-2 mt-4 text-white bg-green-500 rounded hover:bg-green-600">
                Buy Now
            </a>
        </div>
    </div>`,
    styleUrl: './product.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductComponent {
    contentlet = input.required<ProductContentlet>();

    protected get salePrice() {
        return this.formatPrice(this.contentlet().salePrice);
    }

    protected get retailPrice() {
        return this.formatPrice(this.contentlet().retailPrice);
    }

    formatPrice(price: number) {
        if (!price || price === null) {
            return '';
        }

        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(price);
    }
}
