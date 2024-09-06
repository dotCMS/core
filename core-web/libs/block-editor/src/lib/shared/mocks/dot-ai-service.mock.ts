import { Observable, of } from 'rxjs';

import { delay } from 'rxjs/operators';

const DEFAULT_DELAY = 2000;
export class DotAiServiceMock {
    generateContent(): Observable<string> {
        return of(
            `
            Title: Understanding the Inner Workings of an Internal Combustion Engine\n\nIntroduction:\nAn internal combustion engine, commonly known as a "motor," is a fundamental component powering various modes of transportation today. Whether it is used in cars, motorcycles, or even power generators, this remarkable mechanical device plays a pivotal role in our modern society. In this post, we will delve into the intricate workings of an internal combustion engine, shedding light on its main components and the amazing combustion process that propels our vehicles forward.\n\n1. The Components:\nTypically consisting of multiple intricate parts, an internal combustion engine can be simplified into four main components:\n\na. Cylinder Block:\nA robust casing houses the cylinder, piston, and other associated components. This block not only provides structural strength but also assists in dissipating the heat produced during the engine's operation.\n\nb. Pistons:\nPlaced inside each cylinder, pistons move up and down during the engine's operation. Connected to the crankshaft, these essential components transform the linear motion of the pistons into the rotary motion necessary to turn the wheels.\n\nc. Valves:\nIntake and exhaust valves control the flow of air-fuel mixture into the combustion chamber and the expulsion of combustion byproducts. These valves play a crucial role in optimizing engine performance and ensuring efficient fuel combustion.\n\nd. Spark Plugs:\nElectrically ignited by the engine control unit (ECU), spark plugs generate a spark within the combustion chamber, initiating the combustion process. This controlled ignition ensures the synchronized release of energy within the engine.\n\n2. The Combustion Process:\nThe combustion process within an internal combustion engine can be summarized into four essential steps:\n\na. Intake:\nWith the intake valve open, a carefully regulated mixture of air and fuel is drawn into the combustion chamber during the piston's downward stroke.\n\nb. Compression:\nDuring the upward stroke of the piston, the intake valve closes, sealing the combustion chamber. The piston compresses the air-fuel mixture, significantly increasing its pressure and temperature.\n\nc. Combustion and Power Stroke:\nAt the desired moment, the spark plug sparks, igniting the compressed air-fuel mixture. This rapid combustion creates an explosion, generating a force that drives the piston back down, converting the expanding high-pressure gases into mechanical energy.\n\nd. Exhaust:\nWhen the piston reaches the bottom of its stroke, the exhaust valve opens, allowing the expulsion of spent gases resulting from the combustion process. The piston then moves back up to restart the cycle, and the process repeats.\n\nConclusion:\nThe internal combustion engine's marvel lies in its ability to convert chemical energy stored in fuel into mechanical energy, enabling the propulsion of vehicles we rely on daily. Understanding its intricate components and the combustion process that takes place within can help us appreciate the engineering brilliance behind this staple technology. From the rhythmic motion of pistons to the precisely timed ignition of fuel, every aspect of an internal combustion engine harmoniously collaborates to fuel our ever-advancing world
            `
        ).pipe(delay(DEFAULT_DELAY));
    }

    generateAndPublishImage() {
        return of({
            originalPrompt: 'cow in the snow',
            response: 'temp_3839a0f05f',
            revised_prompt:
                "A well-fed, adult cow, with a dark, almost black coat standing majestically in a snowy landscape. The sun is low in the sky, casting a warm, pinkish light that contrasts with the cold whiteness of the snow. The cow's breath freezes in the cold air, creating a visible puff of steam. Snowflakes delicately drift from the sky, settling on the cow's back and in its fur, creating a beautiful winter scene. The cow, unbothered by the snow, looks ahead with a calm, steady gaze resulting in an image of tranquillity and resilience in the face of nature's stark beauty.",
            tempFileName: 'cow_20231211_040442.png',
            url: 'https://oaidalleapiprodscus.blob.core.windows.net/private/org-qZnNeZvyp7uFUh2EX8AJA6gw/user-3adfuZcoiVNCPk2q6ZAVoT5u/img-FHDDQywCgDYdyzKCk3f1ybZi.png?st=2023-12-11T15%3A04%3A42Z&se=2023-12-11T17%3A04%3A42Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-12-10T23%3A17%3A53Z&ske=2023-12-11T23%3A17%3A53Z&sks=b&skv=2021-08-06&sig=hAOg7fmzNvmEk5pL/IR2Bzsy3WR0ww6NYnqr/92ybaU%3D'
        }).pipe(delay(DEFAULT_DELAY));
    }

    checkPluginInstallation() {
        return of(true);
    }
}
