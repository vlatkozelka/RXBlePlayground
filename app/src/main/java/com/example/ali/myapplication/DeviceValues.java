package com.example.ali.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by User on 1/13/2017.
 */

public class DeviceValues implements Parcelable {


    private String baseDataString;
    private double thermocoupleTemperature;
    private double tCAmbientTemperature;
    private double ambientTemperature;
    private boolean isThermocouplePlugged;
    private boolean isChargerPlugged;
    private double batteryVoltage;
    private double infraredTemperature;
    private boolean isBatteryLow;

    public DeviceValues(byte[] data){
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            int i = 0;

            for (byte byteChar : data) {
                int i2 = data[i] & 0xFF; //unint8

                //infrared temp
                double temp = 0;
                if (stringBuilder.length() > 6) {
                    //converts signed byte to unsigned integer
                    int G7G0 = data[6] & 0xFF;
                    int H7H0 = data[7] & 0xFF;
                    temp = (float) G7G0;
                    temp = temp * 256.0;
                    temp += (float) H7H0;
                    temp = temp * 0.02;
                    temp -= 273.15;
                    setInfraredTemperature(temp);


                }
                //IRAmbientTemperature
                double tamb = 0;
                if (stringBuilder.length() > 8) {
                    int I7I0 = data[8] & 0xFF;
                    int J7J0 = data[9] & 0xFF;
                    tamb = ((float) I7I0 * 256.0 + (float) J7J0) * 0.02 - 273.15;


                }
               /* if (Prefs.getInfraredCorrRelativeToAmbientEnabled(context) == 1) {
                    if (temp < 25 && isDeviceShitty()) {
                        double infraredToAmbientFactor = Double.parseDouble(Prefs.getAmbientCorrectionFactor(context));
                        deviceValues.setInfraredTemperature(infraredToAmbientFactor * (temp - tamb) + temp);
                    }
                }*/


                //voltage and charger
                if (stringBuilder.length() > 4) {
                    int bit8 = 0b10000000;
                    int bit7 = 0b01000000;
                    int first4bits = 0b00001111;
                    int unsigneddata4 = data[4] & 0xFF;//unint8
                    int unsigneddata5 = data[5] & 0xFF;
                    // let E6: UInt8 = values[4] & 0b01000000
                    //int E6 = unsigneddata4 & bit7;
                    //int E7 = unsigneddata4 & bit8;
                    int E6 = data[4] & bit7;
                    int E7 = data[4] & bit8;
                    int F7F0 = unsigneddata5;
                    int E3E0 = unsigneddata4 & first4bits;
                    boolean isBatteryLow = E6 != 0;
                    boolean isChargerPlugged = E7 != 0;
                    double batteryVoltage = (((float) E3E0 * 256.0 + (float) F7F0) / 2047.0 * 1.532) / 0.33;
                    setBatteryVoltage(batteryVoltage);
                    setChargerPlugged(isChargerPlugged);
                    setBatteryLow(isBatteryLow);

                }
                //Tc Ambient Temperature
                if (stringBuilder.length() > 2) {
                    int bit1 = 0b00000001;
                    int bit8 = 0b10000000;
                    int bits8to4 = 0b11110000;
                    int unsigneddata2 = data[2] & 0xFF;
                    int unsigneddata3 = data[3] & 0xFF;
                    int D0 = unsigneddata3 & bit1;
                    int D7D4 = unsigneddata3 & bits8to4;
                    int C7C0 = unsigneddata2;
                    int C7 = unsigneddata2 & bit8;
                    boolean isThermocouplePlugged = (D0 == 0);
                    setThermocouplePlugged(isThermocouplePlugged);
                    int intermediate = ((C7C0) * 256 + D7D4);

                    if (C7 != 0) {
                        //Compliment of intermedite
                        intermediate = (~intermediate) + 1;
                    }

                    double ambiantTemp = ((float) intermediate / 16.0) * 0.0625;
                    if (C7 != 0) {
                        ambiantTemp = -ambiantTemp;
                    }


                }
                if (stringBuilder.length() > 0) {
                    int bit8 = 0b10000000;
                    int bits8to2 = 0b11111100;
                    int unsigneddata1 = data[1] & 0xFF;
                    int unsigned16data1 = data[1] & 0xFFFF;
                    int B7B2 = unsigned16data1 & bits8to2;//unint16
                    // int cst = 256 & 0xFFFF;

                    int unsigneddata0 = data[0] & 0xFF;
                    int A7A0 = data[0] & 0xFFFF;//unint16
                    int A7 = unsigneddata0 & bit8;

                    int intermediate = ((A7A0) * 256) + B7B2;

                    if (A7 != 0) {
                        intermediate = (~intermediate) + 1;
                    }
                    double thermocoupleTemperature = (float) intermediate / 16.0;
                    if (A7 != 0) {
                        thermocoupleTemperature = -thermocoupleTemperature;
                    }
                    setThermocoupleTemperature(thermocoupleTemperature);
                    //Polynomial correction
                    /*if (Prefs.isProbeCorrectionEnabled(context)) {
                        String polyCorrString = Prefs.getProbeCorrectionPoly(context);
                        //remove spaces
                        String polyCorrEscaped = polyCorrString.replaceAll("\\s+", "");
                        String[] separatedPolyCorr = polyCorrEscaped.split(";");
                        double cor = 0;
                        double power = 1;
                        double[] polyCorr = new double[separatedPolyCorr.length];
                        for (int k = 0; k <= separatedPolyCorr.length - 1; k++) {
                            polyCorr[k] = Double.parseDouble(separatedPolyCorr[k]);
                            cor += polyCorr[k] * power;
                            power *= thermocoupleTemperature;
                        }
                        deviceValues.setThermocoupleTemperature(cor);
                    }*/


                }
                stringBuilder.append(String.format("%02X ", byteChar));
                i++;
            }
        }
    }

    public double getThermocoupleTemperature() {
        return thermocoupleTemperature;
    }

    public void setThermocoupleTemperature(double thermocoupleTemperature) {
        this.thermocoupleTemperature = thermocoupleTemperature;
    }

    public boolean isThermocouplePlugged() {
        return isThermocouplePlugged;
    }

    public void setThermocouplePlugged(boolean thermocouplePlugged) {
        isThermocouplePlugged = thermocouplePlugged;
    }

    public boolean isChargerPlugged() {
        return isChargerPlugged;
    }

    public void setChargerPlugged(boolean chargerPlugged) {
        isChargerPlugged = chargerPlugged;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public double getInfraredTemperature() {
        return infraredTemperature;
    }

    public void setInfraredTemperature(double infraredTemperature) {
        this.infraredTemperature = infraredTemperature;
    }

    public String getBaseDataString() {
        return baseDataString;
    }

    public void setBaseDataString(String baseDataString) {
        this.baseDataString = baseDataString;
    }

    public double gettCAmbientTemperature() {
        return tCAmbientTemperature;
    }

    public void settCAmbientTemperature(double tCAmbientTemperature) {
        this.tCAmbientTemperature = tCAmbientTemperature;
    }

    public double getAmbientTemperature() {
        return ambientTemperature;
    }

    public void setAmbientTemperature(double ambientTemperature) {
        this.ambientTemperature = ambientTemperature;
    }

    public boolean isBatteryLow() {
        return isBatteryLow;
    }

    public void setBatteryLow(boolean batteryLow) {
        isBatteryLow = batteryLow;
    }

    public DeviceValues() {}

    protected DeviceValues(Parcel in) {

        baseDataString = in.readString();
        thermocoupleTemperature = in.readDouble();
        tCAmbientTemperature = in.readDouble();
        ambientTemperature = in.readDouble();
        isThermocouplePlugged = in.readInt() == 1;
        isChargerPlugged = in.readInt() == 1;
        batteryVoltage = in.readDouble();
        infraredTemperature = in.readDouble();
        isBatteryLow = in.readInt() == 1;

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(baseDataString);
        dest.writeDouble(thermocoupleTemperature);
        dest.writeDouble(tCAmbientTemperature);
        dest.writeDouble(ambientTemperature);
        dest.writeInt(isThermocouplePlugged ? 1 : 0);
        dest.writeInt(isChargerPlugged ? 1 : 0);
        dest.writeDouble(batteryVoltage);
        dest.writeDouble(infraredTemperature);
        dest.writeInt(isBatteryLow ? 1 : 0);


    }

    @SuppressWarnings("unused")
    public static final Creator<DeviceValues> CREATOR = new Creator<DeviceValues>() {
        @Override
        public DeviceValues createFromParcel(Parcel in) {
            return new DeviceValues(in);
        }

        @Override
        public DeviceValues[] newArray(int size) {
            return new DeviceValues[size];
        }
    };

}