interface ListItemProps {
    children: React.ReactNode;
}

/**
 * ListItem component represents a list item in a block editor.
 *
 * @param children - The content of the list item.
 * @returns The rendered list item element.
 */
export const ListItem = ({ children }: ListItemProps) => {
    return <li>{children}</li>;
};

/**
 * Renders an ordered list component.
 *
 * @param children - The content to be rendered inside the ordered list.
 * @returns The ordered list component.
 */
export const OrderedList = ({ children }: ListItemProps) => {
    return <ol>{children}</ol>;
};

/**
 * Renders a bullet list component.
 *
 * @param children - The content of the bullet list.
 * @returns The rendered bullet list component.
 */
export const BulletList = ({ children }: ListItemProps) => {
    return <ul>{children}</ul>;
};
