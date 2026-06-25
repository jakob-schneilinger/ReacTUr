package com.reactur.backend.services;

import com.reactur.backend.dtos.GetUserDto;
import com.reactur.backend.dtos.SetRoleDto;
import com.reactur.backend.exeptions.ResourceNotFoundException;

import java.util.List;

public interface UserService {

    /**
     * Retrieves all users currently registered in the system.
     *
     * @return a list of {@link GetUserDto} representing all users.
     */
    List<GetUserDto> getAllUsers();

    /**
     * Updates the role of a specific user.
     *
     * @param id the unique identifier of the user.
     * @param setRoleDto the DTO containing the new role.
     * @throws ResourceNotFoundException if the user does not exist.
     */
    void setRole(String id, SetRoleDto setRoleDto);

    /**
     * Archives a user by setting their deactivatedAt timestamp.
     *
     * @param id the unique identifier of the user to archive.
     * @throws ResourceNotFoundException if the user does not exist.
     */
    void archiveUser(String id);

    /**
     * Restores an archived user by setting their deactivatedAt timestamp to null.
     *
     * @param id the unique identifier of the user to restore.
     * @throws ResourceNotFoundException if the user does not exist.
     */
    void restoreUser(String id);
}
