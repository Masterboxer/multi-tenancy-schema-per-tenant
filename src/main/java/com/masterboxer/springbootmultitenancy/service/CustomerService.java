package com.masterboxer.springbootmultitenancy.service;

import com.masterboxer.springbootmultitenancy.record.CustomerRecord;
import com.masterboxer.springbootmultitenancy.record.PageResponse;

public interface CustomerService {

    PageResponse<CustomerRecord> getCustomers(int page, int size);

    void insertCustomers(int customerNumber);
}
