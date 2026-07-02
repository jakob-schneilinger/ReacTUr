
/**
 * Represents the lifecycle state of a 3D structure associated with a reactant.
 * This flag tracks the structure from its initial creation (via external API or manual upload)
 * through user verification to its final active or rejected state.
 */
export enum StructureFlag {
    PENDING = 'PENDING',
    READY = 'READY',
    ACTIVE = 'ACTIVE',
    REJECTED = 'REJECTED',
    FAILED = 'FAILED'
}
