'use client';
import Link from 'next/link';
import { isInsideEditor } from '@dotcms/client';
import { useEffect, useState } from 'react';
import ReorderButton from './components/reorderMenu';

function Header({ children }) {
    const [insideEditor, setInsideEditor] = useState(false);
    

    useEffect(() => {
        console.log("isInsideEditor => ", isInsideEditor());
        setInsideEditor(isInsideEditor());
    }, [])

    return (
        <div className="flex items-center justify-between p-4 bg-purple-500">
            <div className="flex items-center">
                <h2 className="text-3xl font-bold text-white">
                    <Link href="/">TravelLux in NextJS</Link>
                </h2>

                {insideEditor && (
                    <ReorderButton />
                )}
            </div>
            
            {children}
        </div>
    );
}




export default Header;
