import Link from 'next/link';
import { reorderMenu } from '@dotcms/client';
import Image from 'next/image';
import PublicImageLoader from '@/utils/publicImageLoader';

function Header({ children }) {
    return (
        <header className="flex items-center justify-between p-4 bg-purple-500">
            <div className="flex items-center">
                <h2 className="text-3xl font-bold text-white">
                    <Link href="/">TravelLux in NextJS</Link>
                </h2>
            </div>
            <ReorderButton />
            {children}
        </header>
    );
}

function ReorderButton() {

    return (
        <button className="bg-[#426BF0] rounded-sm flex cursor-pointer border-none px-2 py-1 gap-2"  onClick={() => reorderMenu()} >
            <Image src="/assets/icons/arrow-up.svg" width="0" height="0" className='w-6 h-6' alt="arrow up" loader={PublicImageLoader}/>
            <Image src="/assets/icons/arrow-down.svg"  width="0" height="0" className='w-6 h-6' alt="arrow down" loader={PublicImageLoader}/>
        </button>
    );
}


export default Header;
