/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.it494.skora.winkpage;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * This fragment has a big {@ImageView} that shows PDF pages, and 2 {@link android.widget.Button}s to move between
 * pages. We use a {@link android.graphics.pdf.PdfRenderer} to render PDF pages as {@link android.graphics.Bitmap}s.
 */
public class PdfRendererBasicFragment extends Fragment implements View.OnClickListener {

    private static final int REQUEST_CONNECT_DEVICE = 1;
    /**
     * Key string for saving the state of current page index.
     */
    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";

    /**
     * File descriptor of the PDF.
     */
    private ParcelFileDescriptor mFileDescriptor;

    /**
     * {@link android.graphics.pdf.PdfRenderer} to render the PDF.
     */
    private PdfRenderer mPdfRenderer;

    /**
     * Page that is currently shown on the screen.
     */
    private PdfRenderer.Page mCurrentPage;

    /**
     * {@link android.widget.ImageView} that shows a PDF page as a {@link android.graphics.Bitmap}
     */
    private ImageView mImageView;

    /**
     * {@link android.widget.Button} to move to the previous page.
     */
    private Button mButtonPrevious;

    /**
     * {@link android.widget.Button} to move to the next page.
     */
    private Button mButtonNext;

    private Button mButtonTop;

    private Button mButtonLast;

    private View mProgressView;

    private Button connectedDevices;

    private ViewFlipper viewFlipper;

    private String TAG = PdfRendererBasicFragment.class.getSimpleName();

    public boolean isDismissedFromSetting;

    private int curPage = -1;

    public static boolean isopened;

