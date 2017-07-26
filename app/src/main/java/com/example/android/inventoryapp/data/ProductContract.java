package com.example.android.inventoryapp.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;


public final class ProductContract {

    final static String CONTENT_AUTHORITY = "com.example.android.inventoryapp";

    final static Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    static final String PATH_PRODUCT = "productions";

    // Empty constructor in order to prevent someone to instantiate this class
    private ProductContract() {}

    /**
     * Inner class that defines constant values for Products database table.
     * Each entry in the table represents a single product.
     */
    public static final class ProductEntry implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCT);

        /**
         * list of products .
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCT;

        /**
         * single Product row.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" +
                        PATH_PRODUCT + "/" + PATH_PRODUCT;

        public static final String TABLE_NAME = "Products";
        public final static String _ID = BaseColumns._ID;
        public final static String NAME = "name";
        public final static String PRICE = "price";
        public final static String QUANTITY = "quantity";
        public final static String SUPPLIER_PHONE = "supplier_phone";
        public final static String IMAGE = "image";
    }
}
