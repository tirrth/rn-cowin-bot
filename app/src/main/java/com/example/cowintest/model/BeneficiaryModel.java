package com.example.cowintest.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class BeneficiaryModel {
    private String name, beneficiaryReferenceId, birthYear, gender, mobileNumber, photoIdType, photoIdNumber, comorbidityInd, vaccinationStatus, vaccine, dose1Date, dose2Date;
    private JSONArray appointments;
    private boolean isSelected = false;

    public BeneficiaryModel(JSONObject beneficiary) {
        this.name = beneficiary.optString("name");
        this.beneficiaryReferenceId = beneficiary.optString("beneficiary_reference_id");
        this.birthYear = beneficiary.optString("birth_year");
        this.gender = beneficiary.optString("gender");
        this.mobileNumber = beneficiary.optString("mobile_number");
        this.photoIdType = beneficiary.optString("photo_id_type");
        this.photoIdNumber = beneficiary.optString("photo_id_number");
        this.comorbidityInd = beneficiary.optString("comorbidity_ind");
        this.vaccinationStatus = beneficiary.optString("vaccination_status");
        this.vaccine = beneficiary.optString("vaccine");
        this.dose1Date = beneficiary.optString("dose1_date");
        this.dose2Date = beneficiary.optString("dose2_date");
        this.appointments = beneficiary.optJSONArray("appointments");
    }

    public String getName() {
        return name;
    }

    public String getBeneficiaryReferenceId(){
        return beneficiaryReferenceId;
    }

    public String getBirthYear()  { return birthYear; }

    public String getGender()  { return gender; }

    public String getMobileNumber()  { return mobileNumber; }

    public String getPhotoIdType()  { return photoIdType; }

    public String getPhotoIdNumber()  { return photoIdNumber; }

    public String getComorbidityInd()  { return comorbidityInd; }

    public String getVaccinationStatus()  { return vaccinationStatus; }

    public String getVaccine()  { return vaccine; }

    public String getDose1Date()  { return dose1Date; }

    public JSONArray getAppointments()  { return appointments; }

    public String getDose2Date()  { return dose2Date; }

    public boolean is1stDoseGiven() { return (dose1Date != null && !dose1Date.isEmpty()); }

    public boolean is2ndDoseGiven() { return (dose2Date != null && !dose2Date.isEmpty()); }

    public boolean is45Plus(){
        final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        final int birthYear = Integer.parseInt(getBirthYear());
        return ((currentYear - birthYear) >= 45);
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isSelected() {
        return isSelected;
    }
}
