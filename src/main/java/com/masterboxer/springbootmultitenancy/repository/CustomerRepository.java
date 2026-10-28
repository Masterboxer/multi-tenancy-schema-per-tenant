package com.masterboxer.springbootmultitenancy.repository;

import com.masterboxer.springbootmultitenancy.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
