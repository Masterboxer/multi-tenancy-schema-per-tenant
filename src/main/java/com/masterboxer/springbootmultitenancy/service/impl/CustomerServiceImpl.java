package com.masterboxer.springbootmultitenancy.service.impl;

import com.masterboxer.springbootmultitenancy.model.Customer;
import com.masterboxer.springbootmultitenancy.record.CustomerRecord;
import com.masterboxer.springbootmultitenancy.repository.CustomerRepository;
import com.masterboxer.springbootmultitenancy.service.CustomerService;
import com.masterboxer.springbootmultitenancy.util.CustomerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional
    @Override
    public void insertCustomers(Integer customerNumber) {
        List<Customer> customers = CustomerFactory.generateCustomers(customerNumber, 0);
        customerRepository.saveAll(customers);
    }
}
