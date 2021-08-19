package com.example.cowintest.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cowintest.R;
import com.example.cowintest.model.BeneficiaryModel;

import java.util.List;

public class RecyclerViewPreferredBeneficiaryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<BeneficiaryModel> preferredBeneficiaryList;
    private OnPreferredBeneficiaryListener mOnPreferredBeneficiaryListener;

     public RecyclerViewPreferredBeneficiaryAdapter(List<BeneficiaryModel> preferredDistrictList, OnPreferredBeneficiaryListener mOnPreferredBeneficiaryListener){
         this.preferredBeneficiaryList = preferredDistrictList;
         this.mOnPreferredBeneficiaryListener = mOnPreferredBeneficiaryListener;
     }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.preferred_beneficiary_card, parent, false);
        return new ViewHolder(view, mOnPreferredBeneficiaryListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BeneficiaryModel preferredBeneficiary = preferredBeneficiaryList.get(position);
        final ViewHolder itemViewHolder = (ViewHolder) holder;
        // Log.d("preferredBeneficiary", preferredBeneficiary.toString());
        itemViewHolder.beneficiaryName.setText(preferredBeneficiary.getName());
        GradientDrawable bgShape = (GradientDrawable) holder.itemView.getBackground();
        GradientDrawable bgIndicatorShape = (GradientDrawable) holder.itemView.findViewById(R.id.preferredBeneficiaryCustomRadioButton).getBackground();
        final int selectedColor = preferredBeneficiary.is45Plus() ? Color.argb(30, 66, 133, 244) : Color.argb(30, 61, 220, 132);
        final int selectedStrokeColor = preferredBeneficiary.is45Plus() ? Color.argb(255, 66, 133, 244) : Color.argb(255, 61, 220, 132);
        final int selectedIndicatorColor = preferredBeneficiary.is45Plus() ? Color.argb(150, 66, 133, 244) : Color.argb(150, 61, 220, 132);
        final int normalColor = Color.WHITE;
        final int normalStrokeColor = Color.argb(255, 204, 204, 204);
        final int normalIndicatorColor = Color.argb(255, 221, 221, 221);
        bgShape.setColor(preferredBeneficiary.isSelected() ? selectedColor : normalColor);
        bgShape.setStroke(1, preferredBeneficiary.isSelected() ? selectedStrokeColor : normalStrokeColor);
        bgIndicatorShape.setColor(preferredBeneficiary.isSelected() ? selectedIndicatorColor : normalIndicatorColor);
        holder.itemView.setOnClickListener(view -> {
            preferredBeneficiary.setSelected(!preferredBeneficiary.isSelected());
            bgShape.setColor(preferredBeneficiary.isSelected() ? selectedColor : normalColor);
            bgShape.setStroke(1, preferredBeneficiary.isSelected() ? selectedStrokeColor : normalStrokeColor);
            bgIndicatorShape.setColor(preferredBeneficiary.isSelected() ? selectedIndicatorColor : normalIndicatorColor);
            if (preferredBeneficiary.is1stDoseGiven() && !preferredBeneficiary.is2ndDoseGiven()) Log.d("Options", "Well, It is working...");
            else Log.d("Options", "Well, It is not working...");
            mOnPreferredBeneficiaryListener.onSelectPreferredBeneficiaryClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return preferredBeneficiaryList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView beneficiaryName;

        public ViewHolder(@NonNull View itemView, OnPreferredBeneficiaryListener onPreferredBeneficiaryListener) {
            super(itemView);
            beneficiaryName = itemView.findViewById(R.id.preferredBeneficiaryName);
            mOnPreferredBeneficiaryListener = onPreferredBeneficiaryListener;
        }
    }

    public interface OnPreferredBeneficiaryListener{
        void onSelectPreferredBeneficiaryClick(int position);
    }
}
