package com.art4l.license;

/**
 * Calculation of the license key for devices
 * This should go into a library and to be used by the calculator and the smartpick flowengine.
 * 
 *
 */

import java.security.NoSuchAlgorithmException;

public class LicenseValidator {


    static public String getLicenseKey(String seedValue){
        String hexValue = getHexKey(seedValue);
        if (hexValue != null) return serialNumber(hexValue);

        return "";
    }


    static private String calculateSecurityHash(String stringInput, String algorithmName)
            throws NoSuchAlgorithmException {
        String hexMessageEncode = "";
        byte[] buffer = stringInput.getBytes();
        java.security.MessageDigest messageDigest =
                java.security.MessageDigest.getInstance(algorithmName);
        messageDigest.update(buffer);
        byte[] messageDigestBytes = messageDigest.digest();
        for (int index=0; index < messageDigestBytes.length ; index ++) {
            int countEncode = messageDigestBytes[index] & 0xff;
            if (Integer.toHexString(countEncode).length() == 1)
                hexMessageEncode = hexMessageEncode + "0";
            hexMessageEncode = hexMessageEncode + Integer.toHexString(countEncode);
        }
        return hexMessageEncode;
    }


    static private String getHexKey(String seedValue){

        String serialNumberEncoded = null;

        try {
            serialNumberEncoded = calculateSecurityHash(seedValue, "SHA-256") +
                    calculateSecurityHash(seedValue, "MD5") +
                    calculateSecurityHash(seedValue, "SHA-1");

        } catch (NoSuchAlgorithmException e){

        }
        return serialNumberEncoded;


    }

    static private String serialNumber(String serialNumberEncoded){
        String serialNumber = ""
                + serialNumberEncoded.charAt(32)  + serialNumberEncoded.charAt(76)
                + serialNumberEncoded.charAt(100) + serialNumberEncoded.charAt(50) + "-"
                + serialNumberEncoded.charAt(2)   + serialNumberEncoded.charAt(91)
                + serialNumberEncoded.charAt(73)  + serialNumberEncoded.charAt(72)
                + serialNumberEncoded.charAt(98)  + "-"
                + serialNumberEncoded.charAt(47)  + serialNumberEncoded.charAt(65)
                + serialNumberEncoded.charAt(18)  + serialNumberEncoded.charAt(85) + "-"
                + serialNumberEncoded.charAt(27)  + serialNumberEncoded.charAt(53)
                + serialNumberEncoded.charAt(102) + serialNumberEncoded.charAt(15)
                + serialNumberEncoded.charAt(99);

        return serialNumber;

    }
}
