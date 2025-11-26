export enum UserPermissions {
    READ = 'READ',
    WRITE = 'WRITE'
}

export enum PermissionsType {
    HTMLPAGES = 'HTMLPAGES',
    CONTAINERS = 'CONTAINERS',
    FOLDERS = 'FOLDERS',
    LINKS = 'LINKS',
    TEMPLATES = 'TEMPLATES',
    TEMPLATE_LAYOUTS = 'TEMPLATE_LAYOUTS',
    STRUCTURES = 'STRUCTURES',
    CONTENTLETS = 'CONTENTLETS',
    CATEGORY = 'CATEGORY',
    RULES = 'RULES'
}

/**
 * Interface for current users.
 *
 * @interface
 */
export interface DotCurrentUser {
    admin: boolean;
    email: string;
    givenName: string;
    roleId: string;
    surname: string;
    userId: string;
}

export interface DotPermissionsType {
    [key: string]: {
        canRead?: boolean;
        canWrite?: boolean;
    };
}
