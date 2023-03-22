package hazelcast.platform.labs.machineshop.domain;

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableFactory;

public class MachineShopPortableFactory implements PortableFactory {
    public static final int ID = 1;
    @Override
    public Portable create(int classId) {
        if (classId == MachineProfile.ID)
            return new MachineProfile();
        else if (classId == MachineStatusEvent.ID)
            return new MachineStatusEvent();
        else if (classId == MachineStatusSummary.ID)
            return new MachineStatusSummary();
        else if (classId == StatusServiceResponse.ID)
            return new StatusServiceResponse();
        else
            return null;
    }
}
