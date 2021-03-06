package co.smartreceipts.android.persistence.database.controllers.alterations;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.model.Trip;
import co.smartreceipts.android.model.factory.BuilderFactory1;
import co.smartreceipts.android.model.factory.ReceiptBuilderFactory;
import co.smartreceipts.android.model.factory.ReceiptBuilderFactoryFactory;
import co.smartreceipts.android.persistence.database.operations.DatabaseOperationMetadata;
import co.smartreceipts.android.persistence.database.tables.ReceiptsTable;
import co.smartreceipts.android.utils.FileUtils;
import co.smartreceipts.android.utils.log.Logger;
import co.smartreceipts.android.utils.UriUtils;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import wb.android.storage.StorageManager;

public class ReceiptTableActionAlterations extends StubTableActionAlterations<Receipt> {


    private final ReceiptsTable mReceiptsTable;
    private final StorageManager mStorageManager;
    private final BuilderFactory1<Receipt, ReceiptBuilderFactory> mReceiptBuilderFactoryFactory;

    public ReceiptTableActionAlterations(@NonNull ReceiptsTable receiptsTable, @NonNull StorageManager storageManager) {
        mReceiptsTable = Preconditions.checkNotNull(receiptsTable);
        mStorageManager = Preconditions.checkNotNull(storageManager);
        mReceiptBuilderFactoryFactory = new ReceiptBuilderFactoryFactory();
    }

    ReceiptTableActionAlterations(@NonNull ReceiptsTable receiptsTable, @NonNull StorageManager storageManager, @Nullable BuilderFactory1<Receipt, ReceiptBuilderFactory> receiptBuilderFactoryFactory) {
        mReceiptsTable = Preconditions.checkNotNull(receiptsTable);
        mStorageManager = Preconditions.checkNotNull(storageManager);
        mReceiptBuilderFactoryFactory = Preconditions.checkNotNull(receiptBuilderFactoryFactory);
    }

    @NonNull
    @Override
    public Observable<Receipt> preInsert(@NonNull final Receipt receipt) {
        return Observable.defer(new Func0<Observable<Receipt>>() {
            @Override
            public Observable<Receipt> call() {
                return Observable.just(updateReceiptFileNameBlocking(mReceiptBuilderFactoryFactory.build(receipt).setIndex(getNextReceiptIndex(receipt)).build()));
            }
        });
    }

    @NonNull
    @Override
    public Observable<Receipt> preUpdate(@NonNull final Receipt oldReceipt, @NonNull final Receipt newReceipt) {
        return Observable.create(new Observable.OnSubscribe<Receipt>() {
            @Override
            public void call(Subscriber<? super Receipt> subscriber) {
                if (newReceipt.getFile() != null && !newReceipt.getFile().equals(oldReceipt.getFile())) {
                    // If we changed the receipt file, rename it to our naming schema
                    if (oldReceipt.getFile() != null) {
                        final ReceiptBuilderFactory factory = mReceiptBuilderFactoryFactory.build(newReceipt);
                        final String oldExtension = "." + StorageManager.getExtension(oldReceipt.getFile());
                        final String newExtension = "." + StorageManager.getExtension(newReceipt.getFile());
                        if (newExtension.equals(oldExtension)) {
                            if (newReceipt.getFile().renameTo(oldReceipt.getFile())) {
                                // Note: Keep 'oldReceipt' here, since File is immutable (and renamedTo doesn't change it)
                                factory.setFile(oldReceipt.getFile());
                            }
                        } else {
                            final String renamedNewFileName = oldReceipt.getFile().getName().replace(oldExtension, newExtension);
                            final String renamedNewFilePath = newReceipt.getFile().getAbsolutePath().replace(newReceipt.getFile().getName(), renamedNewFileName);
                            final File renamedNewFile = new File(renamedNewFilePath);
                            if (newReceipt.getFile().renameTo(renamedNewFile)) {
                                factory.setFile(renamedNewFile);
                            }
                        }
                        subscriber.onNext(factory.build());
                        subscriber.onCompleted();
                    } else {
                        subscriber.onNext(updateReceiptFileNameBlocking(newReceipt));
                        subscriber.onCompleted();
                    }
                } else {
                    subscriber.onNext(newReceipt);
                    subscriber.onCompleted();
                }
                newReceipt.getFile();
            }
        });

    }

    @Override
    public void postUpdate(@NonNull Receipt oldReceipt, @Nullable Receipt newReceipt) throws Exception {
        if (newReceipt != null) { // i.e. - the update succeeded
            if (oldReceipt.hasFile() && newReceipt.hasFile() && newReceipt.getFile() != null && !newReceipt.getFile().equals(oldReceipt.getFile())) {
                // Only delete the old file if we have a new one now...
                mStorageManager.delete(oldReceipt.getFile());
            }
        }
    }

    @Override
    public void postDelete(@Nullable Receipt receipt) throws Exception {
        if (receipt != null && receipt.hasFile()) {
            mStorageManager.delete(receipt.getFile());
        }
    }

