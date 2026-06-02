package com.puyuanmaoshan.platform.plugin.ai_design_assistant.entity;

import java.time.LocalDateTime;
import java.util.Map;

public class Fabric {
    private Long id;
    private Long supplierTenantId;
    private String name;
    private String category;
    private String[] images;
    private String videoUrl;
    private Map<String, Object> specs;
    private Double pricePerMeter;
    private String stockStatus; // in_stock, out_of_stock
    private Integer isVisible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSupplierTenantId() { return supplierTenantId; }
    public void setSupplierTenantId(Long supplierTenantId) { this.supplierTenantId = supplierTenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String[] getImages() { return images; }
    public void setImages(String[] images) { this.images = images; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public Map<String, Object> getSpecs() { return specs; }
    public void setSpecs(Map<String, Object> specs) { this.specs = specs; }
    public Double getPricePerMeter() { return pricePerMeter; }
    public void setPricePerMeter(Double pricePerMeter) { this.pricePerMeter = pricePerMeter; }
    public String getStockStatus() { return stockStatus; }
    public void setStockStatus(String stockStatus) { this.stockStatus = stockStatus; }
    public Integer getIsVisible() { return isVisible; }
    public void setIsVisible(Integer isVisible) { this.isVisible = isVisible; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}