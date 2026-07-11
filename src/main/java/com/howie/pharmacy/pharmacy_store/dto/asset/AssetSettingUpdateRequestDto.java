package com.howie.pharmacy.pharmacy_store.dto.asset;

import java.util.List;

public class AssetSettingUpdateRequestDto {

    private String logoPublicId;
    private List<String> bannerPublicIds;

    public AssetSettingUpdateRequestDto() {
    }

    public String getLogoPublicId() {
        return logoPublicId;
    }

    public void setLogoPublicId(String logoPublicId) {
        this.logoPublicId = logoPublicId;
    }

    public List<String> getBannerPublicIds() {
        return bannerPublicIds;
    }

    public void setBannerPublicIds(List<String> bannerPublicIds) {
        this.bannerPublicIds = bannerPublicIds;
    }
}
