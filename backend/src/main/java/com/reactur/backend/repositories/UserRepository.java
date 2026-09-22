package com.reactur.backend.repositories;

import com.reactur.backend.entities.User;
import com.reactur.backend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Used to guard against ending up with zero remaining ADMIN users.
     *
     * @param role the role to count
     * @return the number of users with that role
     */
    long countByRole(Role role);
}
