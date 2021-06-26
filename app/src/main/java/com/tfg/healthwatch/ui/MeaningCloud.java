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
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;


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
    private ArrayList<String> positive = new ArrayList<String>();
    private ArrayList<String> negative = new ArrayList<String>();
    private int maxRate=-1, minRate=-1, avgRate=-1;
    private TextToSpeech ttobj;

    public MeaningCloud() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        language = Locale.getDefault().getLanguage();

        LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
        String stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();
        responseData = FirebaseDatabase.getInstance().getReference().child("Responses").child(currentUser.getUid()).child(stringDate).child("meaningCloud");

        ttobj = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    ttobj.setLanguage(Locale.getDefault());
                }
            }
        });

        getMinMaxRates();
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

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                responseText.setText("");
                ttobj.stop();
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
                microphoneButton.setColorFilter(getResources().getColor(R.color.custom_dark_grey));
            }

            @Override
            public void onError(int error) {
                //microphoneButton.setImageResource(R.drawable.ic_baseline_mic_off_24);
                Log.e(TAG, String.valueOf(error));
                microphoneStatus.setText("Error");
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
                    microphoneButton.setColorFilter(getResources().getColor(R.color.third));
                    speechRecognizer.startListening(speechRecognizerIntent);
                }

                return false;
            }
        });

        return root;
    }

    @Override
    public void onDestroy() {
        ttobj.stop();
        speechRecognizer.destroy();
        super.onDestroy();
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
            case "P":
                if (!positive.contains(value)) {
                    positive.add(value);
                }
                break;
            case "N":
            case "N+":
                if (!negative.contains(value)) {
                    negative.add(value);
                }
                break;
        }
    }

    private void buildResponseToUser(){

        String response= "¡Hola! Según mi análisis puedo concluir que estás ";
        boolean highRate = false;
        boolean lowRate = false;


        switch (feeling){
            case "P+":
            case "P":
                response += "bien!\n";

                if(maxRate >= avgRate+30){
                    highRate = true;
                    response += "Veo que en algún momento del día has tenido las pulsaciones altas con un pico de " + maxRate + ".\n";
                    response += "Si has hecho deporte es algo normal, pero si has mantenido reposo "+
                            "deberías tener cuidado y vigilar tu estado de salud. Un alto nivel en tu " +
                            "frecuencia cardiovascular y estado de ánimo puede deberse a alguna buena " +
                            "noticia o una sorpresa insesperada! No te excites demasiado e intentar calmarte. " +
                            "Si en unos días sigue así es recomendable que acudas a tu médico de cabecera.\n";
                }

                if(minRate <= avgRate-30){
                    lowRate = true;
                    if(highRate) response += "Además de tener las pulsaciones altas, también estoy viendo ";
                    else response += "Veo ";

                    response += "que en algún momento del día has tenido las pulsaciones bajas, con un pico de " + minRate + ".\n";
                    response += "Esta bajada de pulsaciones puede deberse a que has alcanzado un nivel de " +
                                "salud muy bueno si has estado haciendo deporte y poniéndote en forma, ¡sigue asi!\n";
                    response += "La tranquilidad y un buen estado de ánimo favorece la salud del corazón. ";
                    response += "Sin embargo, si tienes las pulsaciones bajas durante varios días y además " +
                                "tienes sensaciones de mareo, náuseas o vómitos, es mejor que acudas a tu médico.\n";
                }

                if(!highRate && !lowRate){
                    response += "¡Me alegro de que estés bien! Hoy has tenido un día normal sin ningún cambio " +
                            "de salud importante. Tus pulsaciones se han mantenido muy bien en la media y no hay " +
                            "cambios bruscos destacables. ¡Sigue así! ¡Lo estás haciendo muy bien!\n";
                }

                break;
            case "NEU":
            case "NONE":
                response += "normal.\n";

                if(maxRate >= avgRate+30){
                    highRate = true;
                    response += "Veo que en algún momento del día has tenido las pulsaciones altas con un pico de " + maxRate + ".\n";
                    response += "Si has hecho deporte es algo normal, pero si has mantenido reposo "+
                            "deberías tener cuidado y vigilar tu estado de salud. Un alto nivel en tu " +
                            "frecuencia cardiovascular puede deberse a una alteración fisiológica o posible "+
                            "enfermedad cardiovascular. Es importante descartar esta opción, por lo que "+
                            "si en unos días sigue así es recomendable que acudas a tu médico de cabecera.\n";
                }

                if(minRate <= avgRate-30){
                    lowRate = true;
                    if(highRate) response += "Además de tener las pulsaciones altas, también estoy viendo ";
                    else response += "Veo ";

                    response += "que en algún momento del día has tenido las pulsaciones bajas, con un pico de " + minRate + ".\n";
                    response += "Esta bajada de pulsaciones puede deberse a que hayas alcanzado un buen nivel de " +
                            "salud, es posible que la adquisición de mejores hábitos haya reducido la presión arterial. ";
                    response += "La tranquilidad y un buen estado de ánimo favorece la salud del corazón.\n";
                    response += "Sin embargo, si tienes las pulsaciones bajas durante varios días y además " +
                            "tienes sensaciones de mareo, náuseas o vómitos, es mejor que acudas a tu médico.\n";
                }

                if(!highRate && !lowRate){
                    response += "Hoy has tenido un día normal sin ningún cambio de salud importante. " +
                            "Tus pulsaciones se han mantenido muy bien en la media y no hay cambios bruscos "+
                            "destacables. Además tu estado emocional es neutro, si quieres mejorarlo, una buena "+
                            "forma de hacerlo es mediante el deporte. ¡Mucho ánimo!\n";
                }
                break;
            case "N":
            case "N+":
                response += "mal.\n";
                if(maxRate >= avgRate+30){
                    highRate = true;
                    response += "Veo que en algún momento del día has tenido las pulsaciones altas con un pico de " + maxRate + ".\n";
                    response += "Si has hecho deporte es algo normal, pero si has mantenido reposo "+
                            "deberías tener cuidado y vigilar tu estado de salud. Un alto nivel en tu " +
                            "frecuencia cardiovascular y un mal estado de ánimo puede deberse a "+
                            "que estás triste o a una posible depresión. La tristeza provoca un alto "+
                            "nivel de pulsaciones, ya que al estar triste nuestro cuerpo libera adrenalina. " +
                            "También puede deberse a situaciones de miedo y/o estrés. Si en unos días sigues así "+
                            "es recomendable que acudas a un psicólogo o a tu médico de cabecera.\n";
                }

                if(minRate <= avgRate-30){
                    lowRate = true;
                    if(highRate) response += "Además de tener las pulsaciones altas, también estoy viendo ";
                    else response += "Veo ";

                    response += "que en algún momento del día has tenido las pulsaciones bajas, con un pico de " + minRate + ".\n";
                    response += "Esta bajada de pulsaciones puede deberse a que hayas alcanzado un nivel de " +
                            "salud, es posible que la adquisición de mejores hábitos haya reducido la presión arterial.\n";
                    response += "Sin embargo,tambien puede deberse a que el corazón no esté bombeando suficiente sangre "+
                            " y oxígeno, lo que puede causar fatiga y otros síntomas del malestar.\n";
                    response += "Si tienes las pulsaciones bajas durante varios días y además " +
                            "tienes sensaciones de mareo, náuseas, vómitos o malestares, es mejor que acudas a tu médico.\n";
                }

                if(!highRate && !lowRate){
                    response += "Me preocupa que hayas tenido un mal día. Si hay algo que te preocupa o "+
                            "inquieta es importante contar con la familia y amigos para pedirles consejo "+
                            "y ayuda. Por otro lado también puedes acudir a un psicólogo que te ayudará seguro! "+
                            "En cuanto a las pulsaciones, hoy has tenido un día estable sin ningún cambio de salud importante. " +
                            "Tus pulsaciones se han mantenido muy bien en la media y no hay cambios bruscos "+
                            "destacables. Además si quieres mejorarlo, una buena forma de hacerlo es mediante el deporte. ¡Mucho ánimo!\n";
                }
                break;
        }

        if(positive.size() > 0 || negative.size() > 0){
            boolean pos = false,neg = false;
            response += "\nFinalmente, también es destacable añadir los elementos que, según mi analisis, te afectan";

            if(positive.size() > 0){
                pos = true;
                response += " positivamente como ";

                for(String word : positive){
                    response += word + ", ";
                }
            }

            if(negative.size() > 0){
                if(pos) response += ". Y negativamente como ";
                else response+= " negativamente como ";

                for(String word : negative){
                    response += word + ", ";
                }
            }
        }

        responseText.setText(response);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Call Lollipop+ function
            ttobj.speak(response, TextToSpeech.QUEUE_FLUSH, null, null) ;
        }
        else {
            // Call Legacy function
            ttobj.speak(response, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void getMinMaxRates(){
        Log.d(TAG,"getMinMaxRates()");
        LocalDate date= LocalDate.now( ZoneOffset.UTC ) ;
        String stringDate= "" + date.getDayOfMonth() + date.getMonthValue() + date.getYear();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        activityTable = FirebaseDatabase.getInstance().getReference().child("Activity").child(currentUser.getUid()).child(stringDate);

        activityTable.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {

                if(snapshot.child("Average Heart Rate").exists()){
                    avgRate = snapshot.child("Average Heart Rate").getValue(int.class);
                    Log.d(TAG,"Average Heart Rate: "+avgRate);
                }
                if(snapshot.child("Heart Rates").exists()){
                    for(DataSnapshot rate : snapshot.child("Heart Rates").getChildren()){
                        if(maxRate == -1) maxRate = rate.getValue(int.class);
                        if(minRate == -1) minRate = rate.getValue(int.class);
                        if(rate.getValue(int.class) > maxRate) maxRate = rate.getValue(int.class);
                        if(rate.getValue(int.class) < minRate) minRate = rate.getValue(int.class);
                    }
                    Log.d(TAG,"Max heart rate: "+maxRate);
                    Log.d(TAG,"Min heart rate: "+minRate);
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }
}