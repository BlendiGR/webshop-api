package com.example.webshop.repository;

import com.example.webshop.entity.CampaignPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CampaignPriceRepository extends JpaRepository<CampaignPrice, CampaignPrice.Key> {

    // current and coming campaigns
    List<CampaignPrice> findByProductIdAndValidToAfterOrderByValidFrom(Integer productId, LocalDate date);
}
