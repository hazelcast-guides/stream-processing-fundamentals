package hazelcast.platform.labs.machineshop.domain;

import com.hazelcast.nio.serialization.*;

import java.io.IOException;

public class StatusServiceResponse implements Portable {
    private String serialNumber;
    private Short  averageBitTemp10s;
    private Short  warningTemp;
    private Short criticalTemp;
    private String status;

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Short getAverageBitTemp10s() {
        return averageBitTemp10s;
    }

    public void setAverageBitTemp10s(Short averageBitTemp10s) {
        this.averageBitTemp10s = averageBitTemp10s;
    }

    public Short getWarningTemp() {
        return warningTemp;
    }

    public void setWarningTemp(Short warningTemp) {
        this.warningTemp = warningTemp;
    }

    public Short getCriticalTemp() {
        return criticalTemp;
    }

    public void setCriticalTemp(Short criticalTemp) {
        this.criticalTemp = criticalTemp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public int getFactoryId() {
        return MachineShopPortableFactory.ID;
    }

    public static final int ID = 5;

    @Override
    public int getClassId() {
        return StatusServiceResponse.ID;
    }

    @Override
    public void writePortable(PortableWriter portableWriter) throws IOException {
        portableWriter.writeString("serialNumber", this.serialNumber);
        portableWriter.writeShort("averageBitTemp10s", this.averageBitTemp10s);
        portableWriter.writeShort("warningTemp", this.warningTemp);
        portableWriter.writeShort("criticalTemp", this.criticalTemp);
        portableWriter.writeString("status", this.status);
    }

    @Override
    public void readPortable(PortableReader portableReader) throws IOException {
        this.serialNumber = portableReader.readString("serialNumber");
        this.averageBitTemp10s = portableReader.readShort("averageBitTemp10s");
        this.warningTemp = portableReader.readShort("warningTemp");
        this.criticalTemp = portableReader.readShort("criticalTemp");
        this.status = portableReader.readString("status");
    }

    public static ClassDefinition CLASS_DEFINITION =
            new ClassDefinitionBuilder(MachineShopPortableFactory.ID, StatusServiceResponse.ID)
                    .addStringField("serialNumber")
                    .addShortField("averageBitTemp10s")
                    .addShortField("warningTemp")
                    .addShortField("criticalTemp")
                    .addStringField("status").build();

    @Override
    public String toString() {
        return "StatusServiceResponse{" +
                "serialNumber='" + serialNumber + '\'' +
                ", averageBitTemp10s=" + averageBitTemp10s +
                ", warningTemp=" + warningTemp +
                ", criticalTemp=" + criticalTemp +
                ", status='" + status + '\'' +
                '}';
    }
}
