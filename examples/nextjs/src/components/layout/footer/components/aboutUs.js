import React from "react";
import Image from "next/image";

function AboutUs() {
    return (
        <div className="flex gap-7 flex-col">
            <h2 className="text-2xl font-bold text-black">About us</h2>
            <p className="text-sm text-zinc-800">
                We are TravelLux, a community of dedicated travel experts,
                journalists, and bloggers. Our aim is to offer you the best
                insight on where to go for your travel as well as to give you
                amazing opportunities with free benefits and bonuses for
                registered clients.
            </p>
            <Image
                src={"/dA/2f383e7fa6/fileAsset/logo.png"}
                height={53}
                width={221}
                alt="TravelLux logo"
            />
        </div>
    );
}

export default AboutUs;
