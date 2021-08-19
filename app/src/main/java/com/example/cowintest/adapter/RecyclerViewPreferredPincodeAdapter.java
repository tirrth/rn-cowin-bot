package com.example.cowintest.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cowintest.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RecyclerViewPreferredPincodeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private JSONArray preferredPincodeList;
    private OnPreferredPincodeListener mOnPreferredPincodeListener;

    public RecyclerViewPreferredPincodeAdapter(JSONArray preferredDistrictList, OnPreferredPincodeListener mOnPreferredPincodeListener){
        this.preferredPincodeList = preferredDistrictList;
        this.mOnPreferredPincodeListener = mOnPreferredPincodeListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pincode_card, parent, false);
        return new ViewHolder(view, mOnPreferredPincodeListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        try {
            JSONObject preferredPincode = (JSONObject) preferredPincodeList.get(position);
            final ViewHolder itemViewHolder = (ViewHolder) holder;
            Log.d("preferredPincode", preferredPincode.toString());
            itemViewHolder.pincode.setText(preferredPincode.optString("pincode"));
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return preferredPincodeList.length();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView pincode;
        private ImageView removePreferredPincodeImageView;

        public ViewHolder(@NonNull View itemView, OnPreferredPincodeListener onPreferredPincodeListener) {
            super(itemView);
            // itemView.setOnClickListener(this); // Uncomment this to enable click-listener event...
            pincode = itemView.findViewById(R.id.preferredPincode);
            mOnPreferredPincodeListener = onPreferredPincodeListener;
            removePreferredPincodeImageView = itemView.findViewById(R.id.removePreferredPincodeImageView);
            removePreferredPincodeImageView.setOnClickListener(v -> mOnPreferredPincodeListener.onRemovePreferredPincodeClick(getBindingAdapterPosition()));
        }

        @Override
        public void onClick(View view) { Log.d("ClickFromViewHolder", "Clicked"); }
    }

    public interface OnPreferredPincodeListener{
        void onRemovePreferredPincodeClick(int position);
    }
}
