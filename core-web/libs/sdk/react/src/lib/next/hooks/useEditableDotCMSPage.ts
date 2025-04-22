import { useState, useEffect } from 'react';

import { getUVEState, initUVE, createUVESubscription } from '@dotcms/uve';
import { DotCMSEditablePage, UVEEventType } from '@dotcms/uve/types';

export const useEditableDotCMSPage = (editablePage: DotCMSEditablePage) => {
    const [updatedEditablePage, setUpdatedEditablePage] =
        useState<DotCMSEditablePage>(editablePage);

    useEffect(() => {
        if (!getUVEState()) {
            return;
        }

        const { destroyUVESubscriptions } = initUVE(editablePage);

        const { unsubscribe } = createUVESubscription(UVEEventType.CONTENT_CHANGES, (payload) => {
            setUpdatedEditablePage(payload as DotCMSEditablePage);
        });

        return () => {
            destroyUVESubscriptions();
            unsubscribe();
        };
    }, [editablePage]);

    return updatedEditablePage;
};
