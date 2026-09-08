/**
 *Value return by {@see DotRelationshipsPropertyComponent}
 *
 * @export
 * @interface DotRelationshipsPropertyValue
 */
export interface DotRelationshipsPropertyValue {
    /**
     * Absent until a content type is picked — `validateRelationship` treats that as invalid, which
     * is how the form keeps a half-filled relationship from being saved.
     */
    velocityVar?: string;
    cardinality: number;
}
