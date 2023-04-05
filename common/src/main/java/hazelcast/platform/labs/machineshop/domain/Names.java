package hazelcast.platform.labs.machineshop.domain;

import com.hazelcast.config.EventJournalConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;

import java.io.Serializable;

public class Names {

    public static final String PROFILE_MAP_NAME = "machine_profiles";

    public static final String EVENT_MAP_NAME = "machine_events";

    public static final String CONTROLS_MAP_NAME = "machine_controls";

    public static final String SYSTEM_ACTIVITIES_MAP_NAME = "system_activities";

    public static final String STATUS_SUMMARY_MAP_NAME = "machine_status_summary";

    public static class ProfileMapConfigurationTask implements Runnable, HazelcastInstanceAware, Serializable {

        private transient HazelcastInstance hz;
        @Override
        public void run() {
            hz.getConfig().addMapConfig(new MapConfig(PROFILE_MAP_NAME)
                    .setInMemoryFormat(InMemoryFormat.BINARY)
                    .setBackupCount(1));
        }

        @Override
        public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
            this.hz = hazelcastInstance;
        }
    }

    public static class EventMapConfigurationTask implements Runnable, HazelcastInstanceAware, Serializable {

        private transient HazelcastInstance hz;
        @Override
        public void run() {
            hz.getConfig().addMapConfig(new MapConfig(EVENT_MAP_NAME)
                    .setInMemoryFormat(InMemoryFormat.BINARY)
                    .setBackupCount(1)
                    .setEventJournalConfig(
                            new EventJournalConfig()
                                    .setEnabled(true)
                                    .setCapacity(3000000)
                                    .setTimeToLiveSeconds(0)
                    )
            );
        }

        @Override
        public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
            this.hz = hazelcastInstance;
        }
    }

    public static class RequestMapConfigurationTask implements Runnable, HazelcastInstanceAware, Serializable {

        private transient HazelcastInstance hz;
        @Override
        public void run() {
            hz.getConfig().addMapConfig(new MapConfig("*_request")
                    .setInMemoryFormat(InMemoryFormat.BINARY)
                    .setBackupCount(1)
                    .setEventJournalConfig(
                            new EventJournalConfig()
                                    .setEnabled(true)
                                    .setCapacity(3000000)
                                    .setTimeToLiveSeconds(0)
                    )
            );
        }

        @Override
        public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
            this.hz = hazelcastInstance;
        }
    }
}
