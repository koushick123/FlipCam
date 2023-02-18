package com.flipcam.usermanual;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.flipcam.R;

public class FCScreen9 extends Fragment {

    public FCScreen9(){
        //required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fc_screen_9,container,false);
        ImageView imageView = (ImageView) view.findViewById(R.id.fcUserManualScreen9);
        Glide.with(getActivity().getApplicationContext())
                .load(R.drawable.fc_screen_9)
                .fitCenter()
                .into(imageView);
        return view;
    }
}
