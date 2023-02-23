package hazelcast.platform.labs.machineshop.domain;

import com.hazelcast.config.EventJournalConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;

public class Names {

    public static final String PROFILE_MAP_NAME = "machine_profiles";

    public static final String EVENT_MAP_NAME = "machine_events";

    public static final String CONTROLS_MAP_NAME = "machine_controls";

    public static final String SYSTEM_ACTIVITIES_MAP_NAME = "system_activities";

    // These MapConfigs are used with Viridian.  They are sent via executor to the cluster.
    public static final MapConfig PROFILE_MAP_CONFIG =
            new MapConfig(PROFILE_MAP_NAME).setInMemoryFormat(InMemoryFormat.BINARY).setBackupCount(1);

    public static final MapConfig EVENT_MAP_CONFIG =
            new MapConfig(EVENT_MAP_NAME).setInMemoryFormat(InMemoryFormat.BINARY)
                    .setBackupCount(1)
                    .setEventJournalConfig( new EventJournalConfig()
                            .setEnabled(true)
                            .setCapacity(3000000)
                            .setTimeToLiveSeconds(0)
                    );
}
