package com.example.payments.domain.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    Optional<Claim> findByCode(String code);

    List<Claim> findByCodeIn(Collection<String> codes);
}




