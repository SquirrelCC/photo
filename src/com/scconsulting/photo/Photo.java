package com.scconsulting.photo;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Photo extends Activity {
	
	/*
	 * This Activity uses MediaStore.ACTION_IMAGE_CAPTURE Intent to take a picture.
	 * This Intent will open the Camera app, if installed.
	 * A file name is passed in the Intent so that the Android Camera app
	 *   will store the image as a .jpg file for us, at the location that we specify.
	 *   
	 * The Camera app will return to the onActivityResult() method in this Activity
	 *   after the picture is taken.
	 *   
	 * If your app requires that a camera be on the device for the app to be functional,
	 *   add this line to your AndroidManifest file:
	 *   <uses-feature android:name="android.hardware.camera" android:required="true" />
	 *   
	 *   A camera IS REQUIRED for this Photo app, because its main function is to take a picture.
	 *   
	 * You may also need to add this Permission to your AndroidManifest file:
	 *   <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
	 */
	
	private static final int REQUEST_TAKE_PHOTO = 1;
	private static final String KEY_PHOTO_PATH = "photoPath";
	private ImageView imageView;
	private String photoName = null;
	private File photoFile = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Check if anything in savedInstanceState Bundle.
		// If so, Activity is being recreated after screen orientation change,
		//   so get saved information from Bundle.
		if (savedInstanceState != null) {
		    photoName = savedInstanceState.getString(KEY_PHOTO_PATH);
		}
		
		setContentView(R.layout.photo);
		
		// Get a reference to the ImageView in the layout
		imageView = (ImageView)findViewById(R.id.imageView1);
		
		// Check if onCreate() has fired as the result of a screen orientation change.
		// If so, use the photo file name from savedInstanceState to load the photo
		//    into the ImageView. 
		if (photoName != null) {
			//Log.i("Photo", "orientation change");
			loadPhoto();
		}
	}
	
	public void takePic(View v) {
		
		Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		
		// Check to make sure there is an installed app that can handle this Intent.
		// If not, this app would crash when Intent is sent.
		if (intent.resolveActivity(getPackageManager()) != null) {

			// Create the File where the photo should go
	        photoFile = null;
            photoFile = getFileName();
	        
	        // Continue only if the File was successfully created.
	        // Add the file name to the Intent, using Intent.putExtra().
	        // MediaStore will use our file name to store the image.
            // After the picture is taken, MediaStore will return to this Activity
            //    in the onActivityResult() method.
	        if (photoFile != null) {
	            intent.putExtra(MediaStore.EXTRA_OUTPUT,
	                    Uri.fromFile(photoFile));
	            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
	        }
	    }
		else {
			// Log an error message
			Log.e("Photo", "No app installed to handle taking a photo");
		}
	}
	
	private File getFileName() {
		
		// Create a folder to store photos
		String folderName = this.getString(R.string.app_name);
		File appFolder = new File(Environment.getExternalStorageDirectory(), folderName);
		// Create appFolder, if it does nor already exist
		appFolder.mkdirs();
		
	    // Create an image file name.
		// Use time stamp as part of the file name so that each new file name is unique.
		// (In your own app, use whatever file naming you choose.)
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = "JPEG_" + timeStamp + ".jpg";
	    
	    photoFile = new File(appFolder, imageFileName);
	    photoName = photoFile.getAbsolutePath();
	    
	    return photoFile;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		// Check resultCode
		if (resultCode == RESULT_OK) {
			
			// Instead of using "data", read in the file that was stored by MediaStore.
			// "data" may contain a thumbnail image.
			// Pass our own saved file name to BitmapFactory to read the file.
			loadPhoto();
		    
		}
		else {
			Log.e("Photo", "Problem taking photo");
		}
	}
	
	private void loadPhoto() {
		
		// First decode with inJustDecodeBounds=true to check dimensions
	    BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(photoName, options);
	    
	    Display display = getWindowManager().getDefaultDisplay();
	    DisplayMetrics metrics = new DisplayMetrics();
	    display.getMetrics(metrics);
	    int screenWidth = metrics.widthPixels;
	    int screenHeight = metrics.heightPixels;
	    
	    int width = 0;
	    int height = 0;
	    
	    if (screenWidth > screenHeight) {
			width = (int) (screenWidth * 0.5);
			height = (int) (width * 0.667);
		}
		else {
			height = (int) (screenHeight * 0.5);
			width = (int) (height / 0.667);
		}

	    //Log.i("Photo", "outHeight: "+options.outHeight);
	    //Log.i("Photo", "outWidth: "+options.outWidth);
	    int factor = Math.max(options.outHeight/height, options.outWidth/width);
	    
	    //Log.i("Photo", "Factor: "+factor);
	    options.inSampleSize = factor;
	    
	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    Bitmap bitmap = BitmapFactory.decodeFile(photoName, options);
	    imageView.setImageBitmap(bitmap);
	    
	}
	

	/********************************************************************************
	 * Below are methods to add a GeoLocation tag to the image file.
	 * 
	 * Location information is obtained form the LocationManager.
	 * The latitude and longitude of the location are encoded into the Exif standard:
	 *    Each is expressed as a series of three values giving the degrees, minutes, and seconds, respectively.
	 */
	public void geoTag(View v) {
		
		// Get location information
	    Location location = getLocation();
	    
	    TextView textLat = (TextView) findViewById( R.id.textView1);
	    TextView textLong = (TextView) findViewById( R.id.textView2);
	    
    	if (location == null) {
    		textLat.setText( 0 );
    		textLong.setText( 0 );
    	}
    	else {
		    // Show latitude on the screen
	    	textLat.setText( Double.toString(location.getLatitude()) );
	    	
	    	// Show longitude on the screen
	    	textLong.setText( Double.toString(location.getLongitude()) );
    
		    if (photoName != null) {
		    	// Encode and store the location in the Exif information of the photo file.
			    loc2Exif(photoName, location);
			    Toast.makeText(Photo.this, "Exif information stored", Toast.LENGTH_SHORT).show();
		    }
		}
	}
	
	/**
	* Read location information from image.
	* @param imagePath : image absolute path
	* @return : location information
	*/
	public void readGeoTag(View v)
	{
		if (photoName != null) {
			Location location = new Location("");
			try {
				ExifInterface exif = new ExifInterface(photoName);
				float [] latlong = new float[2];
				if(exif.getLatLong(latlong)){
					location.setLatitude(latlong[0]);
					location.setLongitude(latlong[1]);
				}
				String date = exif.getAttribute(ExifInterface.TAG_DATETIME);
				SimpleDateFormat fmt_Exif = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
				location.setTime(fmt_Exif.parse(date).getTime());
				
				// Show latitude on the screen
		    	TextView textLat = (TextView) findViewById( R.id.textView1);
		    	textLat.setText( Double.toString(location.getLatitude()) );
		    	
		    	// Show longitude on the screen
		    	TextView textLong = (TextView) findViewById( R.id.textView2);
		    	textLong.setText( Double.toString(location.getLongitude()) );
	
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Location getLocation() {
		
		LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE); 
		
		Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location == null) {
            location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		if (location == null) {
            location = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
		}
		return location;
	}
	
	// Convert location to Exif format,
	//    then save the data tag into the stored JPG image file.
	public void loc2Exif(String fileName, Location location) {
		try {
			ExifInterface exif = new ExifInterface(fileName);
			if (location == null) {
				exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, dec2Tag(location.getLatitude()));
				exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,dec2Tag(location.getLongitude()));
				
				exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
				exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
			}
			else {
				exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, dec2Tag(location.getLatitude()));
				exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,dec2Tag(location.getLongitude()));
			
				if (location.getLatitude() > 0d) 
					exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N"); 
				else              
					exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
				
				if (location.getLongitude() > 0d) 
					exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");    
				else             
					exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
			}
			
			SimpleDateFormat fmt_Exif = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
			exif.setAttribute(ExifInterface.TAG_DATETIME,fmt_Exif.format(new Date(location.getTime())));
			
			// Save the tag data into the JPEG file.
			exif.saveAttributes();
			
		} catch (IOException e) {
			Log.e("Photo", "IO Exception: " + e.getMessage());
		}         
	}
	
	// Format latitude or longitude as a String of degrees, minutes, seconds
	private String dec2Tag(double coordinate) {
		
		// Get absolute value of the coordinate (if negative, make it positive).
		coordinate = Math.abs(coordinate);  // -105.9876543 -> 105.9876543
		
		// Place degrees into String.
		String stringCoord = Integer.toString((int)coordinate) + "/1,";  // 105/1,
		
		// Place minutes into String.
		coordinate = (coordinate % 1) * 60;  // .987654321 * 60 = 59.259258
		stringCoord = stringCoord + Integer.toString((int)coordinate) + "/1,";  // 105/1,59/1,
		
		// Place seconds into String.
		coordinate = (coordinate % 1) * 60000;  // .259258 * 60000 = 15555
		stringCoord = stringCoord + Integer.toString((int)coordinate) + "/1000";  // 105/1,59/1,15555/1000
		
		return stringCoord;
	}
	
	//********************************************************************************
	
	// The layout is done loading and 
	//   is visible on the screen in the onWindowFocusChanged() method.
	// We can get valid view sizes here, and set a different view size.
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if (hasFocus) {
			
			int screenWidth = ((RelativeLayout)findViewById(R.id.rl_main)).getWidth();
			int screenHeight = ((RelativeLayout)findViewById(R.id.rl_main)).getHeight();
			int width = 0;
			int height = 0;
			
			if (screenWidth > screenHeight) {
				width = (int) (screenWidth * 0.5);
				height = (int) (width * 0.667);
			}
			else {
				height = (int) (screenHeight * 0.5);
				width = (int) (height / 0.667);
			}

			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)imageView.getLayoutParams();
			params.height = height;
			params.width = width;
			imageView.setLayoutParams(params);

		}
	}
	
	// Save information in Bundle in case screen orientation changes when picture is taken.
	// Orientation change causes Activity to be recreated, and any variables lost unless saved.
	@Override
	protected void onSaveInstanceState (Bundle saveState) {
	    super.onSaveInstanceState(saveState);
	    // Save (key, value) pair for photo path. 
	    saveState.putString(KEY_PHOTO_PATH, photoName);
	}
}
