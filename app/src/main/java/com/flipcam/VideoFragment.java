package com.flipcam;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;

import static android.os.Environment.getExternalStoragePublicDirectory;


public class VideoFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    public static final String TAG = "VideoFragment";
    private int MY_PERMISSIONS_WRITE_STORAGE = 0;
    private static final String STORAGE_PERMISSIONS = "android.permission.WRITE_EXTERNAL_STORAGE";
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_WRITE_STORAGE) {
            if (permissions != null && permissions.length > 0) {
                Log.d(TAG, "For storage == " + permissions[0]);
                if (permissions[0].equalsIgnoreCase(STORAGE_PERMISSIONS)) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        fetchMedia();
                    }
                    else{
                        Toast.makeText(getContext(),"FlipCam needs permission to store the video in the Gallery , so that you can play them later." +
                                "If you do not wish to give permission, the files will be stored internally.",Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        Log.d(TAG,"Inside video fragment");
        File file = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        int permission = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Log.d(TAG,"permission == "+permission);
        if(permission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(getActivity(),new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_WRITE_STORAGE);
        }
        else{
            fetchMedia();
        }
        return view;
    }

    private void fetchMedia()
    {
        String[] projection = new String[] {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DISPLAY_NAME
        };
        String[] vid_projection = new String[]{
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DISPLAY_NAME
        };
        File root = Environment.getExternalStorageDirectory();
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
        // content:// style URI for the "primary" external storage volume
        //Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        //This code is necessary when we want to display all media created by FlipCam, when user clicks on a widget.
        Uri videos = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Log.d(TAG,videos+"");

        Cursor cur = getContext().getContentResolver().query(videos,
                vid_projection, // Which columns to return
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME+" = 'FC_Videos'",       // Which rows to return (all rows)
                null,       // Selection arguments (none)
                MediaStore.Images.Media.DATE_TAKEN + " DESC"        // Ordering
        );

        Log.i("ListingImages"," query count=" + cur.getCount());

        if (cur.moveToFirst()) {
            String bucket;
            String date;
            int bucketColumn = cur.getColumnIndex(
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME);

            int dateColumn = cur.getColumnIndex(
                    MediaStore.Video.Media.DATE_TAKEN);

            int id = cur.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);

            do {
                // Get the field values
                bucket = cur.getString(bucketColumn);
                Log.d(TAG,bucket);
                if(bucket.equalsIgnoreCase("FC_Videos")) {
                    date = cur.getString(dateColumn);
                    // Do something with the values.
                    Log.i("ListingImages", " bucket=" + bucket
                            + "  date_taken=" + date + " id = " + cur.getString(id));
                }
            } while (cur.moveToNext());

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