    public PdfRendererBasicFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e(TAG, "+ ON create View  +");
        return inflater.inflate(R.layout.fragment_pdf_renderer_basic, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        isopened=false;

        isDismissedFromSetting=false;
        // Retain view references.
        mImageView = (ImageView) view.findViewById(R.id.image);
        mButtonPrevious = (Button) view.findViewById(R.id.previous);
        mButtonNext = (Button) view.findViewById(R.id.next);
        mButtonTop = (Button) view.findViewById(R.id.top);
        mButtonLast = (Button) view.findViewById(R.id.last);
        viewFlipper = (ViewFlipper) view.findViewById(R.id.viewflipper);

        // Bind events.
        mButtonPrevious.setOnClickListener(this);
        mButtonNext.setOnClickListener(this);
        mButtonTop.setOnClickListener(this);
        mButtonLast.setOnClickListener(this);

        connectedDevices = (Button) view.findViewById(R.id.connected_devices_values);
        connectedDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MainActivity parent = (MainActivity) getActivity();
                try {
                    parent.setupBluetooth();
                }catch(Exception e)
                {
                    e.printStackTrace();
                }
//                Intent serverIntent = new Intent(parent, DeviceListActivity.class);
//                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);

                new AlertDialog.Builder(getActivity())
                        .setTitle("Start Google Glass")
                        .setMessage("Start WinkPageTurner on Google Glass and connect to this device")
                        .setPositiveButton("OK", null)
                        .show();

            }
        });



        // Show the first page by default.
        int index = 0;
        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        if (null != savedInstanceState) {
            index = savedInstanceState.getInt(STATE_CURRENT_PAGE_INDEX, 0);
            Log.e(TAG, "+ Restore Created +");
        }
        Log.e(TAG, "+ ON View Created +");
        showPage(index);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MainActivity parent = (MainActivity) getActivity();
        parent.onActivityResult(requestCode, resultCode, data);
        Log.e(TAG, "+ ON activity result +");
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isopened==true)
        {

            Log.e(TAG, "+++++about to close++++++");
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.detach(this).attach(this).commit();


            ProgressDialog progress = ProgressDialog.show(getActivity(), "First set up", "Setting up example file ...", true);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            File file = new File(getActivity().getExternalMediaDirs()[0].getPath() + "/test.pdf");

            //dialog.dismiss();
            //Log.e(TAG, "async dismissed");
            try {
                mPdfRenderer = new PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
            } catch (IOException e) {
                e.printStackTrace();
            }
            progress.dismiss();

            isopened=false;

        }
        Log.e(TAG, "+++++ON RESUME PDF++++++");

    }



    @Override
    public void onAttach(Activity activity) {
        Log.e(TAG, "+ ON attach +");
        super.onAttach(activity);
        try {
            Log.e(TAG, activity.getLocalClassName());

            openRenderer(activity);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
            activity.finish();
        }
    }

    @Override
    public void onDetach() {
        try {
            closeRenderer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mCurrentPage) {
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mCurrentPage.getIndex());
        }
    }

    /**
     * Sets up a {@link android.graphics.pdf.PdfRenderer} and related resources.
     */
    private void openRenderer(Context context) throws IOException {
        //AssetFileDescriptor afd = context.getAssets().openFd("sample.pdf");

        // In this sample, we read a PDF from the assets directory.
        //mFileDescriptor = afd.getParcelFileDescriptor();

            Log.e(TAG, context.getExternalMediaDirs()[0].getPath() + "/test.pdf");
            File file = new File(context.getExternalMediaDirs()[0].getPath() + "/test.pdf");
        if (file.exists())
        {
            mPdfRenderer = new PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));

        }
        else
        {
            ArrayList<String> files = new ArrayList<String>();
            files.add("test.pdf");
            new FileAsyncTask().execute(files);


        }



    }
    private class FileAsyncTask extends AsyncTask<ArrayList<String>, Void, Void>  {
        ArrayList<String> files;
        ProgressDialog dialog;
        @Override
        protected void onPreExecute() {


        }
        @Override
        protected Void doInBackground(ArrayList<String>... params) {
            files = params[0];
            for (int i = 0; i < files.size(); i++) {
                copyFileToSDCard(files.get(i));
            }             return null;
        }
        @Override
        protected void onPostExecute(Void result) {

        }
    }
    public void copyFileToSDCard(String fileFrom){
        AssetManager is = getActivity().getAssets();
        InputStream fis;
        try {

            fis = is.open(fileFrom);
            FileOutputStream fos;

            fos = new FileOutputStream(new File(getActivity().getExternalMediaDirs()[0].getPath(), fileFrom));

            byte[] b = new byte[8];
            int i;
            while ((i = fis.read(b)) != -1) {
                fos.write(b, 0, i);
            }
            fos.flush();
            fos.close();
            fis.close();
            Log.e(TAG, "file created");
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static boolean copyFile(String from, String to) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(from);
            if (oldfile.exists()) {
                InputStream inStream = new FileInputStream(from);
                FileOutputStream fs = new FileOutputStream(to);
                byte[] buffer = new byte[1444];
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread;
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                fs.close();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Closes the {@link android.graphics.pdf.PdfRenderer} and related resources.
     *
     * @throws java.io.IOException When the PDF file cannot be closed.
     */
    private void closeRenderer() throws IOException {
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }

        if(mPdfRenderer!=null && mFileDescriptor!=null)
        {
            mFileDescriptor.close();
            mPdfRenderer.close();
        }


    }

    /**
     * Shows the specified page of PDF to the screen.
     *
     * @param index The page index.
     */
    private void showPage(int index) {
        if(mPdfRenderer==null)
        {
            //showProgress(false);
            ProgressDialog progress = ProgressDialog.show(getActivity(), "First set up", "Setting up example file ...", true);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            File file = new File(getActivity().getExternalMediaDirs()[0].getPath() + "/test.pdf");

            //dialog.dismiss();
            //Log.e(TAG, "async dismissed");
            try {
                mPdfRenderer = new PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
            } catch (IOException e) {
                e.printStackTrace();
            }
            progress.dismiss();
        }
        if (mPdfRenderer.getPageCount() <= index) {
            Log.e(TAG, "Curpage" +index);
            return;
        }
        // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }



        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get
        // the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // We are ready to show the Bitmap to user.
        mImageView.setImageBitmap(bitmap);
        mImageView.invalidate();
        curPage=index;
        updateUi();
    }

    /**
     * Updates the state of 2 control buttons in response to the current page index.
     */
    private void updateUi() {
        int index = mCurrentPage.getIndex();
        int pageCount = mPdfRenderer.getPageCount();
        mButtonPrevious.setEnabled(0 != index);
        mButtonTop.setEnabled(0 != index);
        mButtonNext.setEnabled(index + 1 < pageCount);
        mButtonLast.setEnabled(index + 1 < pageCount);
        getActivity().setTitle(getString(R.string.app_name_with_index, index + 1, pageCount));
    }

    /**
     * Gets the number of pages in the PDF. This method is marked as public for testing.
     *
     * @return The number of pages.
     */
    public int getPageCount() {
        return mPdfRenderer.getPageCount();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.previous: {
                // Move to the previous page
                //showPage(mCurrentPage.getIndex() - 1);
                goToPrevPage();
                break;
            }
            case R.id.next: {
                // Move to the next page
                //showPage(mCurrentPage.getIndex() + 1);
                goToNextPage();
                break;
            }
            case R.id.top:{
                goToTopPage();
                break;
            }
            case R.id.last:{
                goToLastPage();
                break;
            }
        }
    }

    public void goToNextPage(){

        if(mCurrentPage.getIndex() + 1<=getPageCount()-1) {

            // Next screen comes in from left.
            viewFlipper.setInAnimation(this.getActivity(), R.anim.slide_in_from_right);
            // Current screen goes out from right.
            viewFlipper.setOutAnimation(this.getActivity(), R.anim.slide_out_to_left);


            // Display next screen.
            viewFlipper.showNext();

            showPage(mCurrentPage.getIndex() + 1);

        }
    }
    public void goToPrevPage(){

        if(mCurrentPage.getIndex() - 1>=0)
        {

            // Next screen comes in from right.
            viewFlipper.setInAnimation(this.getActivity(), R.anim.slide_in_from_left);
            // Current screen goes out from left.
            viewFlipper.setOutAnimation(this.getActivity(), R.anim.slide_out_to_right);

            // Display previous screen.
            viewFlipper.showPrevious();

            showPage(mCurrentPage.getIndex() - 1);
        }

    }
    public void goToTopPage(){

        // Next screen comes in from right.
        viewFlipper.setInAnimation(this.getActivity(), R.anim.slide_in_from_left);
        // Current screen goes out from left.
        viewFlipper.setOutAnimation(this.getActivity(), R.anim.slide_out_to_right);

        // Display previous screen.
        viewFlipper.showPrevious();

        showPage(0);
    }
    public void goToLastPage(){

        // Next screen comes in from left.
        viewFlipper.setInAnimation(this.getActivity(), R.anim.slide_in_from_right);
        // Current screen goes out from right.
        viewFlipper.setOutAnimation(this.getActivity(), R.anim.slide_out_to_left);


        // Display next screen.
        viewFlipper.showNext();

        showPage(mPdfRenderer.getPageCount()-1);
    }

    public void setDeviceNameToScreen(String deviceName, boolean isOn)
    {
        if(isOn)
        {
            connectedDevices.setText(deviceName);
        }
        else
        {
            connectedDevices.setText(getString(R.string.turnon));
        }

    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            viewFlipper.setVisibility(show ? View.GONE : View.VISIBLE);
            viewFlipper.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    viewFlipper.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            viewFlipper.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

}
