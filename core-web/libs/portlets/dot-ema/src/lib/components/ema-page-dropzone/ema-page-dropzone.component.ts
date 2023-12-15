import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

interface Payload {
    container: ContainerPayload;
    contentletId: string;
    pageContainers: PageContainer[];
    pageID: string;
}

interface PageContainer {
    identifier: string;
    uuid: string;
    contentletsId: string[];
}

interface ContainerPayload {
    acceptTypes: string;
    contentletsId: string[];
    identifier: string;
    maxContentlets: number;
    uuid: string;
}

interface Contentlets {
    x: number;
    y: number;
    width: number;
    height: number;
    payload: Payload;
}

interface Container {
    x: number;
    y: number;
    width: number;
    height: number;
    contentlets: Contentlets[];
    payload: Payload;
}

interface Column {
    x: number;
    y: number;
    width: number;
    height: number;
    containers: Container[];
}

export interface Row {
    x: number;
    y: number;
    width: number;
    height: number;
    columns: Column[];
}

@Component({
    selector: 'dot-ema-page-dropzone',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './ema-page-dropzone.component.html',
    styleUrls: ['./ema-page-dropzone.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmaPageDropzoneComponent {
    @Input() rows: Row[] = [];

    getStyle(
        item: Row | Column | Container | Contentlets,
        border = 'black'
    ): Record<string, string> {
        return {
            position: 'absolute',
            left: `${item.x}px`,
            top: `${item.y}px`,
            width: `${item.width}px`,
            height: `${item.height}px`,
            border: `1px solid ${border}`
        };
    }

    onDrop(event: DragEvent): void {
        const payload = JSON.parse((event.target as HTMLDivElement).dataset.payload);
        // eslint-disable-next-line no-console
        console.log('onDrop', payload);
    }

    onDragover(event) {
        event.stopPropagation();
        event.preventDefault();
    }
}
