package com.school.sms.model;

public class RequirementTemplate {
    private int id;
    private String category; // "Boarding" or "Day"
    private String itemName;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
}