package com.tfg.healthwatch.ui;

import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tfg.healthwatch.R;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

public class DiagnoseFragment extends Fragment {

    private ConstraintLayout energyButton, habitButton, goalButton, meaningButton;
    private ImageButton mVerySad, mSad, mNormal, mSmile,mHappy;
    private FirebaseUser currentUser;
    private DatabaseReference activityTable;
    private String stringDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
        stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();
        activityTable = FirebaseDatabase.getInstance().getReference().child("Activity").child(currentUser.getUid()).child(stringDate);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_diagnose, container, false);

        mVerySad = root.findViewById(R.id.verySadFace);
        mSad = root.findViewById(R.id.sadFace);
        mNormal = root.findViewById(R.id.normalFace);
        mSmile = root.findViewById(R.id.smileFace);
        mHappy = root.findViewById(R.id.veryHappyFace);
        energyButton = root.findViewById(R.id.energy_test);
        habitButton = root.findViewById(R.id.habit_test);
        goalButton = root.findViewById(R.id.goal_test);
        meaningButton = root.findViewById(R.id.meaning_cloud_button);

        SimpleDateFormat ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss'Z'");
        String now = ISO_8601_FORMAT.format(new Date());


        mVerySad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { activityTable.child("generalFeeling").setValue(1);
            }
        });
        mSad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityTable.child("generalFeeling").setValue(2);
            }
        });
        mNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityTable.child("generalFeeling").setValue(3);
            }
        });
        mSmile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityTable.child("generalFeeling").setValue(4);
            }
        });
        mHappy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityTable.child("generalFeeling").setValue(5);
            }
        });

        NavController navc = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);

        energyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("type", "energy");
                navc.navigate(R.id.action_navigation_diagnose_to_navigation_tests,bundle);
            }
        });

        habitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("type", "habit");
                navc.navigate(R.id.action_navigation_diagnose_to_navigation_tests,bundle);
            }
        });

        goalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("type", "goal");
                navc.navigate(R.id.action_navigation_diagnose_to_navigation_tests,bundle);
            }
        });

        meaningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navc.navigate(R.id.action_navigation_diagnose_to_navigation_meaning_cloud);
            }
        });

        return root;
    }
}