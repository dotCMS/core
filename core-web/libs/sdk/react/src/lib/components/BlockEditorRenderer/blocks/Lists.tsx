interface BlockProps {
    children: React.ReactNode;
}

export const ListItem = ({ children }: BlockProps) => {
    return <li>{children}</li>;
};

export const OrderedList = ({ children }: BlockProps) => {
    return <ol>{children}</ol>;
};

export const BulletList = ({ children }: BlockProps) => {
    return <ul>{children}</ul>;
};

