package it.jaschke.alexandria;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import butterknife.Bind;
import butterknife.ButterKnife;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {
    @Bind(R.id.ean)
    EditText edtEan;
    @Bind(R.id.scan_fab)
    FloatingActionButton btnScan;
    @Bind(R.id.save_button)
    Button btnSave;
    @Bind(R.id.delete_button)
    Button btnDelete;
    @Bind(R.id.bookTitle)
    TextView txtBookTitle;
    @Bind(R.id.bookSubTitle)
    TextView txtBookSubtitle;
    @Bind(R.id.authors)
    TextView txtAuthors;
    @Bind(R.id.categories)
    TextView txtCategories;
    @Bind(R.id.bookCover)
    ImageView imgBookCover;

    private final String BUNDLE_DATA = "bundleData";

    private final int LOADER_ID = 1;
    private final String EAN_CONTENT = "eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    public static final String ISBN_CODE = "ISBN_BARCODE";
    public static final int REQUEST_CODE_BARCODE_SCAN = 40000;

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";

    boolean mBroadcastIsRegistered;
    View rootView;
    Bundle saveBundle;


    public AddBook() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (edtEan != null)
            outState.putString(EAN_CONTENT, edtEan.getText().toString());
        if (saveBundle != null)
            outState.putBundle(BUNDLE_DATA, saveBundle);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ButterKnife.bind(this, rootView);

        initListeners();

        if (savedInstanceState != null) {
            edtEan.setText(savedInstanceState.getString(EAN_CONTENT));

            if (savedInstanceState.getBundle(BUNDLE_DATA) != null) {
                saveBundle = savedInstanceState.getBundle(BUNDLE_DATA);
                setViews(saveBundle.getString("TITLE"), saveBundle.getString("SUBTITLE"),
                        saveBundle.getString("AUTHORS"), saveBundle.getString("IMGURL"),
                        saveBundle.getString("CAT"));
            }
        }

        return rootView;
    }

    private void initListeners() {
        edtEan.addTextChangedListener(new TextWatcher() {
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
//                    clearFields();
                    return;
                }
                if (isNetworkAvailable()) {
                    //Once we have an ISBN, start a book intent
                    Intent bookIntent = new Intent(getActivity(), BookService.class);
                    bookIntent.putExtra(BookService.EAN, ean);
                    bookIntent.setAction(BookService.FETCH_BOOK);
                    getActivity().startService(bookIntent);
                    AddBook.this.restartLoader();
                } else {
                    Toast.makeText(getActivity(), getResources().getText(R.string.network_unavailble), Toast.LENGTH_LONG).show();
                }
            }
        });

        btnScan.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
    }

    private void restartLoader() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (edtEan.getText().length() == 0) {
            return null;
        }
        String eanStr = edtEan.getText().toString();
        if (eanStr.length() == 10 && !eanStr.startsWith("978")) {
            eanStr = "978" + eanStr;
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
        if(data!=null)
            saveBundle = null;

        if ((!data.moveToFirst()) || saveBundle != null)
            return;


        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));

        saveBundle = new Bundle();
        saveBundle.putString("TITLE", bookTitle);
        saveBundle.putString("SUBTITLE", bookSubTitle);
        saveBundle.putString("AUTHORS", authors);
        saveBundle.putString("IMGURL", imgUrl);
        saveBundle.putString("CAT", categories);

        setViews(bookTitle, bookSubTitle, authors, imgUrl, categories);
    }

    private void setViews(String bookTitle, String bookSubTitle, String authors, String imgUrl, String categories) {
        txtBookTitle.setText(bookTitle);
        txtBookSubtitle.setText(bookSubTitle);
        if(authors!=null) {
            String[] authorsArr = authors.split(",");
            txtAuthors.setLines(authorsArr.length);
            txtAuthors.setText(authors.replace(",", "\n"));
        }else{
            txtAuthors.setText("Author Unknown");
        }
        if (Patterns.WEB_URL.matcher(imgUrl).matches()) {
            Glide.with(this)
                    .load(imgUrl)
                    .centerCrop()
                    .crossFade()
                    .into(imgBookCover);
            imgBookCover.setVisibility(View.VISIBLE);
        }
        txtCategories.setText(categories);

        btnSave.setVisibility(View.VISIBLE);
        btnDelete.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields() {
        txtBookTitle.setText("");
        txtBookSubtitle.setText("");
        txtAuthors.setText("");
        txtCategories.setText("");
        imgBookCover.setVisibility(View.INVISIBLE);
        btnSave.setVisibility(View.INVISIBLE);
        btnDelete.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    @Override
    public void onClick(View view) {
        if (view == btnScan) {
            startActivityForResult(new Intent(getActivity(), BarcodeScannerActivity.class), REQUEST_CODE_BARCODE_SCAN);
        } else if (view == btnSave) {
            clearFields();
            edtEan.setText("");
        } else if (view == btnDelete) {
            Intent bookIntent = new Intent(getActivity(), BookService.class);
            bookIntent.putExtra(BookService.EAN, edtEan.getText().toString());
            bookIntent.setAction(BookService.DELETE_BOOK);
            getActivity().startService(bookIntent);
            clearFields();
            edtEan.setText("");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (Activity.RESULT_OK == resultCode) {

            if (REQUEST_CODE_BARCODE_SCAN == requestCode) {

                edtEan.setText(data.getStringExtra(ISBN_CODE));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mBroadcastIsRegistered) {
            getActivity().registerReceiver(informUser,
                    new IntentFilter(BookService.BROADCAST_INFORM));
            mBroadcastIsRegistered = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mBroadcastIsRegistered) {
            getActivity().unregisterReceiver(informUser);
            mBroadcastIsRegistered = false;
        }
    }

    private boolean isNetworkAvailable() {

        ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    private BroadcastReceiver informUser = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Toast.makeText(getActivity(), intent.getStringExtra("INFORM_TITLE") + " added.", Toast.LENGTH_LONG).show();
        }
    };
}
