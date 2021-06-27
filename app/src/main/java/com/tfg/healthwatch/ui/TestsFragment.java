package com.tfg.healthwatch.ui;

import android.annotation.SuppressLint;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Locale;

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
    private DatabaseReference responseData;
    private String language, high, medium, low;
    private ArrayList<TestsFragment.Question> questions = new ArrayList<TestsFragment.Question>();;
    private TextView questionText;
    private EditText responseText;
    private Button saveButton;
    private Integer currentQuestion = 0;
    private SpeechRecognizer speechRecognizer;
    private RecyclerView questionsList;
    private TestListAdapter adapter;

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

    public static class Question {

        public String text;
        public int selectedPunctuation;
        public String id;

        public Question(String text, int selectedPunctuation, String id) {
            this.text = text;
            this.selectedPunctuation = selectedPunctuation;
            this.id = id;
        }

    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        responseData.removeEventListener(responseValues);
    }

    ValueEventListener responseValues = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
            int punctuation = 0;
            String finalResponse = null;
            for(DataSnapshot response: snapshot.getChildren()){
                punctuation += response.getValue(int.class);
            }

            if(punctuation >= 34 && punctuation <= 50){
                finalResponse = high;
            }
            else if(punctuation >= 17 && punctuation <= 33){
                finalResponse = medium;
            }
            else if(punctuation >= 0 && punctuation <= 16){
                finalResponse = low;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext(),R.style.AlertDialogCustom);
            builder.setTitle(getString(R.string.results));
            builder.setMessage(finalResponse);
            builder.show();
        }

        @Override
        public void onCancelled(@NonNull @NotNull DatabaseError error) {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mType = getArguments().getString(ARG_PARAM1);
            Log.d(TAG,"Selected type: "+ mType);

            currentUser = FirebaseAuth.getInstance().getCurrentUser();
            language = Locale.getDefault().getLanguage();
            testData = FirebaseDatabase.getInstance().getReference().child("Tests").child(mType).child(language);

            LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
            String stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();

            responseData = FirebaseDatabase.getInstance().getReference().child("Responses").child(currentUser.getUid()).child(stringDate).child(mType);

        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_tests, container, false);

        // Set the adapter
        questionsList = root.findViewById(R.id.test_question_list);

        if(mType != null){
            switch(mType){
                case "energy":
                case "habit":

                    testData.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                            high = snapshot.child("verdict").child("high").getValue().toString();
                            medium = snapshot.child("verdict").child("medium").getValue().toString();
                            low = snapshot.child("verdict").child("low").getValue().toString();

                            for (DataSnapshot child: snapshot.child("questions").getChildren()) {
                                String post = child.getValue().toString();
                                Log.d(TAG,post);
                                questions.add(new Question(post,0,child.getKey()));
                            }
                            adapter = new TestListAdapter(getContext(), questions,mType);
                            questionsList.setAdapter(adapter);
                            questionsList.setLayoutManager(new LinearLayoutManager(getContext()));
                        }

                        @Override
                        public void onCancelled(@NonNull @NotNull DatabaseError error) {

                        }
                    });

                    break;
                case "goal":
                    break;
                default:
                    Log.e(TAG,"Error getting test type");
            }
        }

        saveButton = root.findViewById(R.id.save_test_button);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    buildResponse();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        return root;
    }

    private void buildResponse() throws Exception {
        if(mType != "error"){
            responseData.addValueEventListener(responseValues);
        }
        else{
            throw new Exception("Type not selected");
        }
    }
}