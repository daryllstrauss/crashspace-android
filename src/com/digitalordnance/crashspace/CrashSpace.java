package com.digitalordnance.crashspace;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import android.widget.EditText;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.uploader.UploadMetaData;
import com.aetrion.flickr.uploader.Uploader;
import com.digitalordnance.SimpleCrypto;

public class CrashSpace extends Activity implements OnTouchListener {
	private static final int TAKE_PICTURE = 1;
	private static final int SELECT_PICTURE = 2;
	private static final int PASSWORD_DIALOG = 1;
	TextView user;
	TextView message;
	SeekBar slider;	
	String baseURL;
	TextView minText;
	WebView status;
	boolean initDone=false;
	float touchDownX;
	protected Uri captureUri;
    MediaScannerConnection ms=null;
    // THESE NEED TO BE CHANGED !!!
    final String appKey="APPKEY";
    final String appSecret="APPSECRET";
    final String encodedToken="ENCODED";
    String flickrToken;
    Flickr flickr;
    Dialog passwordDialog;
    String imagePath;
    
    Boolean authFlickr() {
    	EditText passwordField=(EditText)passwordDialog.findViewById(R.id.password_id);

		String password=passwordField.getText().toString();
		try {
			flickrToken=SimpleCrypto.decrypt(password, encodedToken);
			flickr=new Flickr(appKey, appSecret, new REST());
			Auth auth=flickr.getAuthInterface().checkToken(flickrToken);
			if (auth.getPermission()!=Permission.WRITE) {
				Toast toast=Toast.makeText(this, "Invalid password", Toast.LENGTH_LONG);
				toast.show();
				return false;
			}
		} catch (Exception e) { 
			Log.v("CrashSpace", "flickr auth exception", e);
			return false; 
		}
		SharedPreferences prefs = getSharedPreferences("CrashSpace", 0);
    	SharedPreferences.Editor edit = prefs.edit();
    	edit.putString("flickrToken", flickrToken);
    	edit.commit();
    	return true;
    }
    
    protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	switch (id) {
    	case PASSWORD_DIALOG:
    		LayoutInflater inflater=(LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
    		View layout = inflater.inflate(R.layout.password_dialog,
    		                               (ViewGroup)findViewById(R.id.password_dialog_id));
    		builder = new AlertDialog.Builder(this);
    		builder.setView(layout);
    		builder.setPositiveButton("Login", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int id) {
    				authFlickr();
    				uploadToFlickr();
    			}
    		});
    		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int id) {
    			}
    		});
    		passwordDialog=builder.create();
    		return passwordDialog;
    	}
    	return null;
    }
    
	final View.OnClickListener buttonClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			String url;
			
			url=baseURL+"?";
			url+="id="+URLEncoder.encode(user.getText().toString());
			url+="&type=AndroidApp";
			url+="&diff_mins_max="+Integer.toString(slider.getProgress());
			url+="&msg="+URLEncoder.encode(message.getText().toString());
            status.loadUrl(url);
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
	};
	
	void uploadToFlickr() {
		FileInputStream pic;
		String token;
		
		Log.v("CrashSpace", "Upload to flickr");
		SharedPreferences prefs = getSharedPreferences("CrashSpace", 0);
		if (!prefs.contains("flickrToken")) {
			showDialog(PASSWORD_DIALOG);
			return;
		}
		Log.v("CrashSpace", "Good token. path:"+imagePath);
		try {
			pic=new FileInputStream(imagePath);
		} catch (Exception e) {
			Toast.makeText(this, "Failed to open file", Toast.LENGTH_LONG).show();
			return;
		}
		EditText desc=(EditText)findViewById(R.id.CrashCamMessage_id);
		Log.v("CrashSpace", "Attempting upload");
		try {
			RequestContext rc=RequestContext.getRequestContext();
			flickr=new Flickr(appKey, appSecret, new REST());
			token=prefs.getString("flickrToken", "");
			Auth auth=new Auth();
			auth.setToken(token);
			auth.setPermission(Permission.WRITE);
			rc.setAuth(auth);
			UploadMetaData md=new UploadMetaData();
			md.setTitle(desc.getText().toString());
			md.setDescription("Effortless uploaded by Android CrashSpace App");
			md.setPublicFlag(true);
			Uploader upload=flickr.getUploader();
			upload.upload(pic, md);
		} catch (Exception e) {
			Log.v("CrashSpace", "Upload failed", e);
			return; 
		}
		Toast.makeText(this, "Image uploaded", Toast.LENGTH_SHORT).show();
		Log.v("CrashSpace", "Done");

		ViewAnimator vf=(ViewAnimator)findViewById(R.id.flipper_id);
		vf.setInAnimation(AnimationUtils.loadAnimation(vf.getContext(), R.anim.slide_left_in));
		vf.setOutAnimation(AnimationUtils.loadAnimation(vf.getContext(), R.anim.slide_left_out));
		vf.setDisplayedChild(0);
	}
	
	final View.OnClickListener flickrClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			uploadToFlickr();
		}
	};
	
	final SeekBar.OnSeekBarChangeListener sliderChanged = new SeekBar.OnSeekBarChangeListener() {
		@Override public void onStopTrackingTouch(SeekBar seekBar) {}
		
		@Override public void onStartTrackingTouch(SeekBar seekBar) {}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (fromUser) {
				minText.setText(Integer.toString(progress));
			}
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);
        
        final Button button = (Button) findViewById(R.id.button_id);
        status = (WebView) findViewById(R.id.status_id);
        user = (TextView) findViewById(R.id.user_id);
        message = (TextView) findViewById(R.id.message_id);
        slider = (SeekBar) findViewById(R.id.minute_id);
        baseURL=this.getString(R.string.BaseURL);
        minText = (TextView) findViewById(R.id.minute_text_id);
        final Button flickr = (Button) findViewById(R.id.flickrButton_id);

        status.getSettings().setBuiltInZoomControls(true);
        status.getSettings().setSupportZoom(true);
        status.setInitialScale(1);
        status.loadUrl(baseURL);

        button.setOnClickListener(buttonClick);
        flickr.setOnClickListener(flickrClick);
        slider.setOnSeekBarChangeListener(sliderChanged);
        SharedPreferences prefs = getSharedPreferences("CrashSpace", 0);
        if (prefs.contains("user")) user.setText(prefs.getString("user", ""));
        Log.v("CrashSpace", "user pref: "+prefs.getString("user", "EMPTY").toString());
        if (prefs.contains("minutes")) slider.setProgress(prefs.getInt("minutes", 60));
        if (prefs.contains("message")) message.setText(prefs.getString("message", "I am here"));
        initDone=true;
        
        ViewAnimator vf=(ViewAnimator)findViewById(R.id.flipper_id);
        vf.setOnTouchListener(this);
        
        if (state!=null) {
        	if (state.containsKey("captureUri")) {
        		captureUri=Uri.parse(state.getString("captureUri"));
        		setImage(captureUri.getPath());
        	}
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
    	if (captureUri!=null) state.putString("captureUri", captureUri.toString());
    }
    
    @Override
    public void onResume() {
    	super.onStart();
    	if (initDone) status.loadUrl(baseURL);
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	SharedPreferences prefs = getSharedPreferences("CrashSpace", 0);
    	SharedPreferences.Editor edit = prefs.edit();
    	edit.putString("user", user.getText().toString());
    	edit.putInt("minutes", slider.getProgress());
    	edit.putString("message", message.getText().toString());
    	edit.commit();
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
/*		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			touchDownX=event.getX();
			break;
		case MotionEvent.ACTION_UP:
			float currentX=event.getX();
			ViewAnimator vf=(ViewAnimator)findViewById(R.id.flipper_id);
			if (touchDownX<currentX) {
				vf.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_right_in));
				vf.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_right_out));
				vf.showPrevious();
			} else if (touchDownX>currentX) {
				vf.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_left_in));
				vf.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_left_out));
				vf.showNext();
			}
		}
*/		return true;
	}
	
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }
    
    protected String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor==null) return null; //Cursor can be null, if you used OI File Manager
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    protected void setImage(String path) {
    	Log.v("CrashSpace", "setImage: "+path);
    	Log.v("CrashSpace", "CaptureURI.toString="+captureUri.toString());
    	Log.v("CrashSpace", "CaptureURI.getPath="+captureUri.getPath());
    	imagePath=path;
//    	Bitmap image=BitmapFactory.decodeFile(path);
    	Bitmap image=BitmapFactory.decodeFile("/sdcard/Pictures/capture.jpg");
    	Log.v("CrashSpace", "Image: "+image);
    	ImageView iv=(ImageView)findViewById(R.id.imageDisplay_id);
    	iv.setImageBitmap(image);
		ViewAnimator vf=(ViewAnimator)findViewById(R.id.flipper_id);
		vf.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_right_in));
		vf.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_right_out));
		vf.setDisplayedChild(1);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.v("CrashSpace", "onActivityResult called requestCode="+requestCode+" resultCode="+resultCode);
    	switch (requestCode) {
    	case SELECT_PICTURE:
	        if (resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();
                //MEDIA GALLERY
                final String selectedImagePath = getPath(selectedImageUri);
                if (selectedImagePath!=null) {
                	setImage(selectedImagePath);
                	return;
                }
                	
                //OI FILE Manager
                final String filemanagerstring = selectedImageUri.getPath();
                if(filemanagerstring!=null) {
                    setImage(filemanagerstring);
                    return;
                }
	        }
	        break;
    	case TAKE_PICTURE:
    		if (resultCode == RESULT_OK) {
    			ContentResolver cr = getContentResolver();
    			cr.notifyChange(captureUri, null);
    			setImage(captureUri.getPath());
    			return;
    		}
    	}
    }

    void takePhoto() {
    	Log.v("CrashSpace", "Take Picture");
    	Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
    	File path = new File(Environment.getExternalStorageDirectory(), "Pictures");
    	path.mkdirs();
    	final File file = new File(path, "capture.jpg");
    	captureUri=Uri.fromFile(file);
    	intent.putExtra(MediaStore.EXTRA_OUTPUT, captureUri);
    	startActivityForResult(intent, TAKE_PICTURE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Log.v("CrashSpace", "ItemSelected: "+Integer.toHexString(item.getItemId()));
        switch (item.getItemId()) {
        case R.id.TakePicture_id:
        	takePhoto();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}