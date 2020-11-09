package com.okpd.zoom.Utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BitmapUtils {

    public static Bitmap getBitmapFromAssets(Context context,String fileName, int width, int height)
    {
        AssetManager assetManager = context.getAssets();

        InputStream inputStream;
        Bitmap bitmap = null;
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            inputStream = assetManager.open(fileName);
            options.inSampleSize = calculateInSampleSize(options,width,height);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeStream(inputStream,null,options);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static  Bitmap getBitmapFromGallery(Context context, Uri uri, int width, int height)
    {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri,filePathColumn,null,null,null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picturePath,options);
        options.inSampleSize = calculateInSampleSize(options,width,height);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(picturePath,options);

    }
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static String insertImage(ContentResolver cr,
                              Bitmap source,
                              String title,
                              String description) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,title);
        values.put(MediaStore.Images.Media.DISPLAY_NAME,title);
        values.put(MediaStore.Images.Media.DESCRIPTION,description);
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
        values.put(MediaStore.Images.Media.DATE_ADDED,System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DATE_TAKEN,System.currentTimeMillis());

        Uri uri = null;
        String stringUrl = null;
        try{
            uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
            if(source != null)
            {
                OutputStream outputStream = cr.openOutputStream(uri);
                try{
                    source.compress(Bitmap.CompressFormat.JPEG,50,outputStream);
                }finally
                    {
                        outputStream.close();
                    }

                    long id = ContentUris.parseId(uri);
                    Bitmap miniThumb = MediaStore.Images.Thumbnails.getThumbnail(cr,id,MediaStore.Images.Thumbnails.MICRO_KIND,null);
                   // storeThumbnail(cr,miniThumb,id,50f,50f,MediaStore.Images.Thumbnails.MICRO_KIND);

                }
                else
            {
                cr.delete(uri,null,null);
                uri = null;
            }

        } catch (FileNotFoundException e) {
            if(uri != null)
            {
                cr.delete(uri,null,null);
                uri = null;
            }

        }
        if(uri != null)
            stringUrl = uri.toString();
        return stringUrl;
    }

    private static final Bitmap storeThumbnail(ContentResolver cr, Bitmap source, long id, float width, float height, int kind) {

        Matrix matrix = new Matrix();

        float scaleX = width/source.getWidth();
        float scaleY = height/source.getHeight();

        matrix.setScale(scaleX,scaleY);

        Bitmap thumb = Bitmap.createBitmap(source,0,0,source.getWidth(),source.getHeight(),matrix,true);

        ContentValues contentValues = new ContentValues(4);
        contentValues.put(MediaStore.Images.Thumbnails.KIND,kind);
        contentValues.put(MediaStore.Images.Thumbnails.IMAGE_ID,id);
        contentValues.put(MediaStore.Images.Thumbnails.HEIGHT,height);
        contentValues.put(MediaStore.Images.Thumbnails.WIDTH,width);

        Uri uri = cr.insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,contentValues);
        try{
            OutputStream outputStream = cr.openOutputStream(uri);
            thumb.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
            outputStream.close();
            return  thumb;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
