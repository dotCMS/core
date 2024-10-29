import { faker } from '@faker-js/faker';

import { DatePipe, NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, signal, OnInit } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { IconFieldModule } from 'primeng/iconfield';
import { ImageModule } from 'primeng/image';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

@Component({
    selector: 'dot-dataview',
    standalone: true,
    imports: [
        DataViewModule,
        TagModule,
        ButtonModule,
        TableModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        SkeletonModule,
        ImageModule,
        NgOptimizedImage,
        DatePipe
    ],
    templateUrl: './dot-dataview.component.html',
    styleUrls: ['./dot-dataview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotDataViewComponent implements OnInit {
    $data = signal([]);
    $loading = signal(false);

    ngOnInit() {
        this.$loading.set(true);
        setTimeout(() => {
            const data = faker.helpers.multiple(
                () => ({
                    id: faker.string.uuid(),
                    image: faker.image.url(),
                    title: faker.commerce.productName(),
                    modifiedBy: faker.internet.displayName(),
                    lastModified: faker.date.recent()
                }),
                { count: 100 }
            );
            this.$data.set(data);
            this.$loading.set(false);
        }, 3000);
    }
}
