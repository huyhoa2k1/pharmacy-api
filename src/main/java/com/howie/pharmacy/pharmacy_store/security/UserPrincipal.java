package com.howie.pharmacy.pharmacy_store.security;

public class UserPrincipal {
    private final Integer id;
    private final String phone;

    public UserPrincipal(Integer id, String phone) {
        this.id = id;
        this.phone = phone;
    }

    public Integer getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String toString() {
        return phone;
    }
}
