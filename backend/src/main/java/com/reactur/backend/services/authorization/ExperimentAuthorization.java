package com.reactur.backend.services.authorization;

import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.enums.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Centralizes the ownership/role rules for accessing and modifying {@link Experiment}s, so that
 * every service (experiment service, file attachments, the audit history...) applies the exact
 * same rule.
 * <p>
 * Rules:
 * <ul>
 *     <li>{@code ADMIN} and {@code SUPERVISOR} both have global read/write access to experiments.</li>
 *     <li>The experiment's creator and its designated scientist always have read/write access to it.</li>
 *     <li>Any other approved user ({@code USER}/{@code ADMIN}/{@code SUPERVISOR}) may view an
 *     <b>active</b> experiment, but not an archived one.</li>
 *     <li>{@code UNAUTHORIZED} users have no access at all.</li>
 * </ul>
 * Note: whether modifying an experiment is allowed based on the experiment's archived state is
 * intentionally not part of this authorization class, and must be enforced independently by the
 * calling service / business logic.
 */
@Component
public class ExperimentAuthorization {

    /**
     * True for ADMIN and SUPERVISOR, which have global read/write access to every experiment.
     *
     * @param role the role to check
     * @return true if the role is privileged
     */
    public boolean isPrivileged(Role role) {
        return role == Role.ADMIN || role == Role.SUPERVISOR;
    }

    /**
     * True if the user is this experiment's creator or its designated scientist.
     *
     * @param experiment the experiment to check
     * @param userId     the Keycloak ID of the user to check
     * @return true if the user is the creator or the designated scientist
     */
    public boolean isCreatorOrScientist(Experiment experiment, String userId) {
        boolean isCreator = experiment.getCreatedBy().getId().equals(userId);
        boolean isScientist = experiment.getMetadata().getScientist().getId().equals(userId);
        return isCreator || isScientist;
    }

    /**
     * Active experiments are visible to any approved user. Archived experiments are restricted to
     * the creator, the designated scientist, or a privileged role.
     *
     * @param experiment the experiment to check
     * @param userId     the Keycloak ID of the requesting user
     * @param role       the role of the requesting user
     * @return true if the user may view this experiment
     */
    public boolean canView(Experiment experiment, String userId, Role role) {
        if (experiment.getArchivedAt() == null) {
            return role != Role.UNAUTHORIZED;
        }
        return isPrivileged(role) || isCreatorOrScientist(experiment, userId);
    }

    /**
     * Modifying an experiment (or its files) is restricted to the creator, the designated
     * scientist, or a privileged role (regardless of the experiment's archived state).
     *
     * @param experiment the experiment to check
     * @param userId     the Keycloak ID of the requesting user
     * @param role       the role of the requesting user
     * @return true if the user may modify this experiment
     */
    public boolean canModify(Experiment experiment, String userId, Role role) {
        return isPrivileged(role) || isCreatorOrScientist(experiment, userId);
    }

    /**
     * Same check as {@link #canView}, throwing instead of returning false.
     *
     * @param experiment the experiment to check
     * @param userId     the Keycloak ID of the requesting user
     * @param role       the role of the requesting user
     * @throws AccessDeniedException if the user may not view this experiment
     */
    public void assertCanView(Experiment experiment, String userId, Role role) {
        if (!canView(experiment, userId, role)) {
            throw new AccessDeniedException("You do not have permission to view this experiment.");
        }
    }

    /**
     * Same check as {@link #canModify}, throwing instead of returning false.
     *
     * @param experiment the experiment to check
     * @param userId     the Keycloak ID of the requesting user
     * @param role       the role of the requesting user
     * @throws AccessDeniedException if the user may not modify this experiment
     */
    public void assertCanModify(Experiment experiment, String userId, Role role) {
        if (!canModify(experiment, userId, role)) {
            throw new AccessDeniedException("You do not have permission to modify this experiment.");
        }
    }
}
