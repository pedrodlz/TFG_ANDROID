package com.tfg.healthwatch.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tfg.healthwatch.R;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FirebaseUser currentUser;
    private DatabaseReference userTable;
    private EditText mName, mSurname, mDateBirth, mGender, mHeight, mWeight;
    private Button saveButton;

    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userTable = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUser.getUid());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        mName = root.findViewById(R.id.editTextName);
        mSurname  = root.findViewById(R.id.editTextSurname);
        mDateBirth = root.findViewById(R.id.editTextDate);
        mGender = root.findViewById(R.id.editTextGender);
        mHeight = root.findViewById(R.id.editTextHeight);
        mWeight = root.findViewById(R.id.editTextWeight);
        saveButton = root.findViewById(R.id.save_button);

        userTable.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                if(snapshot.child("name").exists()){
                    mName.setText(snapshot.child("name").getValue().toString());
                }
                if(snapshot.child("surname").exists()){
                    mSurname.setText(snapshot.child("surname").getValue().toString());
                }
                if(snapshot.child("date").exists()){
                    mDateBirth.setText(snapshot.child("date").getValue().toString());
                }
                if(snapshot.child("gender").exists()){
                    mGender.setText(snapshot.child("gender").getValue().toString());
                }
                if(snapshot.child("height").exists()){
                    mHeight.setText(snapshot.child("height").getValue().toString());
                }
                if(snapshot.child("weight").exists()){
                    mWeight.setText(snapshot.child("weight").getValue().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name, surname, date, gender, height, weight;

                name = mName.getText().toString();
                surname = mSurname.getText().toString();
                date = mDateBirth.getText().toString();
                gender = mGender.getText().toString();
                height = mHeight.getText().toString();
                weight = mWeight.getText().toString();

                Map newPost = new HashMap();
                newPost.put("name",name);
                newPost.put("date",date);
                newPost.put("surname",surname);
                newPost.put("height",height);
                newPost.put("weight",weight);
                newPost.put("gender",gender);

                userTable.setValue(newPost);
            }
        });

        return root;
    }
}