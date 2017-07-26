package com.example.android.inventoryapp;

import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

import java.io.ByteArrayOutputStream;

/**
 * Adds a new product entry into the products table.
 * Shows the details of an existing Product Entry from products table and enables user to edit its
 * values.
 */
public class DetailActivity extends AppCompatActivity
        implements android.app.LoaderManager.LoaderCallbacks<Cursor> {
    // Unique id for the Loader.
    private static final int PRODUCT_DETAIL_LOADER = 0;
    // Request code for image capture intent. It is used for checking whether returning intent
    // holds the requested result we need in onActivityResult method.
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    // Stores the Uri of the current product. If it is null then the Activity is in "New product"
    // mode, because the
    private Uri mCurrentProductUri;

    private EditText nameEditText;
    private EditText priceEditText;
    private EditText quantityEditText;
    private EditText supplierPhoneEditText;
    private ImageView productImageView;

    // Variable indicating whether or not the data of Product has been modified.
    // This variable is used for pre-condition for showing confirmation dialogs before exiting the
    // activity.
    private boolean mProductModified = false;

    // Attach listener to view that tracks whether product data has been changed by the user.
    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mProductModified = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        final Intent intent = getIntent();
        // Save the Uri sent by the starting intent. If null then the Activity is in "New product"
        // mode inserting a new Product  instead of "Edit Product" mode updating an existing productt.
        mCurrentProductUri = intent.getData();

        if (mCurrentProductUri == null) {
            setTitle(getString(R.string.detail_activity_title_new_product));
            // Options Menu has changed and should be recreated.
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.detail_activity_title_edit_product));
            getLoaderManager().initLoader(PRODUCT_DETAIL_LOADER, null, this);
        }

        nameEditText = (EditText) findViewById(R.id.name_edit_text);
        priceEditText = (EditText) findViewById(R.id.price_edit_text);
        quantityEditText = (EditText) findViewById(R.id.quantity_edit_text);
        supplierPhoneEditText = (EditText) findViewById(R.id.supplier_phone_edit_text);
        productImageView = (ImageView) findViewById(R.id.product_image_view);

        nameEditText.setOnTouchListener(mOnTouchListener);
        priceEditText.setOnTouchListener(mOnTouchListener);
        quantityEditText.setOnTouchListener(mOnTouchListener);
        supplierPhoneEditText.setOnTouchListener(mOnTouchListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem deleteMenuItem = menu.findItem(R.id.action_delete);

        if (mCurrentProductUri == null) {
            deleteMenuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (!mProductModified) {
            super.onBackPressed();
            return;
        }
        showUnsavedChangesDialog();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_save:
                // Check whether saving was successful or not.
                // If yes, then finish current Activity, if no, then stay in the current Activity.
                if (saveProduct()) {
                    finish();
                }
                return true;
            case R.id.action_delete:
                // Show confirmation dialog in order to prevent accidental removal of Product.
                showDeleteConfirmationDialog();
                return true;
            case R.id.action_take_photo:
                takePhoto();
                return true;
            case android.R.id.home:
                if (!mProductModified) {
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                }
                showUnsavedChangesDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    * Logic for inserting or updating the Product Entry
    */
    private boolean saveProduct() {
        boolean productSaved = true;

        if (isValidProduct()) {
            ContentValues contentValues = getContentValues();
            // Check whether it is a new Product to insert or an existing Product to update.
            if (mCurrentProductUri == null) {
                Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, contentValues);
                // If newUri is not null then insertion was successful else unsuccessful.
                if (newUri == null) {
                    Toast.makeText(this, getString(R.string.toast_message_product_not_saved),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.toast_message_product_saved),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                int rowsUpdated = getContentResolver().update(mCurrentProductUri, contentValues,
                        null, null);
                // If there were no rows updated then the update failed.
                if (rowsUpdated == 0) {
                    Toast.makeText(this, getString(R.string.toast_message_product_update_failed),
                            Toast.LENGTH_SHORT).show();
                    productSaved = false;
                } else {
                    Toast.makeText(this, getString(R.string.toast_message_product_updated),
                            Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.toast_message_fill_in_all_fields),
                    Toast.LENGTH_SHORT).show();
            productSaved = false;
        }
        return productSaved;
    }

    /*
    * Makes validation whether all required fields were filled in and photo has been taken.
    */
    private boolean isValidProduct() {
        boolean isValidProduct = true;

        if (TextUtils.isEmpty(nameEditText.getText().toString().trim()) ||
                TextUtils.isEmpty(priceEditText.getText().toString().trim()) ||
                TextUtils.isEmpty(quantityEditText.getText().toString().trim()) ||
                TextUtils.isEmpty(supplierPhoneEditText.getText().toString().trim())) {
            isValidProduct = false;
        }

        // Checking the imageView whether it contains a Drawable taken by the user.
        if (productImageView.getDrawable() == null) {
            isValidProduct = false;
        }
        return isValidProduct;
    }

    /*
    * Extracts the EditText fields and taken photo from the UI.
    */
    private ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();

        String name = nameEditText.getText().toString().trim();
        contentValues.put(ProductEntry.NAME, name);

        String price = priceEditText.getText().toString().trim();
        contentValues.put(ProductEntry.PRICE, price);

        String quantity = quantityEditText.getText().toString().trim();
        contentValues.put(ProductEntry.QUANTITY, quantity);

        String supplierPhone = supplierPhoneEditText.getText().toString().trim();
        contentValues.put(ProductEntry.SUPPLIER_PHONE, supplierPhone);

        /*
        * Retrieve bitmap from ImageView user has taken, compress and convert it into a byte array.
        */
        BitmapDrawable productBitmapDrawable = (BitmapDrawable) productImageView.getDrawable();
        Bitmap bitmapProductImage = productBitmapDrawable.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmapProductImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] image = stream.toByteArray();
        // Put the byte array into the contentValuse that will be store as BLOB in the database.
        contentValues.put(ProductEntry.IMAGE, image);

        return contentValues;
    }

    private void showUnsavedChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.exit_without_saving);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_product);
        builder.setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /*
    * Indicates a delete operation and shows its success in Toast messages.
    */
    private void deleteProduct() {
        int rowsAffected = getContentResolver().delete(mCurrentProductUri, null, null);
        // If now rows were affected by the delete operation then it failed.
        if (rowsAffected == 0) {
            Toast.makeText(this, getString(R.string.toast_message_product_not_deleted),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.toast_message_product_deleted),
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /*
    * Initiates an image capture Intent in case there is a component that can handle it.
    */
    private void takePhoto() {
        Intent captureImageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Check whether there is a component that can receive the Capture Image Intent
        if (captureImageIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(captureImageIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /*
    * Processes the result of the image capture Intent.
    */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            productImageView.setImageBitmap(imageBitmap);
            // Indicate that product has been modified since last state
            mProductModified = true;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.NAME,
                ProductEntry.PRICE,
                ProductEntry.QUANTITY,
                ProductEntry.SUPPLIER_PHONE,
                ProductEntry.IMAGE
        };

        return new CursorLoader(this, mCurrentProductUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.NAME);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.QUANTITY);
            int supplierPhoneColumnIndex = cursor.getColumnIndex(ProductEntry.SUPPLIER_PHONE);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.IMAGE);

            nameEditText.setText(cursor.getString(nameColumnIndex));
            priceEditText.setText(cursor.getString(priceColumnIndex));
            quantityEditText.setText(cursor.getString(quantityColumnIndex));
            supplierPhoneEditText.setText(cursor.getString(supplierPhoneColumnIndex));

            byte[] imageBytes = cursor.getBlob(imageColumnIndex);
            Bitmap bitmapImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            productImageView.setImageBitmap(bitmapImage);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        nameEditText.setText("");
        priceEditText.setText("");
        quantityEditText.setText("");
        supplierPhoneEditText.setText("");
        productImageView.setImageBitmap(null);
    }

    /*
    * Decreases the quantity by one
    */
    public void decreaseQuantity(View view) {
        int quantity;
        String quantityString = quantityEditText.getText().toString().trim();

        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
            // Logic that prevents decrease button to go into negative range.
            if (quantity > 0) {
                quantity = quantity - 1;
                quantityEditText.setText(String.valueOf(quantity));
                mProductModified = true;
            }
        }
    }

    /*
    * Increases the quantity by one
    */
    public void increaseQuantity(View view) {
        int quantity;
        String quantityString = quantityEditText.getText().toString().trim();

        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        } else {
            quantity = 0;
        }
        quantity = quantity + 1;
        quantityEditText.setText(String.valueOf(quantity));
        mProductModified = true;
    }

    /*
    * Sends a dial Intent with the number of supplier.
    */
    public void callSupplier(View view) {
        String supplierPhone = supplierPhoneEditText.getText().toString().trim();
        // Check whether there is a phone number to call. If not then a Toast alerts the user.
        if (TextUtils.isEmpty(supplierPhone)) {
            Toast.makeText(this, R.string.toast_message_add_phone_number, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check whether there is a component that can receive the Dial Intent
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        if (callIntent.resolveActivity(getPackageManager()) != null) {
            callIntent.setData(Uri.parse("tel:" + supplierPhone));
            startActivity(callIntent);
        }
    }
}