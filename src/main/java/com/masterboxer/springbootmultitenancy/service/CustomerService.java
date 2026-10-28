package com.masterboxer.springbootmultitenancy.service;

import com.masterboxer.springbootmultitenancy.model.Customer;
import com.masterboxer.springbootmultitenancy.record.CustomerRecord;

import java.util.List;

public interface CustomerService {

    List<Customer> getAllCustomers();

    void insertCustomers(Integer customerNumber);
}
