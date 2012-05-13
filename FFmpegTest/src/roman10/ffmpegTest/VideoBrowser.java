package roman10.ffmpegTest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import roman10.iconifiedtextselectedlist.IconifiedTextSelected;
import roman10.iconifiedtextselectedlist.IconifiedTextSelectedView;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;

public class VideoBrowser extends ListActivity implements ListView.OnScrollListener {

	/*this part communicates with native code through jni (java native interface)*/
	//load the native library
	static {
    	System.loadLibrary("ffmpeg");
    	System.loadLibrary("ffmpeg-test-jni");
    }
	//declare the jni functions
	private static native void naInit(String _videoFileName);
	private static native int[] naGetVideoResolution();
	private static native String naGetVideoCodecName();
	private static native String naGetVideoFormatName();
	private static native void naClose();
    private static final String DIRECTORY = "/mnt/sdcard/satish/";
	
	private void showVideoInfo(final File _file) 
	{
		String videoFilename = _file.getAbsolutePath();
		naInit(videoFilename);
		int[] prVideoRes = naGetVideoResolution();
		String prVideoCodecName = naGetVideoCodecName();
		String prVideoFormatName = naGetVideoFormatName();
		naClose();
		String displayText = "Video: " + videoFilename + "\n";
		displayText += "Video Resolution: " + prVideoRes[0] + "x" + prVideoRes[1] + "\n";
		displayText += "Video Codec: " + prVideoCodecName + "\n";
		displayText += "Video Format: " + prVideoFormatName + "\n";
		text_titlebar_text.setText(displayText);
	}
	
	
	/*the rest of the file deals with UI and other stuff*/
	private Context mContext;
	public static VideoBrowser self;
	
