package com.example.webshop.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "productcategories")
public class Category {

    @Id
    private Integer id;

    private String name;

    private String description;

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
