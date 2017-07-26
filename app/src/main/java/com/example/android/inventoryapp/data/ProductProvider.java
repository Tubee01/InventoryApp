package com.example.android.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

/**
 * Content Provider that manages CRUD methods in product_inventory database.
 */
public class ProductProvider extends ContentProvider {

    private final static int PRODUCTS = 100;
    private final static int PRODUCT_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCT, PRODUCTS);

        sUriMatcher.addURI(ProductContract.CONTENT_AUTHORITY, ProductContract.PATH_PRODUCT + "/#",
                PRODUCT_ID);
    }

    private ProductDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new ProductDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        Cursor cursor;
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, null);
                break;
            case PRODUCT_ID:
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(ProductEntry.TABLE_NAME, projection,
                        ProductEntry._ID + "=?", selectionArgs, null, null, null);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown Uri " + uri.toString());
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertProduct(Uri uri, ContentValues contentValues) {
        String name = contentValues.getAsString(ProductEntry.NAME);
        if (name == null) {
            throw new IllegalArgumentException("No name value added to product.");
        }

        Integer price = contentValues.getAsInteger(ProductEntry.PRICE);
        if (price != null && price < 0){
            throw new IllegalArgumentException("No valid price added to product.");
        }

        Integer quantity = contentValues.getAsInteger(ProductEntry.QUANTITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("No valid quantity added to product.");
        }

        String supplierPhone = contentValues.getAsString(ProductEntry.SUPPLIER_PHONE);
        if (supplierPhone == null) {
            throw new IllegalArgumentException("No supplier added to product.");
        }

        byte[] image = contentValues.getAsByteArray(ProductEntry.IMAGE);
        if (image.length == 0) {
            throw new IllegalArgumentException("No image added to product.");
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        long newProductID = database.insert(ProductEntry.TABLE_NAME, null, contentValues);

        if (newProductID == -1) {
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, newProductID);
    }


    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int rowsAffected;

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                rowsAffected = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsAffected = database.delete(ProductEntry.TABLE_NAME, ProductEntry._ID + "=?", selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }

        if (rowsAffected != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsAffected;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection,
                      @Nullable String[] selectionArgs) {

        int match = sUriMatcher.match(uri);
        switch (match) {
            // Update the full list of products based on the specified selection
            case PRODUCTS:
                return updateProduct(uri, contentValues, selection, selectionArgs);
            // Update certain rows of the table based on selected IDs (or commonly single ID).
            case PRODUCT_ID:
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateProduct(uri, contentValues, ProductEntry._ID + "=?", selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updateProduct(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        if (contentValues.size() == 0) {
            return 0;
        }

        if (contentValues.containsKey(ProductEntry.NAME)) {
            String name = contentValues.getAsString(ProductEntry.NAME);
            if (name == null) {
                throw new IllegalArgumentException("No name value added to product.");
            }
        }

        if (contentValues.containsKey(ProductEntry.PRICE)) {
            Integer price = contentValues.getAsInteger(ProductEntry.PRICE);
            if (price != null && price < 0){
                throw new IllegalArgumentException("No valid price added to product.");
            }
        }

        if (contentValues.containsKey(ProductEntry.QUANTITY)) {
            Integer quantity = contentValues.getAsInteger(ProductEntry.QUANTITY);
            if (quantity != null && quantity < 0) {
                throw new IllegalArgumentException("No valid quantity added to product.");
            }
        }

        if (contentValues.containsKey(ProductEntry.SUPPLIER_PHONE)) {
            String supplierPhone = contentValues.getAsString(ProductEntry.SUPPLIER_PHONE);
            if (supplierPhone == null) {
                throw new IllegalArgumentException("No supplier added to product.");
            }
        }

        if (contentValues.containsKey(ProductEntry.IMAGE)) {
            byte[] image = contentValues.getAsByteArray(ProductEntry.IMAGE);
            if (image.length == 0) {
                throw new IllegalArgumentException("No image added to product.");
            }
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        int rowsAffected = database.update(
                ProductEntry.TABLE_NAME,
                contentValues,
                selection,
                selectionArgs
        );

        if (rowsAffected != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsAffected;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}

