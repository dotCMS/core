'use client';
import Link from 'next/link';

import { useEffect, useState } from 'react';
import { isEditMode } from '@/utils/isEditMode';
import ReorderButton from './components/reorderMenu';

function Header({ children }) {
    const [isEditing, setIsEditing] = useState(false);

    useEffect(() => {
        setIsEditing(isEditMode());
    }, []);

    return (
        <div className="flex items-center justify-between p-4 bg-purple-500">
            <div className="flex items-center">
                <h2 className="text-3xl font-bold text-white">
                    <Link href="/">TravelLux in NextJS</Link>
                </h2>

                {isEditing && <ReorderButton />}
            </div>

            {children}
        </div>
    );
}

export default Header;
