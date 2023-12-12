import Link from 'next/link';
import Navigation from './navigation';
import { useContext } from 'react';
import { GlobalContext } from '@/lib/providers/global';

function Header() {
    const { nav } = useContext(GlobalContext);
    return (
        <header className="flex items-center justify-between p-4 bg-blue-500">
            <div className="flex items-center">
                <h2 className="text-3xl font-bold text-white">
                    <Link href="/">TravelLux</Link>
                </h2>
            </div>
            <Navigation className="text-white" nav={nav} />
            <div className="flex items-center">
                <select className="px-2 py-1 border border-gray-300 rounded">
                    <option value="en">English</option>
                    <option value="es">Español</option>
                    <option value="fr">Français</option>
                </select>
            </div>
        </header>
    );
}

export default Header;
