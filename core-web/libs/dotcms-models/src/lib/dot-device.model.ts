export interface DotDevice {
    identifier: string;
    cssHeight: string;
    cssWidth: string;
    name: string;
    inode: string;
    stInode?: string;
}

export interface DotDeviceListItem extends DotDevice {
    icon: string;
}
