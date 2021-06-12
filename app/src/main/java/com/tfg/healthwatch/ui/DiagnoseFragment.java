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

import com.tfg.healthwatch.R;

public class DiagnoseFragment extends Fragment {

    private ConstraintLayout energyButton, habitButton, goalButton, meaningButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_diagnose, container, false);

        energyButton = root.findViewById(R.id.energy_test);
        habitButton = root.findViewById(R.id.habit_test);
        goalButton = root.findViewById(R.id.goal_test);
        meaningButton = root.findViewById(R.id.meaning_cloud_button);

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