//package com.example.cowintest.adapter;
//
//import android.content.Context;
//import android.graphics.Color;
//import android.graphics.ColorSpace;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.cowintest.R;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//public class RecyclerViewVaccinationCenterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
//    private JSONArray centerList;
//    private OnVaccinationCenterListener mOnVaccinationCenterListener;
//    private int selectedPos = RecyclerView.NO_POSITION;
//
//    public RecyclerViewVaccinationCenterAdapter(JSONArray centerList, OnVaccinationCenterListener mOnVaccinationCenterListener){
//        this.centerList = centerList;
//        this.mOnVaccinationCenterListener = mOnVaccinationCenterListener;
//    }
//
//    @NonNull
//    @Override
//    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.center_list_card, parent, false);
//        return new ViewHolder(view, mOnVaccinationCenterListener);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
//        Log.d("position", "" + position);
//        // if(selectedPos == position) holder.itemView.setSelected(!holder.itemView.isSelected());
//        try {
//            JSONObject center = (JSONObject) centerList.get(position);
//            final ViewHolder itemViewHolder = (ViewHolder) holder;
//            // Log.d("Center", center.toString());
//            itemViewHolder.centerName.setText(center.optString("name"));
//            itemViewHolder.centerAddress.setText(" - ".concat(center.optString("address")));
////            holder.itemView.setOnClickListener(new View.OnClickListener() {
////                @Override
////                public void onClick(View v) {
////                    v.setBackgroundColor(Color.BLUE);
////                }
////            });
//        } catch (JSONException e){
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public int getItemCount() {
//        return centerList.length();
//    }
//
//    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
//        public TextView centerName;
//        public TextView centerAddress;
//        public OnVaccinationCenterListener onVaccinationCenterListener;
//
//        public ViewHolder(@NonNull View itemView) {
//            super(itemView);
//            centerName = itemView.findViewById(R.id.centerName);
//            centerAddress = itemView.findViewById(R.id.centerAddress);
//            itemView.setOnClickListener(this);
//        }
//
//        @Override
//        public void onClick(View v) {
//            // notifyItemChanged(selectedPos);
//            // selectedPos = getLayoutPosition();
//            // notifyItemChanged(selectedPos);
//            Log.d("ClickFromViewHolder", "Clicked");
//            onVaccinationCenterListener.onVaccinationCenterClick(getLayoutPosition());
//            // v.setBackgroundColor(Color.argb(50, 66, 133, 244));
//        }
//    }
//
//    public interface OnVaccinationCenterListener{
//        void onVaccinationCenterClick(int position);
//    }
//}

package com.example.cowintest.adapter;

import android.graphics.Color;
import android.graphics.ColorSpace;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cowintest.R;
import com.example.cowintest.model.VaccinationCenterModel;

import java.util.List;


public class RecyclerViewVaccinationCenterAdapter extends RecyclerView.Adapter<RecyclerViewVaccinationCenterAdapter.ViewHolder> {
    public List<VaccinationCenterModel> centerList;
    // private OnVaccinationCenterListener mOnVaccinationCenterListener;
    private boolean _isSelectAll = false;

    // public RecyclerViewVaccinationCenterAdapter(List<VaccinationCenterModel> centerList, OnVaccinationCenterListener onVaccinationCenterListener) {
    //     this.centerList = centerList;
    //     this.mOnVaccinationCenterListener = onVaccinationCenterListener;
    // }

    public RecyclerViewVaccinationCenterAdapter(List<VaccinationCenterModel> centerList) {
        this.centerList = centerList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.center_list_card, parent, false);
        // return new ViewHolder(view, mOnVaccinationCenterListener);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            final VaccinationCenterModel center = centerList.get(position);
            if(_isSelectAll) center.setSelected(true);
            final ViewHolder itemViewHolder = (ViewHolder) holder;
            itemViewHolder.centerName.setText(center.getName());
            itemViewHolder.centerAddress.setText(" - ".concat(center.getAddress()));
            holder.itemView.setBackgroundColor(center.isSelected() ? Color.argb(50, 66, 133, 244) : Color.WHITE);
            holder.itemView.setOnClickListener(view -> {
                // if(_isSelectAll) _isSelectAll = false;
                center.setSelected(!center.isSelected());
                holder.itemView.setBackgroundColor(center.isSelected() ? Color.argb(50, 66, 133, 244) : Color.WHITE);
                // mOnVaccinationCenterListener.onVaccinationCenterClick(position, centerList.get(position));
            });
        } catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return centerList != null ? centerList.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView centerName, centerAddress;
        // OnVaccinationCenterListener mOnVaccinationCenterListener;

        // public ViewHolder(View itemView, OnVaccinationCenterListener onNoteListener) {
        //     super(itemView);
        //     centerName = itemView.findViewById(R.id.centerName);
        //     centerAddress = itemView.findViewById(R.id.centerAddress);
        //     mOnVaccinationCenterListener = onNoteListener;
        // }

        public ViewHolder(View itemView) {
            super(itemView);
            centerName = itemView.findViewById(R.id.centerName);
            centerAddress = itemView.findViewById(R.id.centerAddress);
        }
    }

    public void setIsSelectAll(final boolean _isSelectAll){
        this._isSelectAll = _isSelectAll;
        for(int i = 0; i < getItemCount(); i++){
            centerList.get(i).setSelected(_isSelectAll);
        }
        notifyDataSetChanged();
    }

    public boolean isSelectAll(){
        return _isSelectAll;
    }

    // public interface OnVaccinationCenterListener{
    //     void onVaccinationCenterClick(int position, VaccinationCenterModel selectedCenter);
    // }
}
