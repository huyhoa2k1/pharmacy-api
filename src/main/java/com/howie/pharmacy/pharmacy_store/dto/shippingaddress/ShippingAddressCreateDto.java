package com.howie.pharmacy.pharmacy_store.dto.shippingaddress;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddressCreateDto {
    private String fullname;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String address;
    private Double latitude;
    private Double longitude;
}
