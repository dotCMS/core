import { Directive, HostListener, NgZone, Output, EventEmitter, ElementRef } from '@angular/core';
import { DotUploadFile } from '../models/dot-upload-file.model';
import { FileSystemEntry, FileSystemDirectoryEntry } from '../models/file-system.model';

const OVER_CLASS = 'over';

@Directive({
    selector: '[dotDotDndFilesFolders]'
})
export class DotDndFilesFoldersDirective {
    files: DotUploadFile[] = [];

    @Output() itemDropped: EventEmitter<DotUploadFile[]> = new EventEmitter();

    constructor(private zone: NgZone, private el: ElementRef) {}

    @HostListener('dragover', ['$event'])
    onDragOver(evt: DragEvent) {
        evt.preventDefault();
        evt.stopPropagation();
        this.addOverClass();
    }

    @HostListener('dragleave', ['$event'])
    onDragLeave(evt: DragEvent) {
        evt.preventDefault();
        evt.stopPropagation();
        this.removeOverClass();
    }

    @HostListener('drop', ['$event'])
    onDrop(event: DragEvent) {
        event.preventDefault();
        event.stopPropagation();
        this.removeOverClass();

        const data: DataTransfer = event.dataTransfer;
        const items: DataTransferItemList = data.items;

        for (let i = 0; i < items.length; i++) {
            const entry: FileSystemEntry = items[i].webkitGetAsEntry();
            if (entry.isDirectory) {
                this.traverseFileTree(entry, entry.name);
            } else {
                const toUpload: DotUploadFile = new DotUploadFile(entry.name, entry);
                this.files.push(toUpload);
            }
        }

        this.itemDropped.emit(this.files);
    }

    private addOverClass(): void {
        if (!this.el.nativeElement.classList.contains(OVER_CLASS)) {
            this.el.nativeElement.classList.add(OVER_CLASS);
        }
    }

    private removeOverClass(): void {
        if (this.el.nativeElement.classList.contains(OVER_CLASS)) {
            this.el.nativeElement.classList.remove(OVER_CLASS);
        }
    }

    private traverseFileTree(item: FileSystemEntry, path: string) {
        if (item.isFile) {
            const toUpload: DotUploadFile = new DotUploadFile(path, item);
            this.files.push(toUpload);
        } else {
            path = path + '/';
            const dirReader = (item as FileSystemDirectoryEntry).createReader();
            let entries: FileSystemEntry[] = [];

            const readEntries = () => {
                // tslint:disable-next-line:cyclomatic-complexity
                dirReader.readEntries((items: FileSystemEntry[]) => {
                    if (!items.length) {
                        // add empty folders
                        if (entries.length === 0) {
                            const toUpload: DotUploadFile = new DotUploadFile(path, item);
                            this.zone.run(() => {
                                this.files.push(toUpload);
                            });
                        } else {
                            for (let i = 0; i < entries.length; i++) {
                                this.zone.run(() => {
                                    this.traverseFileTree(entries[i], path + entries[i].name);
                                });
                            }
                        }
                    } else {
                        // continue with the reading
                        entries = entries.concat(items);
                        readEntries();
                    }
                });
            };

            readEntries();
        }
    }
}
