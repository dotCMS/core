import Contentlet from "./contentlet";
import Entry from "./entry";


function Contentlets({ contentlets }) {
    return (
        <ul className="flex flex-col gap-7">
            {contentlets.map((contentlet) => (
                <Contentlet contentlet={contentlet} key={contentlet.identifier}>
                    <li className="flex gap-7 min-h-16">
                        <Entry contentlet={contentlet} />
                    </li>
                </Contentlet>
            ))}
        </ul>
    );
}

export default Contentlets;
