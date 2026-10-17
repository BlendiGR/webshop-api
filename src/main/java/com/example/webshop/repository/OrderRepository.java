package com.example.webshop.repository;

import com.example.webshop.entity.CustomerOrder;
import com.example.webshop.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Integer> {

    // select ... for update, keeps the expire event away while we cancel
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CustomerOrder o WHERE o.id = :id")
    Optional<CustomerOrder> findForUpdate(Integer id);

    // moves the order only if it is still in the expected status
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CustomerOrder o SET o.status = :to WHERE o.id = :id AND o.status = :from")
    int changeStatus(Integer id, OrderStatus from, OrderStatus to);

    boolean existsByCustomerId(Integer customerId);

    boolean existsByShippingAddressId(Integer addressId);
}
