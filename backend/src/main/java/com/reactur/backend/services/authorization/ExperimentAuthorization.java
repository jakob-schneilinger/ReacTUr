package com.reactur.backend.services.authorization;

import com.reactur.backend.entities.experiment.Experiment;
import com.reactur.backend.enums.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Centralizes the ownership/role rules for accessing and modifying {@link Experiment}s, so that
 * every service (experiment service, file attachments, the audit history...) applies
 * the exact same rule
 * <p>
 * Rules:
 * <ul>
 *     <li>{@code ADMIN} and {@code SUPERVISOR} both have global read/write access to experiments.</li>
 *     <li>The experiment's creator and its designated scientist always have read/write access to it.</li>
 *     <li>Any other approved user ({@code USER}/{@code ADMIN}/{@code SUPERVISOR}) may view an
 *     <b>active</b> experiment, but not an archived one.</li>
 *     <li>{@code UNAUTHORIZED} users have no access at all.</li>
 * </ul>
 * Note: whether modifying an experiment is allowed or not based in the experiment's archived state is intentionally not
 * part of this authorization class, and must be enforced independently by the calling service / business logic.
 */
@Component
public class ExperimentAuthorization {

    public boolean isPrivileged(Role role) {
        return role == Role.ADMIN || role == Role.SUPERVISOR;
    }

    public boolean isCreatorOrScientist(Experiment experiment, String userId) {
        boolean isCreator = experiment.getCreatedBy().getId().equals(userId);
        boolean isScientist = experiment.getMetadata().getScientist().getId().equals(userId);
        return isCreator || isScientist;
    }

    /**
     * Active experiments are visible to any approved user. Archived experiments are restricted to
     * the creator, the designated scientist, or a privileged role.
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
     */
    public boolean canModify(Experiment experiment, String userId, Role role) {
        return isPrivileged(role) || isCreatorOrScientist(experiment, userId);
    }

    public void assertCanView(Experiment experiment, String userId, Role role) {
        if (!canView(experiment, userId, role)) {
            throw new AccessDeniedException("You do not have permission to view this experiment.");
        }
    }

    public void assertCanModify(Experiment experiment, String userId, Role role) {
        if (!canModify(experiment, userId, role)) {
            throw new AccessDeniedException("You do not have permission to modify this experiment.");
        }
    }
}
