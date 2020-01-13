package com.example.navigatorz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

public class NavigationFragment extends Fragment {

    private Button navButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.navigation_fragment,container,false);
        MapsActivity activity = (MapsActivity) getActivity();


        navButton = view.findViewById(R.id.btn_navigate);
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.startNavigation();

            }
        });
        return view;
    }
}
