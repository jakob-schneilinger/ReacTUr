package com.reactur.backend.repositories;

import com.reactur.backend.entities.User;
import com.reactur.backend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /** Used to guard against demoting the last remaining ADMIN (see {@code UserServiceImpl.setRole}). */
    long countByRole(Role role);
}
