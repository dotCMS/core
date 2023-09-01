import * as fs from 'fs';
import * as path from 'path';

interface Member {
    name: string;
    slack_id?: string;
}

interface Team {
    id: string;
    name: string;
    label: string;
    project_url: string;
    members: string[];
    current_milestone: string;
}

export const getMetadata = (confDir: string, member: string, get: string): string => {
    const members: Member[] = JSON.parse(readFromFile(confDir, 'members.json')).members;
    const teams: Team[] = JSON.parse(readFromFile(confDir, 'teams.json')).teams;

    let result;
    switch (get) {
        case 'MEMBERS':
            result = members.map((m) => m.name);
            break;
        case 'TEAMS':
            result = teams.map((t) => t.id);
            break;
        case 'TEAMS_BY_MEMBER':
            result = teams.filter((team) => team.members.includes(member)).map((team) => team.id);
            break;
    }

    return result ? JSON.stringify(result, null, 0) : '';
};

const readFromFile = (configDir: string, file: string): string => {
    const confFile = path.join(configDir, file);
    console.log(`Reading config file from ${confFile}`);

    return fs.readFileSync(confFile, {
        encoding: 'utf8',
        flag: 'r'
    });
};
