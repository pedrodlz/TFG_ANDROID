package com.tfg.healthwatch.ui.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.tfg.healthwatch.BLEService;
import com.tfg.healthwatch.DashboardActivity;
import com.tfg.healthwatch.MainActivity;
import com.tfg.healthwatch.R;
import com.tfg.healthwatch.ui.login.LoginActivity;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private SettingsViewModel settingsViewModel;
    private Button googleLogOut;
    private GoogleSignInClient mGoogleSignInClient;
    private ConstraintLayout profileButton, bluetoothButton, themeButton, languageButton, aboutButton;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);
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
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(getResources().getString(R.string.language_string));

                String[] languages = getResources().getStringArray(R.array.languages_list);
                builder.setItems(languages, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String option = languages[which];

                        /*Locale myLocale = new Locale(lang);
                        Resources res = getResources();
                        DisplayMetrics dm = res.getDisplayMetrics();
                        Configuration conf = res.getConfiguration();
                        conf.setLocale(myLocale);
                        res.updateConfiguration(conf, dm);
                        Intent refresh = new Intent(this, AndroidLocalize.class);
                        finish();
                        startActivity(refresh);*/
                    }
                });
                builder.show();
            }
        });

        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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
                mGoogleSignInClient.signOut().addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    }
                });
            }
        });

        return root;
    }
}