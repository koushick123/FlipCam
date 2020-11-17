package com.flipcam.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by koushick on 24-Mar-18.
 */

public class MediaProvider extends ContentProvider {

    public static final String TAG = "MediaProvider";
    private static MediaDBHelper mediaDBHelper;
    private static SQLiteDatabase writeMediaDatabase;
//    private int MEDIA_ID = 1000;
    static boolean VERBOSE = false;

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    private static final int INSERT_MEDIA = 300;
    private static final int DELETE_MEDIA = 400;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static{
        sUriMatcher.addURI(MediaTableConstants.CONTENT_AUTHORITY,"/addMedia",INSERT_MEDIA);
        sUriMatcher.addURI(MediaTableConstants.CONTENT_AUTHORITY,"/deleteMedia",DELETE_MEDIA);
    }

    @Override
    public boolean onCreate() {
        getMediaBHelper();
        return true;
    }

    public void getMediaBHelper(){
        if(mediaDBHelper == null) {
            mediaDBHelper = new MediaDBHelper(getContext());
        }
    }

    public static SQLiteDatabase getWriteMediaDatabase(){
        if(writeMediaDatabase == null){
            String myPath = MediaDBHelper.DB_PATH + MediaDBHelper.DATABASE_NAME;
            try {
                writeMediaDatabase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
                if (writeMediaDatabase != null) {
                    return writeMediaDatabase;
                }
            }
            catch(SQLiteCantOpenDatabaseException sqlEx)
            {
                writeMediaDatabase = mediaDBHelper.getWritableDatabase();
            }
            if(VERBOSE)Log.d(TAG,"DB Path == "+writeMediaDatabase.getPath());
        }
        return writeMediaDatabase;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] selection) {
        int match = sUriMatcher.match(uri);
        switch (match){
            case DELETE_MEDIA:
                if(VERBOSE)Log.d(TAG,"deleting Media = "+selection[0]);
                try {
                    getWriteMediaDatabase().beginTransaction();
                    String deleteMed = "DELETE FROM MEDIA WHERE FILE_NAME = ?";
                    SQLiteStatement sqLiteStatement = getWriteMediaDatabase().compileStatement(deleteMed);
                    sqLiteStatement.bindString(1, selection[0]);
                    if(sqLiteStatement.executeUpdateDelete() != 0){
                        if(VERBOSE)Log.d(TAG, "DELETED = "+selection[0]);
                    }
                    else{
                        if(VERBOSE)Log.d(TAG, "No Data deleted");
                    }
                }
                finally {
                    getWriteMediaDatabase().endTransaction();
                }
                break;
        }
        return 0;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        String getMedia;
        int match = sUriMatcher.match(uri);
        switch (match)
        {
            case INSERT_MEDIA:
                if(VERBOSE)Log.d(TAG,"inserting Media");
                getMedia = SQLiteQueryBuilder.buildQueryString(false,MediaTableConstants.MEDIA_TABLE + " media1 ",null,null,null,null,MediaTableConstants.ID,null );
                Cursor media_exist = getWriteMediaDatabase().rawQuery(getMedia, null);
                try {
                    boolean error = false;
                    getWriteMediaDatabase().beginTransaction();
                    String insertMed = "INSERT INTO " + MediaTableConstants.MEDIA_TABLE + " ( " + MediaTableConstants.ID + " , " +
                            MediaTableConstants.FILE_NAME + " , " + MediaTableConstants.MEMORY_STORAGE +
                            " ) VALUES (?,?,?)";
                    SQLiteStatement sqLiteStatement = getWriteMediaDatabase().compileStatement(insertMed);
                    sqLiteStatement.bindString(2, contentValues.getAsString("filename"));
                    sqLiteStatement.bindLong(3, contentValues.getAsLong("memoryStorage"));
                    if (sqLiteStatement.executeInsert() == -1) {
                        error = true;
                        if(VERBOSE)Log.d(TAG, "not inserted...some error");
                    } else {
                        if(VERBOSE)Log.d(TAG, "inserted!!== > " + contentValues.getAsString("filename"));
                    }
                    sqLiteStatement.close();
                    if(!error){
                        getWriteMediaDatabase().setTransactionSuccessful();
                        getContext().getContentResolver().notifyChange(uri, null);
                    }
                }
                finally {
                    getWriteMediaDatabase().endTransaction();
                    media_exist.close();
                }
                break;
        }
        return uri;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        return super.bulkInsert(uri, values);
    }
}
