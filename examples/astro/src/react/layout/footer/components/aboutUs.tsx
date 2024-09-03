import useImageSrc from "src/react/hooks/useImageSrc";

export const AboutUs = () => {
  const src = useImageSrc({ src: "82da90eb04/fileAsset/logo.png", width: 221 });

  return (
    <div className="flex flex-col gap-7">
      <h2 className="text-2xl font-bold text-black">About us</h2>
      <p className="text-sm text-zinc-800">
        We are TravelLux, a community of dedicated travel experts, journalists,
        and bloggers. Our aim is to offer you the best insight on where to go
        for your travel as well as to give you amazing opportunities with free
        benefits and bonuses for registered clients.
      </p>
      <img src={src} height={53} width={221} alt="TravelLux logo" />
    </div>
  );
};
