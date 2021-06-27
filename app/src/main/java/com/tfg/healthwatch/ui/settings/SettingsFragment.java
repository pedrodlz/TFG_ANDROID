package com.tfg.healthwatch.ui.settings;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.tfg.healthwatch.MainActivity;
import com.tfg.healthwatch.R;
import com.tfg.healthwatch.ui.login.LoginActivity;

import java.util.Locale;
import java.util.Objects;

public class SettingsFragment extends Fragment {

    private Button googleLogOut;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth firebaseAuth;
    private ConstraintLayout profileButton, bluetoothButton, themeButton, languageButton, aboutButton;


    FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            if (firebaseAuth.getCurrentUser() == null){
                //Do anything here which needs to be done after signout is complete
                signOutComplete();
            }
        }
    };

    private void signOutComplete() {
        if(isAdded()){
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        profileButton= root.findViewById(R.id.profile_button);
        bluetoothButton= root.findViewById(R.id.bluetooth_button);
        themeButton= root.findViewById(R.id.theme_button);
        languageButton= root.findViewById(R.id.language_button);
        aboutButton= root.findViewById(R.id.about_button);
        googleLogOut = root.findViewById(R.id.google_logout_btn);

        NavController navc = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);

        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navc.navigate(R.id.action_navigation_settings_to_navigation_profile);
            }
        });

        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navc.navigate(R.id.action_navigation_settings_to_navigation_bluetooth);
            }
        });

        languageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext(),R.style.AlertDialogCustom);
                String[] languages = getResources().getStringArray(R.array.languages_list);
                builder.setItems(languages, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String option = languages[which];

                        if(!option.isEmpty()){

                            if(option.equals(getString(R.string.english))){
                                option = "en";
                            }
                            else if(option.equals(getString(R.string.spanish))){
                                option = "es";
                            }
                            else option = "-1";

                            if(!option.equals("-1")){
                                Log.d("OPTION CORRECT: ", option);
                                Locale locale = new Locale(option);
                                Locale.setDefault(locale);

                                Resources resources = getContext().getResources();
                                Configuration configuration = resources.getConfiguration();
                                configuration.setLocale(locale);

                                getContext().createConfigurationContext(configuration);
                                resources.updateConfiguration(configuration, resources.getDisplayMetrics());

                                //Reload fragment
                                if (getParentFragmentManager() != null) {

                                    getParentFragmentManager()
                                            .beginTransaction()
                                            .detach(SettingsFragment.this)
                                            .attach(SettingsFragment.this)
                                            .commit();
                                }
                            }
                        }
                    }
                });
                builder.show();
            }
        });

        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext(),R.style.AlertDialogCustom);
                builder.setTitle(getResources().getString(R.string.about_string));
                builder.setMessage(getString(R.string.about_information_text));
                builder.show();
            }
        });

        googleLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                mGoogleSignInClient = LoginActivity.getmGoogleSignInClient();

                if(mGoogleSignInClient != null){
                    mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            signOutComplete();
                        }
                    });
                }

            }
        });

        return root;
    }
}