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

public class FCPhotoScreen extends Fragment {

    public FCPhotoScreen(){
        //required empty public constructor.
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fc_photo_screen,container,false);
        ImageView imageView = (ImageView) view.findViewById(R.id.fcUserManualScreen5);
        Glide.with(getActivity().getApplicationContext())
                .load(R.drawable.fc_photo_screen)
                .fitCenter()
                .into(imageView);
        return view;
    }
}
