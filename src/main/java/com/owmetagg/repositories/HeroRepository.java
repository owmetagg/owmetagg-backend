package com.owmetagg.repositories;

import com.owmetagg.entities.Hero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HeroRepository extends JpaRepository<Hero, Long> {
    Optional<Hero> findByName(String name);
    List<Hero> findByRole(String role);
}