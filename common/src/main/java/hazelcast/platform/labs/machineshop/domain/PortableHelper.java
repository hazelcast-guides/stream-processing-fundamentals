package hazelcast.platform.labs.machineshop.domain;

import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;

/*
 * This class provides utilities related to Portable serialization.  This module (common) is meant to
 * be included on the server side, so it cannot contain domain objects.
 */
public class PortableHelper {
    public static final int MACHINE_SHOP_PORTABLE_FACTORY_ID = 1;
    public static final int MACHINE_STATUS_ID = 3;
    public static int MACHINE_PROFILE_ID = 1;

    public static ClassDefinition MACHINE_STATUS_CLASS_DEFINITION =
            new ClassDefinitionBuilder(MACHINE_SHOP_PORTABLE_FACTORY_ID, MACHINE_STATUS_ID)
                    .addStringField("serialNumber")
                    .addShortField("averageBitTemp10s")
                    .addLongField("eventTime")
                    .build();


}
