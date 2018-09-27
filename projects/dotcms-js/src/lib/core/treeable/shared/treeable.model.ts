/**
 * Treeable is the parent class/interface for all dotCMS Objects that can live under a Host/Folder
 */
export class Treeable {
    inode: string;
    identifier: string;
    type: string;
    modDate: Date;
    name: string;
    live: boolean;
    working: boolean;
    archived: boolean;
    title: string;
    displayType: string;
    modUser: string;
}