import Link from 'next/link';
import { reorderMenu } from '@dotcms/client';

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
    const arrowUp = {
        backgroundImage: 'url(data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDgiIGhlaWdodD0iNDgiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CgogPGc+CiAgPHRpdGxlPmJhY2tncm91bmQ8L3RpdGxlPgogIDxyZWN0IGZpbGw9Im5vbmUiIGlkPSJjYW52YXNfYmFja2dyb3VuZCIgaGVpZ2h0PSI0MDIiIHdpZHRoPSI1ODIiIHk9Ii0xIiB4PSItMSIvPgogPC9nPgogPGc+CiAgPHRpdGxlPkxheWVyIDE8L3RpdGxlPgogIDxwYXRoIGZpbGw9IiNmZmZmZmYiIGlkPSJzdmdfMSIgZD0ibTE0LjgzLDMwLjgzbDkuMTcsLTkuMTdsOS4xNyw5LjE3bDIuODMsLTIuODNsLTEyLC0xMmwtMTIsMTJsMi44MywyLjgzeiIvPgogIDxwYXRoIGlkPSJzdmdfMiIgZmlsbD0ibm9uZSIgZD0ibS0zMC42OTQ1NTcsOS40MjU4ODdsNDgsMGwwLDQ4bC00OCwwbDAsLTQ4eiIvPgogPC9nPgo8L3N2Zz4=)'
    };

    const arrowDown = {
        backgroundImage: 'url(data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDgiIGhlaWdodD0iNDgiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CgogPGc+CiAgPHRpdGxlPmJhY2tncm91bmQ8L3RpdGxlPgogIDxyZWN0IGZpbGw9Im5vbmUiIGlkPSJjYW52YXNfYmFja2dyb3VuZCIgaGVpZ2h0PSI0MDIiIHdpZHRoPSI1ODIiIHk9Ii0xIiB4PSItMSIvPgogPC9nPgogPGc+CiAgPHRpdGxlPkxheWVyIDE8L3RpdGxlPgogIDxwYXRoIGZpbGw9IiNmZmZmZmYiIGlkPSJzdmdfMSIgZD0ibTE0LjgzLDE2LjQybDkuMTcsOS4xN2w5LjE3LC05LjE3bDIuODMsMi44M2wtMTIsMTJsLTEyLC0xMmwyLjgzLC0yLjgzeiIvPgogIDxwYXRoIGlkPSJzdmdfMiIgZmlsbD0ibm9uZSIgZD0ibS0xOC4zOTk4OTksMTcuMDc4NDczbDQ4LDBsMCw0OGwtNDgsMGwwLC00OHoiLz4KIDwvZz4KPC9zdmc+)'
    };

    return (
        <button className="bg-[#426BF0] rounded-sm flex cursor-pointer border-none"  onClick={() => reorderMenu()} >
            <span className="bg-contain bg-no-repeat h-9 w-9" style={arrowUp}></span>
            <span className="bg-contain bg-no-repeat h-9 w-9" style={arrowDown}></span>
        </button>
    );
}


export default Header;
