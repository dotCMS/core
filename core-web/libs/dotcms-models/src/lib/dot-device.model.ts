export interface DotDevice {
    identifier: string;
    cssHeight: string;
    cssWidth: string;
    name: string;
    inode: string;
}

export interface DotDeviceIcon extends DotDevice {
    icon: string;
}
