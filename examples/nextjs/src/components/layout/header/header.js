'use client';

import Link from 'next/link';

import { useIsEditMode } from '@/hooks/isEditMode';
import ReorderButton from './components/reorderMenu';

function Header({ children }) {
    const isEditMode = useIsEditMode();

    return (
        <div className="flex items-center justify-between p-4 bg-purple-500">
            <div className="flex items-center">
                <h2 className="text-3xl font-bold text-white">
                    <Link href="/">TravelLux in NextJS</Link>
                </h2>

                {isEditMode && <ReorderButton />}
            </div>

            {children}
        </div>
    );
}

export default Header;
