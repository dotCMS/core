"use client";

import { useState } from "react";
import ReactPlayer from "react-player";
import Image from "next/image";

export function Video({ thumbnailCustom }) {
    const [isPlaying, setIsPlaying] = useState(false);

    return (
        <div className="relative w-full h-full min-h-96">
            {!isPlaying ? (
                <>
                    <Image
                        src={thumbnailCustom}
                        alt="Video thumbnail"
                        fill={true}
                        className="object-cover w-full h-full"
                        objectFit="cover"
                    />
                    <button
                        className="absolute top-1/2 left-1/2 p-4 bg-white bg-opacity-75 rounded-full transform -translate-x-1/2 -translate-y-1/2"
                        onClick={() => setIsPlaying(true)}
                    >
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            className="w-12 h-12 text-black"
                            viewBox="0 0 24 24"
                            fill="currentColor"
                        >
                            <path d="M8 5v14l11-7z" />
                        </svg>
                    </button>
                </>
            ) : (
                <ReactPlayer
                    url="https://www.youtube.com/watch?v=LXb3EKWsInQ"
                    width="100%"
                    height="100%"
                    onPause={() => setIsPlaying(false)}
                />
            )}
        </div>
    );
}
