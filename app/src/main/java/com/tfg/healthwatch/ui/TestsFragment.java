package com.tfg.healthwatch.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tfg.healthwatch.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
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
    private DatabaseReference responseData;
    private String language;
    private ArrayList questions = new ArrayList();
    private TextView questionText;
    private EditText responseText;
    private Button saveButton;
    private Integer currentQuestion = 0;
    private SpeechRecognizer speechRecognizer;
    private RecyclerView questionsList;

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
        public int selectedPuntuation;
        public String id;

        public Question(String text, int selectedPuntuation, String id) {
            this.text = text;
            this.selectedPuntuation = selectedPuntuation;
            this.id = id;
        }

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

                LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
                String stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();

                responseData = FirebaseDatabase.getInstance().getReference().child("Responses").child(currentUser.getUid()).child(stringDate);
                DatabaseReference table = testData.child("error").child(language);;
                switch(mType){
                    case "energy":
                    case "habit":
                        if(mType == "energy") table = testData.child("energy").child(language);
                        else if(mType == "habit") table = testData.child("habit").child(language);

                        table.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                questions = new ArrayList<String>();

                                for (DataSnapshot child: snapshot.getChildren()) {
                                    String post = child.getValue().toString();
                                    Log.e("Value " ,post);
                                    questions.add(post);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        break;
                    case "goal":
                        break;
                    default:
                        Log.e(TAG,"Error getting test type");
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_tests, container, false);

        // Set the adapter
        questionsList = root.findViewById(R.id.goals_list);
        //goalTable.child(currentUser.getUid()).push().setValue(new Goal("Test goal","custom"));

        ArrayList<TestsFragment.Question> questionArrayList = new ArrayList<TestsFragment.Question>();

        for (String child: questions) {
            
            Boolean status = (Boolean) child.child("status").getValue();

            goalArrayList.add(new GoalsFragment.Goal(text,type,status,child.getKey()));
        }
        GoalListAdapater adapter = new GoalListAdapater(getContext(), goalArrayList);
        goalList.setAdapter(adapter);
        goalList.setLayoutManager(new LinearLayoutManager(getContext()));




        saveButton = root.findViewById(R.id.save_test_button);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    saveResponse();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        return root;
    }

    private void saveResponse() throws Exception {
        if(mType != "error"){

        }
        else{
            throw new Exception("Type not selected");
        }
    }
}