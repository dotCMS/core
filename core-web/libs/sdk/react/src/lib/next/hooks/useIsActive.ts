import { useEffect, useState } from 'react';

import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';

const useActiveContentlet = () => {
    const [activeContentlet, setActiveContentlet] = useState<string | null>(null);

    useEffect(() => {
        window.addEventListener('message', (event) => {
            if (event.data.name === __DOTCMS_UVE_EVENT__.UVE_ACTIVE_CONTENTLET) {
                setActiveContentlet(event.data.identifier);
            }
        });

        return () => {
            window.removeEventListener('message', (event) => {
                if (event.data.name === __DOTCMS_UVE_EVENT__.UVE_ACTIVE_CONTENTLET) {
                    setActiveContentlet(null);
                }
            });
        };
    }, []);

    return activeContentlet;
};

export default useActiveContentlet;
