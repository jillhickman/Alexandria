package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.Utility;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private EditText mEan;
    private String mSuccesfulEan;
    private final String SUCCESSFUL_EAN = "succssfulEan";
    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT="eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";

    static final int EAN_REQUEST = 1;  // The request code
//    public static final int RESULT_OK = 1;
    public static final String EAN_RESULTS_KEY = "eanResultsKey";

    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mSuccesfulEan !=null){
            outState.putString(SUCCESSFUL_EAN, mSuccesfulEan);
        }
        if(mEan !=null) {
            outState.putString(EAN_CONTENT, mEan.getText().toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EAN_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                mEan.setText(data.getStringExtra(EAN_RESULTS_KEY));
                mSuccesfulEan = data.getStringExtra(EAN_RESULTS_KEY);
            }

        }
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        mEan = (EditText) rootView.findViewById(R.id.ean);

//        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
//        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);

//        rootView.findViewById(R.id.confirm_layout).setVisibility(View.GONE);

        mEan.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean = s.toString();

                //catch isbn10 numbers
                if (ean.length() == 10 && !ean.startsWith("978")) {
                    ean = "978" + ean;
                }
                if (ean.length() < 13) {
                    return;
                }
                if (Utility.isNetworkAvailable(getActivity())) {
                    //Once we have an ISBN, start a book intent
                    Intent bookIntent = new Intent(getActivity(), BookService.class);
                    bookIntent.putExtra(BookService.EAN, ean);
                    bookIntent.setAction(BookService.FETCH_BOOK);
                    getActivity().startService(bookIntent);
                    AddBook.this.restartLoader();
                } else {
                    //No internet, show toast.
                    Toast.makeText(getActivity(), R.string.internet_toast_message, Toast.LENGTH_SHORT).show();
                }
            }
        });

        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This is the callback method that the system will invoke when your button is
                // clicked. You might do this by launching another app or by including the
                //functionality directly in this app.
                // Hint: Use a Try/Catch block to handle the Intent dispatch gracefully, if you
                // are using an external app.
                //when you're done, remove the toast below.
                Intent scanIntent = new Intent(getActivity(), ScannerActivity.class);
                startActivityForResult(scanIntent, EAN_REQUEST);
                getLoaderManager().destroyLoader(LOADER_ID);

            }
        });

        if(savedInstanceState!=null){
            mSuccesfulEan = savedInstanceState.getString(SUCCESSFUL_EAN);
            mEan.setText(savedInstanceState.getString(EAN_CONTENT));

        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!TextUtils.isEmpty(mSuccesfulEan)){

            mEan.setText(mSuccesfulEan);

        }
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(mEan.getText().length()==0){
            return null;
        }
        String eanStr= mEan.getText().toString();
        if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }
        mSuccesfulEan = mEan.getText().toString();
        clearField();

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        if(!TextUtils.isEmpty(authors)){
            String[] authorsArr = authors.split(",");
            ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
            ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",","\n"));
        }else {
            ((TextView) rootView.findViewById(R.id.authors)).setText((""));
        }

        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
            //Using Picasso to load images. Also added default placeholder image.
            ImageView bookImageView = (ImageView) rootView.findViewById(R.id.bookCover);
            if (bookImageView != null) {
                Picasso.with(getActivity()).load(imgUrl).placeholder(R.drawable.ic_launcher).into(bookImageView);
                rootView.findViewById(R.id.bookCover).setVisibility(View.VISIBLE);
            }
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);

        getLoaderManager().destroyLoader(LOADER_ID);

    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    }


    private void clearField(){
        mEan.setText("");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }
}
