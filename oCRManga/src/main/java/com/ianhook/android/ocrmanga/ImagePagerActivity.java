package com.ianhook.android.ocrmanga;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.eyesfree.ocr.client.Ocr;
import com.googlecode.eyesfree.ocr.client.OcrResult;
import com.googlecode.eyesfree.ocr.client.Ocr.CompletionCallback;
import com.googlecode.eyesfree.ocr.client.Ocr.Parameters;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.ianhook.android.ocrmanga.util.ImageFileReader;
import com.ianhook.android.ocrmanga.util.MangaReader;
import com.ianhook.android.ocrmanga.util.OcrGeneticDetection;
import com.ianhook.android.ocrmanga.util.OcrRectTest;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;

public class ImagePagerActivity extends FragmentActivity {
    private final static String PAGE_NUM = "page";
    public final static String FILE_NAME = "com.ianhook.android.ocrmanga.FILE_NAME";
    private static final String PARAMS_FILE = "/storage/sdcard/Manga/jpn_params.ser";
    private static final String BEST_PARAMS_FILE = "/storage/sdcard/Manga/ga_best_params.ser";

    public static final String TAG = "ImagePagerActivity";
    private ImagePagerAdapter mIPA;
    private ViewPager mViewPager;
    private static Ocr ocr;
    private ImageFragment mCurrentFragment;
    private static FragmentManager mFragManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //hide the action bar (menu options at top) 
        ActionBar actionBar = getActionBar();
        actionBar.hide();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.argb(128, 0, 0, 0)));

        setContentView(R.layout.activity_image_pager);
        
        Intent intent = getIntent();
        String file_name = intent.getStringExtra(FILE_NAME);
        File f = new File(file_name);
        actionBar.setTitle(f.getName());
        
        mFragManager = getSupportFragmentManager();
        try {
            mIPA = new ImagePagerAdapter(mFragManager, file_name, recordDisplaySize());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mIPA);
        mViewPager.setBackgroundColor(Color.BLACK);

        SharedPreferences settings = getSharedPreferences(mIPA.getFileName(), 0);
        mViewPager.setCurrentItem(settings.getInt(PAGE_NUM, mIPA.getCount()));

        if (savedInstanceState == null && !OcrGeneticDetection.mDoGA) {
            Log.d(TAG, "creating OCR");
            ocr = new Ocr(this, null);
            /* TODO
            Parameters params;
            try
            {
                FileInputStream fileIn = new FileInputStream(PARAMS_FILE);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                params = (Parameters) in.readObject();
                in.close();
                fileIn.close();
            }catch(IOException i)
            {
                Log.d(TAG, "IOException on params file");
                i.printStackTrace();
                return;
            }catch(ClassNotFoundException c)
            {
                Log.d(TAG, "Parameters class not found");
                c.printStackTrace();
                return;
            }
            */
            Parameters params = ocr.getParameters();
            params.setFlag(Parameters.FLAG_DEBUG_MODE, false);
            params.setFlag(Parameters.FLAG_ALIGN_TEXT, false);
            params.setLanguage("jpn");
            params.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT);
            //params.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
            /*
            //Fitness: 20304
            params.setVariable("edge_tile_x", "30");
            params.setVariable("edge_tile_y", "32");
            params.setVariable("edge_thresh", "44");
            params.setVariable("edge_avg_thresh", "4");
            params.setVariable("single_min_aspect", "0.12387389438495032");
            params.setVariable("single_max_aspect", "7.140991507983401");
            params.setVariable("single_min_area", "25");
            params.setVariable("single_min_density", "0.2814948034673991");
            params.setVariable("pair_h_ratio", "1.2320803151162179");
            params.setVariable("pair_d_ratio", "1.6700535687077367");
            params.setVariable("pair_h_dist_ratio", "1.2066236790559628");
            params.setVariable("pair_v_dist_ratio", "1.0045547320629222");
            params.setVariable("pair_h_shared", "0.14599912098744383");
            params.setVariable("cluster_width_spacing", "3");
            params.setVariable("cluster_shared_edge", "0.8526428970173985");
            params.setVariable("cluster_h_ratio", "0.28984088586226053");
            params.setVariable("cluster_min_blobs", "2");
            params.setVariable("cluster_min_aspect", "1.78901587228034");
            params.setVariable("cluster_min_fdr", "3.0215533597321147");
            params.setVariable("cluster_min_edge", "35");
            params.setVariable("cluster_min_edge_avg", "35");
            */

            /*
            params.setVariable("edge_tile_x", "17");
            params.setVariable("edge_tile_y", "51");
            params.setVariable("edge_thresh", "42");
            params.setVariable("edge_avg_thresh", "26");
            params.setVariable("single_min_aspect", "0.1226374440099194");
            params.setVariable("single_max_aspect", "4.811634464353835");
            params.setVariable("single_min_area", "6");
            params.setVariable("single_min_density", "0.3068444569992789");
            params.setVariable("pair_h_ratio", "1.7957964645383617");
            params.setVariable("pair_d_ratio", "1.4796231886128681");
            params.setVariable("pair_h_dist_ratio", "0.25631797578843374");
            params.setVariable("pair_v_dist_ratio", "0.10635169868222327");
            params.setVariable("pair_h_shared", "0.20969381113262964");
            params.setVariable("cluster_width_spacing", "5");
            params.setVariable("cluster_shared_edge", "0.8834573555007488");
            params.setVariable("cluster_h_ratio", "1.4790379594586032");
            params.setVariable("cluster_min_blobs", "5");
            params.setVariable("cluster_min_aspect", "1.78901587228034");
            params.setVariable("cluster_min_fdr", "2.605697183075306");
            params.setVariable("cluster_min_edge", "25");
            params.setVariable("cluster_min_edge_avg", "10");
            */
            /*
            try
            {
                FileOutputStream fileOut =
                        new FileOutputStream("/storage/sdcard/Manga/jpn_params.ser");
                ObjectOutputStream out = new ObjectOutputStream(fileOut);
                out.writeObject(params);
                out.close();
                fileOut.close();
            }catch(IOException i)
            {
                i.printStackTrace();
            }
            */
            try {
                FileInputStream fileIn =
                        new FileInputStream(PARAMS_FILE);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                params = (Parameters) in.readObject();
                in.close();
                fileIn.close();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch(IOException i) {
                i.printStackTrace();
            }

            ocr.setParameters(params);
        }
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.image_pager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_highlight) {
            Log.d(TAG, "Find text");
            mCurrentFragment.findText();
            return true;
        } else if (id == R.id.action_add_test) {
            mCurrentFragment.saveTest();
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void menuForFragment(ImageFragment v) {

        getActionBar().show();
        mCurrentFragment = v;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int id = data.getIntExtra(DisplayMessageActivity.HIGHLIGHT_ID, -1);
        if(resultCode == DisplayMessageActivity.UPDATE_HIGHLIGHT) {
            RectF highlight = (RectF) data.getParcelableExtra(DisplayMessageActivity.HIGHLIGHT);
            if (id > -1) {
                Log.d(TAG, highlight.toString());
                mCurrentFragment.updateHighlight(id, highlight);
            }
        } else if(resultCode == DisplayMessageActivity.NEXT) {
            mCurrentFragment.next(id);
        } else if(resultCode == DisplayMessageActivity.PREVIOUS) {
            mCurrentFragment.previous(id);
        } else if(resultCode == DisplayMessageActivity.DELETE) {
            mCurrentFragment.delete(id);
        }
    }

    @Override
    protected void onStop(){
       super.onStop();

      // We need an Editor object to make preference changes.
      // All objects are from android.context.Context
      SharedPreferences settings = getSharedPreferences(mIPA.getFileName(), 0);
      SharedPreferences.Editor editor = settings.edit();
      editor.putInt(PAGE_NUM, mViewPager.getCurrentItem());

      // Commit the edits!
      editor.commit();
    }
    
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private Point recordDisplaySize() {

        Display display = getWindowManager().getDefaultDisplay();
        Point screenSize = new Point()  ;
        
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealSize(screenSize);
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(screenSize);
        } else {
            screenSize = new Point(display.getWidth(), display.getHeight());
        }
        return screenSize;
    }
    
    public static class ImagePagerAdapter extends FragmentStatePagerAdapter {

        private static ImageFileReader mMangaReader;
        private static Point mScreenSize;

        public ImagePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public ImagePagerAdapter(FragmentManager fm, String file_name, Point screen_size) throws IOException {
            super(fm);
            setScreen(screen_size);
            setFile(file_name);
        }
        
        public void setScreen(Point size) {
            mScreenSize = size;
        }

        public void setFile(String file_name) throws IOException {
            Log.v(TAG, "loading file " + file_name);
            setFile(new File(file_name));
        }

        public void setFile(File file) throws IOException {
            String name = file.getName();
            if(name.endsWith("zip")) {
                mMangaReader = new MangaReader(file, mScreenSize);
            } else if(name.endsWith("ser")) {
                mMangaReader = new MangaReader(file, mScreenSize);
            } else {
                throw new IOException("unknown file type");
            }
        }
        
        public static String getFileName() {
            return mMangaReader.getFileName();
        }
        
        @Override
        public Fragment getItem(int arg0) {
            Log.v(TAG, String.format("setting image %d", arg0));
            
            Fragment fragment = new ImageFragment();
            Bundle args = new Bundle();
            args.putInt(ImageFragment.ARG_OBJECT, getCount() - arg0);
            
            fragment.setArguments(args);
            
            // TODO Auto-generated method stub
            // Our object is just an integer :-P
            return fragment;
        }

        @Override
        public int getCount() {
            return mMangaReader.getCount();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "file: " + mMangaReader.getImageName(position);
        }
        
        static private Bitmap getImage(int position) {
            return mMangaReader.getImage(position);
        }

        static public Point getScreenSize() {
            return mScreenSize;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class ImageFragment extends Fragment { 
        public static final String ARG_OBJECT = "int";
        
        private int position;
        private ImageViewTouch mImageView;
        private Bitmap mBitmap;
        private View mRootView;
        private List<OcrResult> mResults;
        private List<Highlighter> mHighlights;
        
        @SuppressWarnings("unused")
        private Highlighter displayHighlight(Rect bounds) {
            return displayHighlight(mImageView, bounds.left, bounds.top, 
                    bounds.width(), bounds.height());
        }
        
        @SuppressLint("NewApi")
        private Highlighter displayHighlight(View v, int x, int y, int width, int height) {
            //FrameLayout current = (FrameLayout) v.getParent();
            
            LayoutInflater vi = (LayoutInflater) getActivity().getApplicationContext().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            Highlighter newHighlight = (Highlighter) vi.inflate(R.layout.fragment_highlighter, null);
            RectF temp = new RectF(x,y,x+width,y+height);
            newHighlight.setRect(temp);

            newHighlight.setImagePage(this);

            ((ViewGroup) v.getParent()).addView(newHighlight);
            return newHighlight;          
            
        }

        public void next(int id) {
            id += 1;
            if(id == mHighlights.size()) id = 0;

            mHighlights.get(id).onClick(mHighlights.get(id));
        }

        public void previous(int id) {
            id -= 1;
            if(id < 0) id = mHighlights.size() - 1;

            mHighlights.get(id).onClick(mHighlights.get(id));
        }

        public void delete(int id){
            mHighlights.remove(id);
        }

        public void updateHighlight(int id, RectF bounds) {
            Log.d(TAG, String.format("number of highlights %d: %d", mHighlights.size(), id));
            mHighlights.get(id).updateRectF(bounds);
        }

        public ImageViewTouch getImageView() {
            return mImageView;
        }


        public Bitmap getBitmapSection(RectF bounds) {
            return getBitmapSection(bounds, null);
        }

        public Bitmap getBitmapSection(RectF bounds, RectF pad) {

            RectF outRect = new RectF(bounds);

            RectF highlightRectF = new RectF(bounds);

            Matrix dmat = mImageView.getImageViewMatrix();

            Log.v(TAG, String.format("dmat %s", dmat.toString()));

            dmat.invert(dmat);

            outRect.left = (float) Math.max(0.0, outRect.left);
            outRect.top = (float) Math.max(0.0, outRect.top);

            if(mBitmap.isRecycled()) {
                drawImage();
            }

            outRect.bottom = (float) Math.min(mBitmap.getHeight(), outRect.bottom);
            outRect.right = (float) Math.min(mBitmap.getWidth(), outRect.right);

            if(pad != null) {
                pad.left = outRect.width() * .75f;
                pad.top = outRect.height() * .75f;
                if(outRect.left - pad.left < 0) {
                    pad.left += outRect.left - pad.left; //this should be negative so we add
                }
                if(outRect.top - pad.top < 0) {
                    pad.top += outRect.top - pad.top; //this should be negative so we add
                }
                pad.right = pad.left + highlightRectF.width();
                pad.bottom = pad.top + highlightRectF.height();

                outRect.left = Math.max(0, outRect.left - pad.left);
                outRect.top = Math.max(0, outRect.top - pad.top);
                outRect.right = Math.min(mBitmap.getWidth(), outRect.right + outRect.width() * .75f);
                outRect.bottom = Math.min(mBitmap.getHeight(), outRect.bottom + outRect.height() * .75f);

                Log.v(TAG, String.format("padding %f, %f, %f, %f", pad.left,
                        pad.top,
                        pad.width(),
                        pad.height()));
            }

            Log.v(TAG, String.format("highlight %f, %f, %f, %f", bounds.left,
                    bounds.top,
                    bounds.width(),
                    bounds.height()));
            Log.v(TAG, String.format("outRect %f, %f, %f, %f", outRect.left,
                    outRect.top,
                    outRect.width(), outRect.height()));

            Log.v(TAG, String.format("imageView %d,%d,%d,%d", mImageView.getLeft(),
                    mImageView.getTop(), mImageView.getWidth(), mImageView.getHeight()));
            Log.v(TAG, String.format("bitmap %d,%d", mBitmap.getWidth(), mBitmap.getHeight()));

            Bitmap resizedBitmap = Bitmap.createBitmap(mBitmap,
                    (int)outRect.left,
                    (int)outRect.top,
                    (int)outRect.width(),
                    (int)outRect.height());

            return resizedBitmap;
        }

        public Ocr getOcr() {
            return ocr;
        }
        
        public void findText() {
            CompletionCallback displayHighlightCB = new DisplayHighlightCB();
            if(mResults == null) {
                ocr.setCompletionCallback(displayHighlightCB);
                ocr.enqueue(mBitmap);
            } else {
                displayHighlightCB.onCompleted(mResults);
            }
        }

        public void saveTest() {
            //find the text in the image, in case it hasn't been done already
            //findText();
            OcrRectTest ocrTest = new OcrRectTest();
            ocrTest.writeTests(ImagePagerAdapter.getFileName(), position, mResults, ImagePagerAdapter.getScreenSize());
        }
    
        private OnLongClickListener mLongClickListener = new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Log.d(TAG, "Long Click caught");

                ImagePagerActivity IPA = (ImagePagerActivity) getActivity();
                if(IPA.getActionBar().isShowing()) {
                    IPA.getActionBar().hide();
                } else {
                    IPA.menuForFragment(ImageFragment.this);
                }
                    
                return true;
            }   
        };

        private class DisplayHighlightCB implements CompletionCallback {

            @Override
            public void onCompleted(List<OcrResult> results) {
                String tag = "DisplayHighlightCB";
                Log.d(tag, "got some results");
                ImagePagerActivity IPA = (ImagePagerActivity) getActivity();
                if (IPA.getActionBar().isShowing()) {
                    IPA.getActionBar().hide();
                }
                mResults = results;
                if (results.isEmpty()) {
                    Log.d(tag, "no text found");
                } else {
                    mHighlights = new ArrayList<Highlighter>();
                    for (int i = 0; i < results.size(); i++) {
                        if (results.get(i).getBounds().top == 0) {
                            continue;
                        }

                        Highlighter tempHI = displayHighlight(results.get(i).getBounds());
                        tempHI.setHighlightID(i);
                        mHighlights.add(tempHI);
                        Log.d(tag, results.get(i).getBounds().flattenToString());
                        Log.d(tag, results.get(i).getString());
                    }
                }
            }
        }

        public ImageFragment() {
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mRootView = inflater.inflate(R.layout.fragment_image_pager,
                    container, false);
            
            Bundle args = getArguments();
            position = args.getInt(ARG_OBJECT);
            
            mImageView = (ImageViewTouch) mRootView.findViewById(R.id.imageView);
            mImageView.setOnLongClickListener(mLongClickListener);
            //mImageView.setOnTouchListener(mTouchListener);
            
            return mRootView;
        }
        
        private void drawImage() {

            if(mBitmap == null || mBitmap.isRecycled()) {

                mImageView.setOnLayoutChangeListener(new ImageViewTouchBase.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChanged(boolean changed, int left, int top, int right, int bottom) {

                        if(mHighlights != null) {
                            for (int i = 0; i < mHighlights.size(); i++) {
                                mHighlights.get(i).setImagePage(ImageFragment.this);
                                mHighlights.get(i).setHighlightID(i);
                            }
                        }
                    }
                });

                mBitmap = ImagePagerAdapter.getImage(position);
                mImageView.setImageBitmap(mBitmap, null, 1f, 8f);
                //TODO fix initial image display
                //works with smaller images
                //mImageView.setDisplayType(DisplayType.NONE);
                //work with larger images
                mImageView.setDisplayType(DisplayType.FIT_TO_SCREEN);
                
                Log.v(TAG, String.format("image set %d", position));
                try{
                    Log.v(TAG, String.format("image dims %d", mBitmap.getWidth()));
                } catch (NullPointerException e) {
                    Log.v(TAG, String.format("image width"));
                }
                try{
                    Log.v(TAG, String.format("image dims %d", mBitmap.getHeight()));
                } catch (NullPointerException e) {
                    Log.v(TAG, String.format("image height"));
                }
                Log.v(TAG, String.format("image dims %d,%d", mBitmap.getWidth(), mBitmap.getHeight()));
                
            }
            ProgressBar progress = (ProgressBar) getActivity().findViewById(R.id.progress);
            progress.setVisibility(View.GONE);
        }
        
        private void deleteImage() {

            if(mBitmap != null && !mBitmap.isRecycled()) {
                mImageView.setImageBitmap(null);
                mBitmap.recycle();
                Log.v(TAG, String.format("image delete %d", position));
            }
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            
            Log.v(TAG, String.format("act create %d", position));
        }
        
        @Override
        public void onStart() {
            super.onStart();
            
            Log.v(TAG, String.format("start %d", position));
        }
        
        @Override
        public void onResume() {
            super.onResume();
            
            Log.v(TAG, String.format("resume %d", position));
            drawImage();
        }

        @Override
        public void onPause() {
            super.onPause();

            getActivity().getActionBar().hide();
            Log.v(TAG, String.format("pause %d", position));
            deleteImage();
        }

        @Override
        public void onStop() {
            super.onStop();
            
            Log.v(TAG, String.format("stop %d", position));
            deleteImage();
        }
    }
}
