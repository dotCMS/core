import { BlockProps } from '../../../models/blocks.interface';

/**
 * ListItem component represents a list item in a block editor.
 *
 * @param children - The content of the list item.
 * @returns The rendered list item element.
 */
export const ListItem = ({ children }: BlockProps) => {
    return <li>{children}</li>;
};

/**
 * Renders an ordered list component.
 *
 * @param children - The content to be rendered inside the ordered list.
 * @returns The ordered list component.
 */
export const OrderedList = ({ children }: BlockProps) => {
    return <ol>{children}</ol>;
};

/**
 * Renders a bullet list component.
 *
 * @param children - The content of the bullet list.
 * @returns The rendered bullet list component.
 */
export const BulletList = ({ children }: BlockProps) => {
    return <ul>{children}</ul>;
};