    @NonNull
    public Observable<Receipt> preCopy(@NonNull final Receipt receipt, @NonNull final Trip toTrip) {
        return Observable.defer(new Func0<Observable<Receipt>>() {
            @Override
            public Observable<Receipt> call() {
                try {
                    return Observable.just(copyReceiptFileBlocking(receipt, toTrip));
                } catch (final IOException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public void postCopy(@NonNull Receipt oldReceipt, @Nullable Receipt newReceipt) throws Exception {
        // Intentional no-op
    }

    @NonNull
    public Observable<Receipt> preMove(@NonNull final Receipt receipt, @NonNull final Trip toTrip) {
        // Move = Copy + Delete
        return preCopy(receipt, toTrip);
    }

    public void postMove(@NonNull Receipt oldReceipt, @Nullable Receipt newReceipt) throws Exception {
        if (newReceipt != null) { // i.e. - the move succeeded (delete the old data)
            Logger.info(this, "Completed the move procedure");
            if (mReceiptsTable.delete(oldReceipt, new DatabaseOperationMetadata()).toBlocking().first() != null) {
                if (oldReceipt.hasFile()) {
                    if (!mStorageManager.delete(oldReceipt.getFile())) {
                        Logger.error(this, "Failed to delete the moved receipt's file");
                    }
                }
            } else {
                Logger.error(this, "Failed to delete the moved receipt from the database for it's original trip");
            }
        }
    }

    @NonNull
    public Observable<List<? extends Map.Entry<Receipt, Receipt>>> getReceiptsToSwapUp(@NonNull Receipt receiptToSwapUp, @NonNull List<Receipt> receipts) {
        final int indexToSwapWith = receipts.indexOf(receiptToSwapUp) - 1;
        if (indexToSwapWith < 0) {
            return Observable.error(new RuntimeException("This receipt is at the start of the list already"));
        } else {
            final Receipt swappingWith = receipts.get(indexToSwapWith);
            return Observable.<List<? extends Map.Entry<Receipt, Receipt>>>just(swapDates(receiptToSwapUp, swappingWith));
        }
    }

    @NonNull
    public Observable<List<? extends Map.Entry<Receipt, Receipt>>> getReceiptsToSwapDown(@NonNull Receipt receiptToSwapDown, @NonNull List<Receipt> receipts) {
        final int indexToSwapWith = receipts.indexOf(receiptToSwapDown) + 1;
        if (indexToSwapWith > (receipts.size() - 1)) {
            return Observable.error(new RuntimeException("This receipt is at the end of the list already"));
        } else {
            final Receipt swappingWith = receipts.get(indexToSwapWith);
            return Observable.<List<? extends Map.Entry<Receipt, Receipt>>>just(swapDates(receiptToSwapDown, swappingWith));
        }
    }

    @NonNull
    private Receipt updateReceiptFileNameBlocking(@NonNull Receipt receipt) {
        final ReceiptBuilderFactory builder = mReceiptBuilderFactoryFactory.build(receipt);

        final StringBuilder stringBuilder = new StringBuilder(receipt.getIndex() + "_");
        stringBuilder.append(FileUtils.omitIllegalCharactersFromFileName(receipt.getName().trim()));
        final File file = receipt.getFile();
        if (file != null) {
            stringBuilder.append('.').append(StorageManager.getExtension(receipt.getFile()));
            final String newName = stringBuilder.toString();
            final File renamedFile = mStorageManager.getFile(receipt.getTrip().getDirectory(), newName);
            if (!renamedFile.exists()) {
                Logger.info(this, "Changing image name from: {} to: {}", file.getName(), newName);
                builder.setFile(mStorageManager.rename(file, newName)); // Returns oldFile on failure
            }
        }

        return builder.build();
    }

    @NonNull
    private Receipt copyReceiptFileBlocking(@NonNull Receipt receipt, @NonNull Trip toTrip) throws IOException {
        final ReceiptBuilderFactory builder = mReceiptBuilderFactoryFactory.build(receipt);
        builder.setTrip(toTrip);
        if (receipt.hasFile()) {
            final File destination = mStorageManager.getFile(toTrip.getDirectory(), System.currentTimeMillis() + receipt.getFileName());
            if (mStorageManager.copy(receipt.getFile(), destination, true)) {
                Logger.info(this, "Successfully copied the receipt file to the new trip: {}", toTrip.getName());
                builder.setFile(destination);
            } else {
                throw new IOException("Failed to copy the receipt file to the new trip: " + toTrip.getName());
            }
        }
        builder.setIndex(getNextReceiptIndex(receipt));
        return updateReceiptFileNameBlocking(builder.build());
    }

    @NonNull
    private List<? extends Map.Entry<Receipt, Receipt>> swapDates(@NonNull Receipt receipt1, @NonNull Receipt receipt2) {
        final ReceiptBuilderFactory builder1 = mReceiptBuilderFactoryFactory.build(receipt1);
        final ReceiptBuilderFactory builder2 = mReceiptBuilderFactoryFactory.build(receipt2);
        builder1.setDate(receipt2.getDate());
        builder2.setDate(receipt1.getDate());
        return Arrays.asList(new AbstractMap.SimpleImmutableEntry<>(receipt1, builder1.build()), new AbstractMap.SimpleImmutableEntry<>(receipt2, builder2.build()));
    }

    private int getNextReceiptIndex(@NonNull Receipt receipt) {
        return mReceiptsTable.get(receipt.getTrip()).toBlocking().first().size() + 1;
    }
}
