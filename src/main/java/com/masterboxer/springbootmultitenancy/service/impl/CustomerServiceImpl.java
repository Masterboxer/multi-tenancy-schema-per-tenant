package com.masterboxer.springbootmultitenancy.service.impl;

import com.masterboxer.springbootmultitenancy.model.Customer;
import com.masterboxer.springbootmultitenancy.record.CustomerRecord;
import com.masterboxer.springbootmultitenancy.record.PageResponse;
import com.masterboxer.springbootmultitenancy.repository.CustomerRepository;
import com.masterboxer.springbootmultitenancy.service.CustomerService;
import com.masterboxer.springbootmultitenancy.util.CustomerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerRecord> getCustomers(int page, int size) {
        // Fixed sort on the primary key: stable pages, and no client-controlled sort property
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id"));
        return PageResponse.from(customerRepository.findAll(pageRequest).map(CustomerRecord::toRecord));
    }

    @Override
    @Transactional
    public void insertCustomers(int customerNumber) {
        List<Customer> customers = CustomerFactory.generateCustomers(customerNumber, 0);
        customerRepository.saveAll(customers);
    }
}
