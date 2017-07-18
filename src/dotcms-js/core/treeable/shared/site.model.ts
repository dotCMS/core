import {Treeable} from './treeable.model';

/**
 * Site/Host Object for the Site/Host Object in dotCMS
 */
export class Site extends Treeable {
    hostname: string;
    aliases: string;
    identifier: string;
    inode: string;
}