package com.reactur.backend.enums;

/**
 * Defines the roles a user can have within this application.
 */
public enum Role {
    /** Synced from Keycloak but not yet approved by an admin/supervisor; no access beyond checking their own role. */
    UNAUTHORIZED,
    /** A normal, approved user. */
    USER,
    /** Full access, including managing other users' roles and the admin-only option catalogs. */
    ADMIN,
    /** Approved, with most of ADMIN's access, but not user management, the option catalogs, or updating/archiving/restoring a reactant. */
    SUPERVISOR
}
