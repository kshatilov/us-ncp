package com.shatilov.us.ncp.dca.utils;

public class Utils {
    public static byte[] getStreamCmd() {
        byte command_data = (byte) 0x01;
        byte payload_data = (byte) 3;
        byte emg_mode = (byte) 0x02;
        byte imu_mode = (byte) 0x00;
        byte class_mode = (byte) 0x00;

        return new byte[]{command_data, payload_data, emg_mode, imu_mode, class_mode};
    }
}
