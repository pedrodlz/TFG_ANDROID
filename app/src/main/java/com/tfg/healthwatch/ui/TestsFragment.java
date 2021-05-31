package com.tfg.healthwatch.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tfg.healthwatch.R;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TestsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TestsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "type";

    // TODO: Rename and change types of parameters
    private String mType;
    private static String TAG = "TestFragment";
    private FirebaseUser currentUser;
    private DatabaseReference testData;
    private String language;
    private ArrayList questions;

    public TestsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param type Parameter 1.
     * @return A new instance of fragment TestsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TestsFragment newInstance(String type) {
        TestsFragment fragment = new TestsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mType = getArguments().getString(ARG_PARAM1);

            if(mType != null){
                currentUser = FirebaseAuth.getInstance().getCurrentUser();
                language = Locale.getDefault().getLanguage();
                testData = FirebaseDatabase.getInstance().getReference().child("Tests");
                switch(mType){
                    case "energy":
                        testData = testData.child("energy").child(language);

                        testData.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String value = snapshot.child("0").getValue().toString();

                                Log.d(TAG, value);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        break;
                    case "habit":
                        break;
                    case "goal":
                        break;
                    default:
                        Log.e(TAG,"Error getting test type");

                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_tests, container, false);

        TextView testText = root.findViewById(R.id.test_text);

        testText.setText(mType);

        return root;
    }
}