package com.example.webshop.repository;

import com.example.webshop.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Integer> {

    List<Address> findByCustomerIdOrderById(Integer customerId);

    Optional<Address> findByIdAndCustomerId(Integer id, Integer customerId);

    // system versioning, the address row as it was at the given time
    @Query(value = """
            SELECT street_address AS streetAddress, postal_code AS postalCode, city, country
            FROM customeraddresses FOR SYSTEM_TIME AS OF TIMESTAMP :time
            WHERE id = :id""", nativeQuery = true)
    Optional<AddressRow> findAsOf(Integer id, LocalDateTime time);

    interface AddressRow {
        String getStreetAddress();
        String getPostalCode();
        String getCity();
        String getCountry();
    }
}