	/**
	 * activity life cycle: this part of the source code deals with activity life cycle
	 */
	@Override
	public void onCreate(Bundle icicle) 
	{
		super.onCreate(icicle);
		mContext = this.getApplicationContext();
		Log.d("satish", "Oncreate Entered!");
		self = this;
		initUI();
	}
	
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		unbindDisplayEntries();
	}
	
	public void unbindDisplayEntries() {
		if (displayEntries!=null) {
			int l_count = displayEntries.size();
			for (int i = 0; i < l_count; ++i) {
				IconifiedTextSelected l_its = displayEntries.get(i);
				if (l_its != null) {
					Drawable l_dr = l_its.getIcon();
					if (l_dr != null) {
						l_dr.setCallback(null);
						l_dr = null;
					}
				}
			}
		}
		if (l_displayEntries!=null) {
			int l_count = l_displayEntries.size();
			for (int i = 0; i < l_count; ++i) {
				IconifiedTextSelected l_its = l_displayEntries.get(i);
				if (l_its != null) {
					Drawable l_dr = l_its.getIcon();
					if (l_dr != null) {
						l_dr.setCallback(null);
						l_dr = null;
					}
				}
			}
		}
	}

	/**
	 * Data: this part of the code deals with data processing
	 */
	public List<IconifiedTextSelected> displayEntries = new ArrayList<IconifiedTextSelected>();
	public static List<IconifiedTextSelected> l_displayEntries = new ArrayList<IconifiedTextSelected>();;
	
	/**load images
	 * this part of code deals with loading of images
	 */
	private File currentDirectory;
	public int media_browser_load_option = 2;
	private static int last_media_browser_load_option = 2;
	private static int number_of_icons = 0;
	private static final String upOneLevel = "..";
	
	LoadVideoTask loadTask;
	private void loadVideosFromDirectory(String _dir) 
	{
		try 
		{
			loadTask = new LoadVideoTask();
			loadTask.execute(_dir);
		} 
		catch (Exception e) 
		{
			Toast.makeText(this, "Load media fail!", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void getVideosFromDirectoryNonRecurAddParent(File _dir) 
	{
		//add the upper one level data
		if (_dir.getParent()!=null) 
		{
			this.displayEntries.add(new IconifiedTextSelected(
					upOneLevel,
					getResources().getDrawable(R.drawable.folderback), 
					false, false, 0));
		}
	}
	
	private void getVideosFromDirectoryNonRecur(File _dir)
	{
		Drawable folderIcon = this.getResources().getDrawable(R.drawable.normalfolder);
		//add the 
		if (!_dir.isDirectory()) 
		{
			return;
		}
		
		File[] files = _dir.listFiles();
		
		if (files == null) 
		{
			return;
		}
		
		Drawable videoIcon = null;
		int l_iconType = 0;
		for (File currentFile : files) {
			if (currentFile.isDirectory()) {
				//if it's a directory
				this.displayEntries.add(new IconifiedTextSelected(
						currentFile.getPath(),
						folderIcon, false, false, 0));
			} else {
				String l_filename = currentFile.getName();
				if (checkEndsWithInStringArray(l_filename,getResources().getStringArray(R.array.fileEndingVideo))) 
				{
					if (number_of_icons < 10) {
						videoIcon = null;
						++number_of_icons;
						l_iconType = 22;
					} else {
						videoIcon = null;
						l_iconType = 2;
					}
					this.displayEntries.add(new IconifiedTextSelected(
							currentFile.getPath(),
							videoIcon, false, false, l_iconType));
				}
			}
		}
	}
		
	private void getVideosFromDirectoryRecur(File _dir) {
		Drawable videoIcon = null;
		File[] files = _dir.listFiles();
		int l_iconType = 2;
		if (files == null) {
			return;
		}
		for (File currentFile : files) {
			if (currentFile.isDirectory()) {
				getVideosFromDirectoryRecur(currentFile);
				continue;
			} else {
				String l_filename = currentFile.getName();
				//if it's an image file
				if (checkEndsWithInStringArray(l_filename,
						getResources().getStringArray(R.array.fileEndingVideo))) {
					if (number_of_icons < 10) {
						videoIcon = null;
						++number_of_icons;
						l_iconType = 22;
					} else {
						videoIcon = null;
						l_iconType = 2;
					}
					this.displayEntries.add(new IconifiedTextSelected(
							currentFile.getPath(),
							videoIcon, false, false, l_iconType));
				}
			}
		}
	}
	
	private void getVideosFromGallery() 
	{
		Drawable videoIcon = null;
		Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
		String[] projection = {MediaStore.Video.Media.DATA};
		Cursor l_cursor = this.managedQuery(uri, projection, null, null, null);
		int videoNameColumnIndex;
		String videoFilename;
		File videoFile;
		int l_iconType = 2;
		if (l_cursor!=null) {
			if (l_cursor.moveToFirst()) {
				do {
					videoNameColumnIndex = l_cursor.getColumnIndexOrThrow(
							MediaStore.Images.Media.DATA);
					videoFilename = l_cursor.getString(videoNameColumnIndex);
					videoFile = new File(videoFilename);
					if (!videoFile.exists()) {
						continue;
					}
					if (number_of_icons <= 10) {
						videoIcon = null;
						++number_of_icons;
						l_iconType = 22;
					} else {
						videoIcon = null;
						l_iconType = 2;
					}
					this.displayEntries.add(new IconifiedTextSelected(
							videoFile.getAbsolutePath(),
							videoIcon, false, false, l_iconType));
				} while (l_cursor.moveToNext());
			}
		}
		if (l_cursor!=null) {
			l_cursor.close();
		}
	}
	
	private boolean checkEndsWithInStringArray(String checkItsEnd, 
			String[] fileEndings){
		for(String aEnd : fileEndings){
			if(checkItsEnd.endsWith(aEnd))
				return true;
		}
		return false;
	}
	
	private class LoadVideoTask extends AsyncTask<String, Void, Void> {
		@Override
		protected void onPreExecute() {
			System.gc();
			displayEntries.clear();
			showDialog(DIALOG_LOAD_MEDIA);
		}
		@Override
		protected Void doInBackground(String... params) {
			File l_root = new File(params[0]);
			if (l_root.isDirectory()) {
				number_of_icons = 0;
				currentDirectory = l_root;
				if (media_browser_load_option == 0) 
				{
					//list all videos in the root directory without going into sub folder
					getVideosFromDirectoryNonRecurAddParent(l_root);
					getVideosFromDirectoryNonRecur(l_root);
				} 
				else if (media_browser_load_option == 1) 
				{
					//list all videos in the root folder recursively
					getVideosFromDirectoryRecur(l_root);
				}
				else if (media_browser_load_option == 2) 
				{
					//list all videos in the gallery
					getVideosFromGallery();
				}
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void n) 
		{
			refreshUI();
			dismissDialog(DIALOG_LOAD_MEDIA);
		}
	}
	
	/**
	 * UI: this part of the source code deals with UI
	 */
	//bottom menu
	private int currentFocusedBtn = 1;
	private Button btn_bottommenu1;
	private Button btn_bottommenu2;
	private Button btn_bottommenu3;
	//private Button btn_bottommenu4;
	//title bar
	private TextView text_titlebar_text;
	
	private void initUI() 
	{
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.video_browser);
		//title bar
		text_titlebar_text = (TextView) findViewById(R.id.titlebar_text);
		text_titlebar_text.setText("Click a video to display info");
		
		//bottom menu
		int l_btnWidth = this.getWindowManager().getDefaultDisplay().getWidth()/4;
		btn_bottommenu1 = (Button) findViewById(R.id.video_browser_btn1);
		//btn_bottommenu1 = (ActionMenuButton) findViewById(R.id.main_topsecretimport_btn1);
		btn_bottommenu1.setWidth(l_btnWidth);
		btn_bottommenu1.setOnClickListener(new View.OnClickListener() 
		{
			public void onClick(View v) 
			{
				btn_bottommenu1.setEnabled(false);
				btn_bottommenu2.setEnabled(true);
				btn_bottommenu3.setEnabled(true); 
				currentFocusedBtn = 1;
				last_list_view_pos = 0;
				media_browser_load_option = 2;
				last_media_browser_load_option = media_browser_load_option;
				loadVideosFromDirectory(DIRECTORY);
				/*
				File dirx = new File(DIRECTORY);
				if (!dirx.canRead()) {
				    throw new RuntimeException("button1");
				}
		        getVideosFromDirectoryNonRecurAddParent(dirx);
                getVideosFromDirectoryNonRecur(dirx);
                */
                Log.d("satish","btn1");
			}
		});
		
		btn_bottommenu2 = (Button) findViewById(R.id.video_browser_btn2);
		btn_bottommenu2.setWidth(l_btnWidth);
		btn_bottommenu2.setOnClickListener(new View.OnClickListener() 
		{
			public void onClick(View v) 
			{
				btn_bottommenu1.setEnabled(true);
				btn_bottommenu2.setEnabled(false);
				btn_bottommenu3.setEnabled(true);
				currentFocusedBtn = 2;
				last_list_view_pos = 0;
				media_browser_load_option = 0;
				last_media_browser_load_option = media_browser_load_option;
				loadVideosFromDirectory(DIRECTORY);
				Log.d("satish","btn2");
			}
		});
		btn_bottommenu3 = (Button) findViewById(R.id.video_browser_btn3);
		btn_bottommenu3.setWidth(l_btnWidth);
		btn_bottommenu3.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				btn_bottommenu1.setEnabled(true);
				btn_bottommenu2.setEnabled(true);
				btn_bottommenu3.setEnabled(false);
				currentFocusedBtn = 3;
				last_list_view_pos = 0;
				media_browser_load_option = 1;
				last_media_browser_load_option = media_browser_load_option;
				loadVideosFromDirectory(DIRECTORY);
                Log.d("satish","btn3");
			}
		});
		media_browser_load_option = last_media_browser_load_option;
		if (media_browser_load_option==2) {
			btn_bottommenu1.setEnabled(false);
		} else if (media_browser_load_option==0) {
			btn_bottommenu2.setEnabled(false);
		} else if (media_browser_load_option==1){
			btn_bottommenu3.setEnabled(false);
		}
		loadVideosFromDirectory(DIRECTORY);
	}
	//refresh the UI when the directoryEntries changes
	private static int last_list_view_pos = 0;
	public void refreshUI() {
		int l_btnWidth = this.getWindowManager().getDefaultDisplay().getWidth()/4;
		btn_bottommenu1.setWidth(l_btnWidth);
		btn_bottommenu2.setWidth(l_btnWidth);
		btn_bottommenu3.setWidth(l_btnWidth);
		//btn_bottommenu4.setWidth(l_btnWidth);
		
		SlowAdapter itla = new SlowAdapter(this);
		itla.setListItems(this.displayEntries);		
		this.setListAdapter(itla);
        getListView().setOnScrollListener(this);
        int l_size = this.displayEntries.size();
        if (l_size > 50) {
        	getListView().setFastScrollEnabled(true);
        } else {
        	getListView().setFastScrollEnabled(false);
        }
        if (l_size > 0) {
	        if (last_list_view_pos < l_size) {
				getListView().setSelection(last_list_view_pos);
			} else {
				getListView().setSelection(l_size-1);
			}
        }
		registerForContextMenu(getListView());
	}
	
	@Override
    public void onConfigurationChanged (Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	refreshUI();
    }
	
	static final int DIALOG_LOAD_MEDIA = 1;
	static final int DIALOG_HELP = 2;
	@Override
	protected Dialog onCreateDialog(int id) {
        switch(id) {
        case DIALOG_LOAD_MEDIA:
        	ProgressDialog dialog = new ProgressDialog(this);
	        dialog.setTitle("Load Files");
	        dialog.setMessage("Please wait while loading...");
	        dialog.setIndeterminate(true);
	        dialog.setCancelable(true);
        	return dialog;
        default:
        	return null;
        }
	}
	/**
	 * scroll events methods: this part of the source code contain the control source code
	 * for handling scroll events
	 */
	private boolean mBusy = false;
	private void disableButtons() {
		btn_bottommenu1.setEnabled(false);
		btn_bottommenu2.setEnabled(false);
		btn_bottommenu3.setEnabled(false);
	}
	
	private void enableButtons() {
		if (currentFocusedBtn!=1) {
			btn_bottommenu1.setEnabled(true);
		}
		if (currentFocusedBtn!=2) {
			btn_bottommenu2.setEnabled(true);
		}
		if (currentFocusedBtn!=3) {
			btn_bottommenu3.setEnabled(true);
		}
	}
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		last_list_view_pos = view.getFirstVisiblePosition();
	}

	//private boolean mSaveMemory = false;
	public void onScrollStateChanged(AbsListView view, int scrollState) {		
		switch (scrollState) {
        case OnScrollListener.SCROLL_STATE_IDLE:
        	enableButtons();
            mBusy = false;
            int first = view.getFirstVisiblePosition();
            int count = view.getChildCount();
            int l_releaseTarget;
            for (int i=0; i<count; i++) {
            	IconifiedTextSelectedView t = (IconifiedTextSelectedView)view.getChildAt(i);
            	if (t.getTag()!=null) {
            		String filename = this.displayEntries.get(first+i).getText();
            		t.setText(filename);
            		int l_type = this.displayEntries.get(first+i).getType();
            		Drawable l_icon = null;
            		try {
	            		if (l_type == 1) {
	            			l_icon = null;
	            		} else if (l_type == 2) {
	            			l_icon = null;
	            		}
            		} catch (OutOfMemoryError e) {
            			//if outofmemory, we try to clean up 10 view image resources,
            			//and try again
            			for (int j = 0; j < 10; ++j) {
            				l_releaseTarget = first - count - j;
	    	            	if (l_releaseTarget > 0) {
	    	    				IconifiedTextSelected l_its = displayEntries.get(l_releaseTarget);
	    	    				IconifiedTextSelectedView l_itsv = (IconifiedTextSelectedView)
	    	    					this.getListView().getChildAt(l_releaseTarget);
	    	    				if (l_itsv!=null) {
	    	    					l_itsv.setIcon(null);
	    	    				}
	    	    				if (l_its != null) {
	    	    					Drawable l_dr = l_its.getIcon();
	    	    					l_its.setIcon(null);
	    	    					if (l_dr != null) {
	    	    						l_dr.setCallback(null);
	    	    						l_dr = null;
	    	    					}
	    	    				}
	    	            	}
            			}
            			System.gc();
            			//after clean up, we try again
            			if (l_type == 1) {
	            			l_icon = null;
	            		} else if (l_type == 2) {
	            			l_icon = null;
	            		}
            		}
            		this.displayEntries.get(first+i).setIcon(l_icon);
            		if (l_icon != null) 
            		{
            			t.setIcon(l_icon);
            			t.setTag(null);
            		}
            	}
            }
            //System.gc();
            break;
        case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
        	disableButtons();
            mBusy = true;
            break;
        case OnScrollListener.SCROLL_STATE_FLING:
        	disableButtons();
            mBusy = true;
            break;
        }
	}
	
	/**
	 * List item click action
	 */
	private File currentFile;
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		last_list_view_pos = position;
		String selectedFileOrDirName = this.displayEntries.get((int)id).getText();
		if (selectedFileOrDirName.equals(upOneLevel)) {
			if (this.currentDirectory.getParent()!=null) {
				last_list_view_pos = 0;
				browseTo(this.currentDirectory.getParentFile());
			}
		} else {
			File l_clickedFile = new File(this.displayEntries.get((int)id).getText());
			if (l_clickedFile != null) {
				if (l_clickedFile.isDirectory()) {
					last_list_view_pos = 0;
					browseTo(l_clickedFile);
				} else {
					showVideoInfo(l_clickedFile);
				}
			}
		}
	}
	
	private void browseTo(final File _dir) {
		if (_dir.isDirectory()) {
			this.currentDirectory = _dir;
			loadVideosFromDirectory(_dir.getAbsolutePath());
		} 
	}
	
	/**
	 * Slow adapter: this part of the code implements the list adapter
	 * Will not bind views while the list is scrolling
	 */
	private class SlowAdapter extends BaseAdapter {
    	/** Remember our context so we can use it when constructing views. */
    	private Context mContext;

    	private List<IconifiedTextSelected> mItems = new ArrayList<IconifiedTextSelected>();

    	public SlowAdapter(Context context) {
    		mContext = context;
    	}

    	public void setListItems(List<IconifiedTextSelected> lit) 
    	{ mItems = lit; }

    	/** @return The number of items in the */
    	public int getCount() { return mItems.size(); }

    	public Object getItem(int position) 
    	{ return mItems.get(position); }

    	/** Use the array index as a unique id. */
    	public long getItemId(int position) {
    		return position;
    	}

    	/** @param convertView The old view to overwrite, if one is passed
    	 * @returns a IconifiedTextSelectedView that holds wraps around an IconifiedText */
    	public View getView(int position, View convertView, ViewGroup parent) {
    		IconifiedTextSelectedView btv;
    		if (convertView == null) {
    			btv = new IconifiedTextSelectedView(mContext, mItems.get(position));
    		} else { // Reuse/Overwrite the View passed
    			// We are assuming(!) that it is castable! 
    			btv = (IconifiedTextSelectedView) convertView;
    			btv.setText(mItems.get(position).getText());
    		}
    		if (position==0) {
    			if (VideoBrowser.self.media_browser_load_option==0) {
    				btv.setIcon(R.drawable.folderback);
    			} else if (mItems.get(0).getIcon()!=null) {
    				btv.setIcon(mItems.get(position).getIcon());
    			} else {
    				btv.setIcon(R.drawable.video);
    			}
    		}
    		//in busy mode
    		else if (mBusy){
    			//if icon is NULL: the icon is not loaded yet; load default icon
    			if (mItems.get(position).getIcon()==null) {
    				btv.setIcon(R.drawable.video);
    				//mark this view, indicates the icon is not loaded
    				btv.setTag(this);
    			} else {
    				//if icon is not null, just display the icon
    				btv.setIcon(mItems.get(position).getIcon());
    				//mark this view, indicates the icon is loaded
    				btv.setTag(null);
    			}
    		} else {
    			//if not busy
    			Drawable d = mItems.get(position).getIcon();
    			if (d == null) {
    				//icon is not loaded, load now
    				btv.setIcon(R.drawable.video);
    				btv.setTag(this);
    			} else {
    				btv.setIcon(mItems.get(position).getIcon());
    				btv.setTag(null);
    			}
    		}
    		return btv;
    	}
    }
}
