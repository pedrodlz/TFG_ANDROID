package com.tfg.healthwatch.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
    private TextView questionText, microphoneStatus;
    private EditText responseText;
    private ImageView cancelButton, microphoneButton, saveButton;
    private Integer currentQuestion = 0;
    private SpeechRecognizer speechRecognizer;

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

                                try {
                                    nextQuestion();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                        break;
                    case "goal":
                        break;
                    case "meaning":
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

        questionText = root.findViewById(R.id.test_text);
        microphoneStatus = root.findViewById(R.id.microphone_status);
        responseText = root.findViewById(R.id.response_text);
        cancelButton = root.findViewById(R.id.cancel_test_button);
        microphoneButton = root.findViewById(R.id.microphone_response_button);
        saveButton = root.findViewById(R.id.save_response_button);

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

        checkPermission();

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getActivity());

        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {
                microphoneStatus.setText("Escuchando...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
                //microphoneButton.setImageResource(R.drawable.ic_baseline_mic_off_24);
                Log.e(TAG, String.valueOf(error));
            }

            @Override
            public void onResults(Bundle results) {
                //microphoneButton.setImageResource(R.drawable.ic_baseline_mic_off_24);
                ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                String result = data.get(0);
                responseText.setText(result);
                microphoneStatus.setText("");
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });

        microphoneButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP){
                    speechRecognizer.stopListening();
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    //microphoneButton.setImageResource(R.drawable.ic_baseline_mic_24);
                    speechRecognizer.startListening(speechRecognizerIntent);
                }

                return false;
            }
        });

        if (mType == "meaning") {
            try {
                nextQuestion();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return root;
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(getActivity(),new String[]{
                    Manifest.permission.RECORD_AUDIO
            },1);
        }
    }

    private void nextQuestion() throws Exception {
        if(mType != "error"){
            // Show next
            if(currentQuestion < questions.size()){
                questionText.setText(questions.get(currentQuestion).toString());
            }
            else if(mType == "meaning"){
                questionText.setText("Cuentame como te sientes");
            }
            else{
                //Exit test
                Toast.makeText(getContext(),"Test finished!",Toast.LENGTH_SHORT).show();
            }
        }
        else{
            throw new Exception("Type not selected");
        }
    }

    private void saveResponse() throws Exception {
        if(mType != "error"){
            if(currentQuestion < questions.size()){
                String questionIndex = currentQuestion+"";
                String response = responseText.getText().toString();
                responseData.child(mType)
                        .child(questionIndex)
                        .setValue(response)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                responseText.setText("");
                                currentQuestion++;
                                try {
                                    nextQuestion();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
            }
            else if(mType == "meaning"){
                sendDataToApiMeaning();
            }
        }
        else{
            throw new Exception("Type not selected");
        }
    }

    private void sendDataToApiMeaning(){
        // Instantiate the RequestQueue.
        try{
            RequestQueue queue = Volley.newRequestQueue(getContext());
            String url ="https://api.meaningcloud.com/sentiment-2.1?key=74833d8cd376c37a060366a8c88b529c&lang="+language;
            String textToAnalyze = responseText.getText().toString();
            url += "&txt="+textToAnalyze;

            // Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String  response) {
                            // Display the first 500 characters of the response string.
                            Log.i("VOLLEY", response);

                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            microphoneStatus.setText("That didn't work!");
                            Log.e("VOLLEY", error.toString());

                        }
            });

            // Add the request to the RequestQueue.
            queue.add(stringRequest);
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}