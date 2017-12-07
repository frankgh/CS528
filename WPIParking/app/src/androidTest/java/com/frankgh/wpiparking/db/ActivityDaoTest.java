package com.frankgh.wpiparking.db;

/**
 * @author Francisco Guerrero <email>me@frankgh.com</email> on 12/6/17.
 */

import android.arch.persistence.room.Room;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.frankgh.wpiparking.db.dao.ActivityDao;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * Test the implementation of {@link ActivityDao}
 */
@RunWith(AndroidJUnit4.class)
public class ActivityDaoTest {

//    @Rule
//    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase mDatabase;

    private ActivityDao mActivityDao;

    @Before
    public void initDb() throws Exception {
        // using an in-memory database because the information stored here disappears when the
        // process is killed
        mDatabase = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
                AppDatabase.class)
                // allowing main thread queries, just for testing
                .allowMainThreadQueries()
                .build();

        mActivityDao = mDatabase.activityDao();
    }

    @After
    public void closeDb() throws Exception {
        mDatabase.close();
    }
}
