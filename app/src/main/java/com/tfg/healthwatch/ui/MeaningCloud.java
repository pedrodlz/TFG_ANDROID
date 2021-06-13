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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Locale;


public class MeaningCloud extends Fragment {

    private JSONObject resultsObject;
    private String language;
    private FirebaseUser currentUser;
    private DatabaseReference responseData;
    private DatabaseReference activityTable;
    private String TAG = "MeaningCloudResults";
    private String feeling;
    private TextView microphoneStatus, testText;
    private EditText responseText;
    private SpeechRecognizer speechRecognizer;
    private ImageView cancelButton, microphoneButton, saveButton;
    private ArrayList<String> veryPositive = new ArrayList<String>();
    private ArrayList<String> positive = new ArrayList<String>();
    private ArrayList<String> neutral = new ArrayList<String>();
    private ArrayList<String> negative = new ArrayList<String>();
    private ArrayList<String> veryNegative = new ArrayList<String>();

    public MeaningCloud() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        language = Locale.getDefault().getLanguage();

        LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
        String stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();
        responseData = FirebaseDatabase.getInstance().getReference().child("Responses").child(currentUser.getUid()).child(stringDate).child("meaningCloud");
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_meaning_cloud_result, container, false);
        testText = root.findViewById(R.id.test_text);
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

        return root;
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(getActivity(),new String[]{
                    Manifest.permission.RECORD_AUDIO
            },1);
        }
    }

    private void saveResponse() throws Exception {
        String response = responseText.getText().toString();
        responseData.child("responseText")
            .setValue(response)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    sendDataToApiMeaning();
                }
            });
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
                            //Log.i("VOLLEY", response);
                            /*NavController navc = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
                            Bundle bundle = new Bundle();
                            bundle.putString("test_results", response);
                            navc.navigate(R.id.action_navigation_tests_to_navigation_meaning_cloud,bundle);*/
                            try {
                                resultsObject = new JSONObject(response);
                                responseText.setText("");
                                treatResponse();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

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

    private void treatResponse() throws JSONException {
        Log.d(TAG,"API results: "+ resultsObject);
        feeling = resultsObject.getString("score_tag");
        responseData.child("textFeeling").setValue(feeling);

        JSONArray entityList = new JSONArray(resultsObject.getString("sentimented_entity_list"));
        JSONArray conceptList = new JSONArray(resultsObject.getString("sentimented_concept_list"));

        for(int i=0; i<entityList.length();i++){
            JSONObject value = (JSONObject) entityList.get(i);
            classifyValue(value.getString("score_tag"),value.getString("form"));
        }

        for(int i=0; i<conceptList.length();i++){
            JSONObject value = (JSONObject) conceptList.get(i);
            classifyValue(value.getString("score_tag"),value.getString("form"));
        }

        buildResponseToUser();
    }

    private void classifyValue(String score, String value){
        switch(score){
            case "P+":
                veryPositive.add(value);
                break;
            case "P":
                positive.add(value);
                break;
            case "NEU":
                neutral.add(value);
                break;
            case "N":
                negative.add(value);
                break;
            case "N+":
                veryNegative.add(value);
                break;
        }
    }

    private void buildResponseToUser(){
        String response= "¡Hola! Según mi analisis veo que estás ";

        LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
        String stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        activityTable = FirebaseDatabase.getInstance().getReference().child("Activity").child(currentUser.getUid()).child(stringDate);

        switch (feeling){
            case "P+":
                response += "estupendamente!";
                break;
            case "P":
                response += "bien!";
                break;
            case "NEU":
                response += "normal";
                break;
            case "N":
                response += "mal";
                break;
            case "N+":
                response += "muy mal";
                break;
        }

        response+="\n";

        activityTable.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                if(snapshot.child("Average Heart Rate").exists()){

                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        testText.setText("Resultados");
        responseText.setText(response);

    }
}