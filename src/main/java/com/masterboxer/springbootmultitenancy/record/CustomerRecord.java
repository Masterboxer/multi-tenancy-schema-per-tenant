package com.masterboxer.springbootmultitenancy.record;

import com.masterboxer.springbootmultitenancy.model.Customer;
import lombok.Builder;

import java.util.Date;

@Builder
public record CustomerRecord(Long id,
        String firstName,
        String lastName,
        String country,
        String address,
        String city,
        Date creationDate,
        Date updateDate) {

    public static CustomerRecord toRecord(Customer customer) {
        return CustomerRecord.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .country(customer.getCountry())
                .address(customer.getAddress())
                .city(customer.getCity())
                .creationDate(customer.getCreationDate())
                .updateDate(customer.getUpdateDate())
                .build();
    }
}
