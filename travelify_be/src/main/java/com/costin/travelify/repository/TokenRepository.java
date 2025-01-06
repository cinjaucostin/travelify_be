package com.costin.travelify.repository;

import com.costin.travelify.entities.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Integer> {
    @Query("""
            select t from Token t inner join User u on t.user.id=u.id
            where u.id = :userId and t.expired = false
            """)
    List<Token> findAllValidTokensByUser(int userId);

    Optional<Token> findByToken(String token);
}
