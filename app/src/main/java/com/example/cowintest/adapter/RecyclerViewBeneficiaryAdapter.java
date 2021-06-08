package com.example.cowintest.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cowintest.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RecyclerViewBeneficiaryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    // private Context context;
    private JSONArray beneficiaryList;
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public RecyclerViewBeneficiaryAdapter(Context context, JSONArray beneficiaryList){
        // this.context = context;
        this.beneficiaryList = beneficiaryList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == TYPE_ITEM){
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.beneficiary_card, parent, false);
            return new ViewHolder(view);
        } else if(viewType == TYPE_HEADER){
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.beneficiary_header, parent, false);
            return new HeaderViewHolder(view);
        }
        else return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            JSONObject beneficiary = (JSONObject) beneficiaryList.get(position);
            if (holder instanceof ViewHolder) {
                final ViewHolder itemViewHolder = (ViewHolder) holder;
                Log.d("Beneficiary", beneficiary.toString());
                itemViewHolder.beneficiaryName.setText(beneficiary.optString("name"));
                itemViewHolder.beneficiaryVaccinationStatus.setText(beneficiary.optString("vaccination_status"));
                itemViewHolder.beneficiaryBirthYear.setText(beneficiary.optString("birth_year"));
                itemViewHolder.beneficiaryPhotoId.setText(beneficiary.optString("photo_id_type"));
                itemViewHolder.beneficiaryIdNumber.setText(beneficiary.optString("photo_id_number"));
                GradientDrawable bgVaccinationStatusShape = (GradientDrawable) itemViewHolder.beneficiaryVaccinationStatus.getBackground();
                final String vaccinationStatus = beneficiary.optString("vaccination_status");
                if (!vaccinationStatus.equalsIgnoreCase("Not Vaccinated")) {
                    if (vaccinationStatus.equalsIgnoreCase("Partially Vaccinated")) {
                        bgVaccinationStatusShape.setColor(Color.rgb(65, 173, 73));
                        bgVaccinationStatusShape.setStroke(1, Color.rgb(3, 175, 16));
                        itemViewHolder.beneficiaryVaccinationStatus.setTextColor(Color.WHITE);
                    }
                    itemViewHolder.beneficiaryVaccinationInformation.setVisibility(View.VISIBLE);
                    itemViewHolder.beneficiaryReferenceId.setText(beneficiary.optString("beneficiary_reference_id"));
                    final String referenceId = beneficiary.optString("beneficiary_reference_id");
                    final int subtractedLength = referenceId.length() - 4;
                    if (subtractedLength > 0) {
                        final String lastFourDigits = referenceId.substring(subtractedLength);
                        itemViewHolder.beneficiarySecretCode.setText(lastFourDigits);
                    }
                } else {
                    bgVaccinationStatusShape.setColor(Color.rgb(255, 152, 64));
                    bgVaccinationStatusShape.setStroke(1, Color.rgb(255, 96, 2));
                }
            } else if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
                headerViewHolder.registeredMobileNumber.setText("XXX-XXX-".concat(beneficiary.optString("mobile_number")));
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;
        return TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return beneficiaryList.length();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView beneficiaryName;
        public TextView beneficiaryVaccinationStatus;
        public TextView beneficiaryBirthYear;
        public TextView beneficiaryPhotoId;
        public TextView beneficiaryIdNumber;
        public TextView beneficiaryReferenceId;
        public TextView beneficiarySecretCode;
        public LinearLayout beneficiaryVaccinationInformation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // itemView.setOnClickListener(this); // Uncomment this to enable click-listener event...
            beneficiaryName = itemView.findViewById(R.id.beneficiaryName);
            beneficiaryVaccinationStatus = itemView.findViewById(R.id.beneficiaryVaccinationStatus);
            beneficiaryBirthYear = itemView.findViewById(R.id.beneficiaryBirthYear);
            beneficiaryPhotoId = itemView.findViewById(R.id.beneficiaryPhotoId);
            beneficiaryIdNumber = itemView.findViewById(R.id.beneficiaryIdNumber);
            beneficiaryVaccinationInformation = itemView.findViewById(R.id.beneficiaryVaccinationInformation);
            beneficiaryReferenceId = itemView.findViewById(R.id.beneficiaryReferenceId);
            beneficiarySecretCode = itemView.findViewById(R.id.beneficiarySecretCode);
        }

        @Override
        public void onClick(View view) { Log.d("ClickFromViewHolder", "Clicked"); }
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder{
        public TextView registeredMobileNumber;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            registeredMobileNumber = itemView.findViewById(R.id.registeredMobileNumber);
        }
    }
}
