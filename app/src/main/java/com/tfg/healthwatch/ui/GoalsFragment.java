package com.tfg.healthwatch.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tfg.healthwatch.R;
import com.tfg.healthwatch.ui.dummy.DummyContent;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class GoalsFragment extends Fragment {

    private ImageView mAddButton;
    private FirebaseUser currentUser;
    private DatabaseReference goalTable;
    private RecyclerView goalList;

    public GoalsFragment() {
    }

    public static class Goal {

        public String text;
        public Boolean status;
        public String type;
        public String id;

        public Goal(String text, String type, Boolean status,String id) {
            this.text = text;
            this.type = type;
            this.status = status;
            this.id = id;
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        goalTable = FirebaseDatabase.getInstance().getReference().child("Goals");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_goals, container, false);

        // Set the adapter
        goalList = view.findViewById(R.id.goals_list);
        //goalTable.child(currentUser.getUid()).push().setValue(new Goal("Test goal","custom"));
        goalTable.child(currentUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {

                ArrayList<Goal> goalArrayList = new ArrayList<Goal>();

                for (DataSnapshot child: snapshot.getChildren()) {
                    String text = child.child("value").getValue().toString();
                    String type = child.child("type").getValue().toString();
                    Boolean status = (Boolean) child.child("status").getValue();

                    goalArrayList.add(new Goal(text,type,status,child.getKey()));
                }
                GoalListAdapater adapter = new GoalListAdapater(getContext(), goalArrayList);
                goalList.setAdapter(adapter);
                goalList.setLayoutManager(new LinearLayoutManager(getContext()));
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        mAddButton = view.findViewById(R.id.add_goal_button);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] colors = getResources().getStringArray(R.array.goals_elements_list);

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(getResources().getString(R.string.goal_pick_list));
                builder.setItems(colors, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        AlertDialog.Builder inputBuilder = new AlertDialog.Builder(getContext());

                        String option = colors[which];
                        final String[] m_Text = new String[1];

                        inputBuilder.setTitle(option);
                        Map initPost = new HashMap();

                        final EditText input = new EditText(getContext());
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        inputBuilder.setView(input);

                        // Set up the buttons
                        inputBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!option.equals(getString(R.string.custom))){
                                    initPost.put("type",option);
                                }
                                else{
                                    initPost.put("type","custom");
                                }
                                initPost.put("value",input.getText().toString());
                                initPost.put("status",false);

                                TimeZone tz = TimeZone.getTimeZone("UTC");
                                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
                                df.setTimeZone(tz);

                                initPost.put("timeStamp",df.format(new Date()));
                                goalTable.child(currentUser.getUid()).push().setValue(initPost);
                            }
                        });
                        inputBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        inputBuilder.show();


                    }
                });
                builder.show();
            }
        });

        return view;
    }
}
