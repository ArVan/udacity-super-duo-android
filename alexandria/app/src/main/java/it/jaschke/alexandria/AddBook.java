package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.services.DownloadImage;


public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private EditText eanEditText;
    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT="eanContent";

    private Toast mToast;


    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(eanEditText !=null) {
            outState.putString(EAN_CONTENT, eanEditText.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        eanEditText = (EditText) rootView.findViewById(R.id.ean);

        eanEditText.addTextChangedListener(new TextWatcher() {
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
                String ean =s.toString();
                //catch isbn10 numbers
                if(ean.length()==10 && !ean.startsWith("978")){
                    ean="978"+ean;
                }
                if(ean.length()<13){
                    clearFields();
                    return;
                }
                //Once we have an ISBN, start a book intent
                fetchBook(ean);
            }
        });

        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(Utilies.isNetworkAvailable(getActivity())) {
                scanBarCode();
               } else {
                   mToast = Toast.makeText(getContext(), getString(R.string.message_not_connected), Toast.LENGTH_SHORT);
                    mToast.show();
               }
            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, eanEditText.getText().toString());
                bookIntent.setAction(BookService.SAVE_BOOK);
                getActivity().startService(bookIntent);
                eanEditText.setText("");
            }
        });

        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, eanEditText.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                eanEditText.setText("");
            }
        });

        if(savedInstanceState!=null){
            eanEditText.setText(savedInstanceState.getString(EAN_CONTENT));
            eanEditText.setHint("");
        }

        return rootView;
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(eanEditText.getText().length()==0){
            return null;
        }
        String eanStr= eanEditText.getText().toString();
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

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));

        if(authors != null) {
            String[] authorsArr = authors.split(",");
            ((TextView) rootView.findViewById(R.id.authors)).setLines(authorsArr.length);
            ((TextView) rootView.findViewById(R.id.authors)).setText(authors.replace(",","\n"));
        }

        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            new DownloadImage((ImageView) rootView.findViewById(R.id.bookCover)).execute(imgUrl);
        }

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        ((TextView) rootView.findViewById(R.id.categories)).setText(categories);

        rootView.findViewById(R.id.foundBookContainer).setVisibility(View.VISIBLE);

        if(data.getInt(data.getColumnIndex(AlexandriaContract.BookEntry.IS_SAVED)) == 0) {
            rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
        } else {
            rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.foundBookContainer).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    private void fetchBook(String isbn) {
        if(Utilies.isNetworkAvailable(getActivity())) {
            Intent bookIntent = new Intent(getActivity(), BookService.class);
            bookIntent.putExtra(BookService.EAN, isbn);
            bookIntent.setAction(BookService.FETCH_BOOK);
            getActivity().startService(bookIntent);
            AddBook.this.restartLoader();
        } else {
            mToast = Toast.makeText(getContext(), getString(R.string.message_not_connected), Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    /**
     * Scan a ISBN book barcode using the zxing library
     */
    private void scanBarCode() {
            // create the intent integrator to scan in the current fragment
            IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);

            // use a custom scanactivity that can be rotated
            integrator.setCaptureActivity(ScanerActivity.class);
            integrator.setOrientationLocked(false);

            // limit scanning to only one-dimensional barcodes
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);

            // set the prompt message
            integrator.setPrompt(getString(R.string.scanner_prompt));

            // launch the scanner
            integrator.initiateScan();
    }

    /**
     * Catch the result of a scanned barcode
     * @param requestCode int
     * @param resultCode int
     * @param data Intent
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // parse the result in a intentresult object
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if(result != null) {
            if(result.getContents() == null) {
                mToast = Toast.makeText(getActivity(), getString(R.string.scanner_cancelled), Toast.LENGTH_SHORT);
                mToast.show();
            } else {

                // get the scanned code and code format
                String ean = result.getContents();

                // inform the user about the ean number
                mToast = Toast.makeText(getActivity(), getString(R.string.scanned_ean, ean), Toast.LENGTH_SHORT);
                mToast.show();

                // once we have an isbn, add it to the search field (this will trigger the bookservice call)
                eanEditText.setText(ean);
            }
        }
    }
}
