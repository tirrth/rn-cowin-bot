package com.example.cowintest.model;

import org.json.JSONObject;

public class VaccinationCenterModel {
    private String name, address;
    private int centerId;
    private boolean isSelected = false;

    public VaccinationCenterModel(JSONObject center) {
        this.name = center.optString("name");
        this.address = center.optString("address");
        this.centerId = center.optInt("center_id");
    }

    public String getName() {
        return name;
    }

    public String getAddress(){
        return address;
    }

    public int getCenterId(){
        return centerId;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isSelected() {
        return isSelected;
    }
}
