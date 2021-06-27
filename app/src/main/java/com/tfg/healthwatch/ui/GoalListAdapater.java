package com.tfg.healthwatch.ui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tfg.healthwatch.R;

import java.util.ArrayList;

public class GoalListAdapater extends RecyclerView.Adapter<GoalListAdapater.Goal> {

    private Context context;
    private ArrayList<GoalsFragment.Goal> goalsList;
    private FirebaseUser currentUser;
    private DatabaseReference goalTable;

    public GoalListAdapater(Context ct, ArrayList<GoalsFragment.Goal> goals){
        context = ct;
        goalsList = goals;

    }

    @NonNull
    @Override
    public Goal onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.goal_list_item,parent, false);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        goalTable = FirebaseDatabase.getInstance().getReference().child("Goals").child(currentUser.getUid());

        return new Goal(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Goal holder, int position) {
        String showText = goalsList.get(position).text;

        if(!goalsList.get(position).type.equals("custom")){
            String[] text = goalsList.get(position).type.split(" ");
            if(text.length > 1){
                showText = text[0] + " " + goalsList.get(position).text + " " + text[1];
            }
            else{
                showText = goalsList.get(position).type + " " + goalsList.get(position).text;
            }
        }

        holder.goalText.setText(showText);
        holder.goalStatus.setChecked(goalsList.get(position).status);

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goalTable.child(goalsList.get(position).id).removeValue();
            }
        });

        holder.goalStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                goalTable.child(goalsList.get(position).id).child("status").setValue(isChecked);
            }
        });

    }

    @Override
    public int getItemCount() {
        return goalsList.size();
    }

    public class Goal extends RecyclerView.ViewHolder{

        TextView goalText;
        CheckBox goalStatus;
        ImageView deleteButton;

        public Goal(@NonNull View itemView) {
            super(itemView);

            goalText = itemView.findViewById(R.id.goal_text);
            goalStatus = itemView.findViewById(R.id.goal_checked);
            deleteButton = itemView.findViewById(R.id.delete_row);
        }
    }
}
