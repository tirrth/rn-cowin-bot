package com.example.cowintest.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cowintest.R;
import com.example.cowintest.model.VaccinationCenterModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RecyclerViewPreferredDistrictAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private JSONArray preferredDistrictList;
    private OnPreferredDistrictListener mOnPreferredDistrictListener;

    public RecyclerViewPreferredDistrictAdapter(JSONArray preferredDistrictList, OnPreferredDistrictListener mOnPreferredDistrictListener){
        this.preferredDistrictList = preferredDistrictList;
        this.mOnPreferredDistrictListener = mOnPreferredDistrictListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.district_card, parent, false);
        return new ViewHolder(view, mOnPreferredDistrictListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            JSONObject preferredDistrict = (JSONObject) preferredDistrictList.get(position);
            final ViewHolder itemViewHolder = (ViewHolder) holder;
            Log.d("preferredDistrict", preferredDistrict.toString());
            itemViewHolder.districtName.setText(preferredDistrict.optJSONObject("district_info").optString("district_name"));
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return preferredDistrictList.length();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView districtName;
        private ImageView removePreferredDistrictImageView;
        // public OnPreferredDistrictListener onPreferredDistrictListener;

        public ViewHolder(@NonNull View itemView, OnPreferredDistrictListener onPreferredDistrictListener) {
            super(itemView);
            // itemView.setOnClickListener(this); // Uncomment this to enable click-listener event...
            districtName = itemView.findViewById(R.id.preferredDistrictName);
            mOnPreferredDistrictListener = onPreferredDistrictListener;
            removePreferredDistrictImageView = itemView.findViewById(R.id.removePreferredDistrictImageView);
            removePreferredDistrictImageView.setOnClickListener(v -> mOnPreferredDistrictListener.onRemovePreferredDistrictClick(getBindingAdapterPosition()));
        }

        @Override
        public void onClick(View view) { Log.d("ClickFromViewHolder", "Clicked"); }
    }

    public interface OnPreferredDistrictListener{
        void onRemovePreferredDistrictClick(int position);
    }
}
