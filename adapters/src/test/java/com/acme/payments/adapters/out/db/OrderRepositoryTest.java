package com.acme.payments.adapters.out.db;

import com.acme.payments.adapters.out.db.entity.OrderEntity;
import com.acme.payments.adapters.out.db.repo.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@AutoConfigureTestEntityManager
@ContextConfiguration(classes = OrderRepositoryTest.Config.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OrderRepositoryTest {

    @Autowired OrderRepository repo;
    @Autowired TestEntityManager em;

    @Configuration
    @EnableJpaRepositories(basePackages = "com.acme.payments.adapters.out.db.repo")
    @EntityScan(basePackages = "com.acme.payments.adapters.out.db.entity")
    static class Config {}

    @Test
    void saves_and_reads_order() {
        OrderEntity e = new OrderEntity("o1", "m1", 1000L, "INR", "NEW");
        repo.save(e);
        assertThat(repo.findById("o1")).isPresent();
    }
}
