package com.flipcam;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;


public class VideoFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    public static final String TAG = "VideoFragment";
    public VideoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment VideoFragment.
     */
    public static VideoFragment newInstance() {
        VideoFragment fragment = new VideoFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        Log.d(TAG,"Inside video fragment");
        fetchMedia();
        return view;
    }

    private void fetchMedia()
    {
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        Log.d(TAG,root.getPath());
        File fc = new File(root.getPath()+"/FlipCam");
        if(!fc.exists()){
            fc.mkdir();
            Log.d(TAG,"FlipCam dir created");
            File videos = new File(fc.getPath()+"/FC_Videos");
            if(!videos.exists()){
                videos.mkdir();
                Log.d(TAG,"Videos dir created");
            }
            File images = new File(fc.getPath()+"/FC_Images");
            if(!images.exists()){
                images.mkdir();
                Log.d(TAG,"Images dir created");
            }
        }
        //Use this for sharing files between apps
        File videos = new File(root.getPath()+"/FlipCam/FC_Videos/");
        if(videos.listFiles() != null){
            Log.d(TAG,"List = "+videos.listFiles());
            Log.d(TAG,"List length = "+videos.listFiles().length);
            for(File file : videos.listFiles()){
                Log.d(TAG,file.getPath());
                //Picasso.with(getContext())
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
