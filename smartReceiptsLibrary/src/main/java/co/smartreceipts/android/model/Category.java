package co.smartreceipts.android.model;

import android.os.Parcelable;
import android.support.annotation.NonNull;

import co.smartreceipts.android.sync.model.Syncable;

/**
 * A contract definition by which we can track receipt categories
 */
public interface Category extends Parcelable, Comparable<Category>, Syncable {

    /**
     * @return the full-name representation of this category
     */
    @NonNull
    String getName();

    /**
     * @return the "code" associated with this category
     */
    @NonNull
    String getCode();
}
