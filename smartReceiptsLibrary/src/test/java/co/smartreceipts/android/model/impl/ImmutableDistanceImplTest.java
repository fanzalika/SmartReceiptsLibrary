package co.smartreceipts.android.model.impl;

import android.os.Parcel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.TimeZone;

import co.smartreceipts.android.DefaultObjects;
import co.smartreceipts.android.model.Distance;
import co.smartreceipts.android.model.Trip;
import co.smartreceipts.android.model.WBCurrency;
import co.smartreceipts.android.sync.model.SyncState;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class ImmutableDistanceImplTest {

    private static final double EPSILON = 1d / Distance.RATE_PRECISION;

    private static final int ID = 5;
    private static final String LOCATION = "Location";
    private static final BigDecimal DISTANCE = new BigDecimal(12.55d);
    private static final BigDecimal RATE = new BigDecimal(0.33d);
    private static final Date DATE = new Date(1409703721000L);
    private static final WBCurrency CURRENCY = WBCurrency.getInstance("USD");
    private static final TimeZone TIMEZONE = TimeZone.getDefault();
    private static final String COMMENT = "Comment";

    // Class under test
    ImmutableDistanceImpl mDistance;

    Trip mTrip;

    SyncState mSyncState;

    @Before
    public void setUp() throws Exception {
        mTrip = DefaultObjects.newDefaultTrip();
        mSyncState = DefaultObjects.newDefaultSyncState();
        mDistance = new ImmutableDistanceImpl(ID, mTrip, LOCATION, DISTANCE, RATE, CURRENCY, DATE, TIMEZONE, COMMENT, mSyncState);
    }

    @Test
    public void getId() {
        assertEquals(ID, mDistance.getId());
    }

    @Test
    public void getTrip() {
        assertEquals(mTrip, mDistance.getTrip());
    }

    @Test
    public void getLocation() {
        assertEquals(LOCATION, mDistance.getLocation());
    }

    @Test
    public void getDistance() {
        assertEquals(DISTANCE.doubleValue(), mDistance.getDistance().doubleValue(), EPSILON);
    }

    @Test
    public void getDecimalFormattedDistance() {
        assertEquals("12.55", mDistance.getDecimalFormattedDistance());
    }

    @Test
    public void getDate() {
        assertEquals(DATE, mDistance.getDate());
    }

    @Test
    public void getTimeZone() {
        assertEquals(TIMEZONE, mDistance.getTimeZone());
    }

    @Test
    public void getRate() {
        assertEquals(RATE.doubleValue(), mDistance.getRate().doubleValue(), EPSILON);
    }

    @Test
    public void getDecimalFormattedRate() {
        assertEquals("0.330",mDistance.getDecimalFormattedRate());
    }

    @Test
    public void getCurrencyFormattedRate() {
        assertEquals("$0.33",mDistance.getCurrencyFormattedRate());
    }

    @Test
    public void getComment() {
        assertEquals(COMMENT, mDistance.getComment());
    }

    @Test
    public void getSyncState() {
        Assert.assertEquals(mSyncState, mDistance.getSyncState());
    }

    @Test
    public void compareTo() {
        assertTrue(mDistance.compareTo(new ImmutableDistanceImpl(ID, mTrip, LOCATION, DISTANCE, RATE, CURRENCY, DATE, TIMEZONE, COMMENT, mSyncState)) == 0);
        assertTrue(mDistance.compareTo(new ImmutableDistanceImpl(ID, mTrip, LOCATION, DISTANCE, RATE, CURRENCY, new Date(DATE.getTime()*2), TIMEZONE, COMMENT, mSyncState)) > 0);
        assertTrue(mDistance.compareTo(new ImmutableDistanceImpl(ID, mTrip, LOCATION, DISTANCE, RATE, CURRENCY, new Date(0), TIMEZONE, COMMENT, mSyncState)) < 0);
    }

    @Test
    public void equals() {
        Assert.assertEquals(mDistance, mDistance);
        Assert.assertEquals(mDistance, new ImmutableDistanceImpl(ID, mTrip, LOCATION, DISTANCE, RATE, CURRENCY, DATE, TIMEZONE, COMMENT, mSyncState));
        assertThat(mDistance, not(equalTo(new Object())));
        assertThat(mDistance, not(equalTo(mock(Distance.class))));
        assertThat(mDistance, not(equalTo(new ImmutableDistanceImpl(-1, mTrip, LOCATION, DISTANCE, RATE, CURRENCY, DATE, TIMEZONE, COMMENT, mSyncState))));
        assertThat(mDistance, not(equalTo(new ImmutableDistanceImpl(ID, mock(Trip.class), LOCATION, DISTANCE, RATE, CURRENCY, DATE, TIMEZONE, COMMENT, mSyncState))));
        assertThat(mDistance, not(equalTo(new ImmutableDistanceImpl(ID, mTrip, "bad", DISTANCE, RATE, CURRENCY, DATE, TIMEZONE, COMMENT, mSyncState))));
        assertThat(mDistance, not(equalTo(new ImmutableDistanceImpl(ID, mTrip, LOCATION, new BigDecimal(0), RATE, CURRENCY, DATE, TIMEZONE, COMMENT, mSyncState))));
        assertThat(mDistance, not(equalTo(new ImmutableDistanceImpl(ID, mTrip, LOCATION, DISTANCE, new BigDecimal(0), CURRENCY, DATE, TIMEZONE, COMMENT, mSyncState))));
        assertThat(mDistance, not(equalTo(new ImmutableDistanceImpl(ID, mTrip, LOCATION, DISTANCE, RATE, WBCurrency.MISSING_CURRENCY, DATE, TIMEZONE, COMMENT, mSyncState))));
        assertThat(mDistance, not(equalTo(new ImmutableDistanceImpl(ID, mTrip, LOCATION, DISTANCE, RATE, CURRENCY, new Date(System.currentTimeMillis()), TIMEZONE, COMMENT, mSyncState))));
        assertThat(mDistance, not(equalTo(new ImmutableDistanceImpl(ID, mTrip, LOCATION, DISTANCE, RATE, CURRENCY, DATE, TIMEZONE, "bad", mSyncState))));
    }

    @Test
    public void parcelEquality() {
        final Parcel parcel = Parcel.obtain();
        mDistance.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        final ImmutableDistanceImpl distance = ImmutableDistanceImpl.CREATOR.createFromParcel(parcel);
        assertNotNull(distance);
        assertEquals(distance, mDistance);
    }

}