package com.stergiosnanos.userapplication;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import static java.lang.System.exit;

/* TODO Clean up code */

public class UserApplication {
  public static void main(String[] args) {

    String hostName = "155.207.18.208";
    int localPort = Integer.parseInt(args[0]);
    int hostPort = Integer.parseInt(args[1]);
    String echoCode = args[2];
    String imageCode = args[3];
    String audioCode = args[4];
    String ithakicopterCode = args[5];
    String obdCode = args[6];


    // ============================ Echo Client ==========================================
    EchoClient echoClient = null;
    try {
      echoClient = new EchoClient(echoCode, hostName, hostPort, localPort);
    } catch (UnknownHostException | SocketException e) {
      System.out.println("EchoClient: " + e.getMessage());
      e.printStackTrace();
      exit(-1);
    }

    try {
      System.out.println("trying echo...");
      System.out.println("echoClient: " + echoClient.echo());
      System.out.println("echo completed!");
    } catch (IOException e) {
      System.err.println("echoClient: " + e.getMessage());
      e.printStackTrace();
    }

    try {
      System.out.println("trying echoTime with delay...");
      echoClient.echoTime(5 * 60, false);

      System.out.println("trying echoTime without delay...");
      echoClient.echoTime(5 * 60, true);
      System.out.println("echoTime completed!");
    } catch (IOException e) {
      System.err.println("echoClient.echoTime: " + e.getMessage());
      e.printStackTrace();
    }

    try {
      System.out.println("trying echoTemps...");
      System.out.println("echoTemps: " + echoClient.echoTemps(0));
      System.out.println("echoTemps: " + echoClient.echoTemps(1));
      System.out.println("echoTemps: " + echoClient.echoTemps(2));
      System.out.println("echoTemps: " + echoClient.echoTemps(3));
      System.out.println("echoTemps: " + echoClient.echoTemps(4));
      System.out.println("echoTemps: " + echoClient.echoTemps(5));
      System.out.println("echoTemps: " + echoClient.echoTemps(6));
      System.out.println("echoTemps: " + echoClient.echoTemps(7));
      System.out.println("echoTemps: " + echoClient.echoTemps(8));
      System.out.println("echoTemps: " + "echoTemps completed!");
    } catch (IOException e) {
      System.err.println("echoClient.echoTemps: " + e.getMessage());
      e.printStackTrace();
    }

    echoClient.closeSocket();
    exit(0);

    // ============================ Image Client ==========================================
    ImageClient imageClient = null;
    try {
      imageClient = new ImageClient(imageCode, hostName, hostPort, localPort);
    } catch (UnknownHostException | SocketException e) {
      System.err.println("ImageClient: " + e.getMessage());
      e.printStackTrace();
      exit(-1);
    }

    try {
      int packetsReceived = imageClient.getImage("UDP=128CAM=FIX", "imageFIX128_E1.jpeg");
      System.out.println("Image Client: imageFIX128_E1.jpeg: packets received = " + packetsReceived);

      packetsReceived = imageClient.getImage("UDP=254CAM=FIX", "imageFIX254_E1.jpeg");
      System.out.println("Image Client: imageFIX254_E1.jpeg: packets received = " + packetsReceived);

      packetsReceived = imageClient.getImage("UDP=512CAM=FIX", "imageFIX512_E1.jpeg");
      System.out.println("Image Client: imageFIX512_E1.jpeg: packets received = " + packetsReceived);

      packetsReceived = imageClient.getImage("UDP=1024CAM=FIX", "imageFIX1024_E1.jpeg");
      System.out.println("Image Client: imageFIX1024_E1.jpeg: packets received = " + packetsReceived);
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      int packetsReceived = imageClient.getImage("UDP=1024DIR=CCAM=PTZ", "imagePTZ_E2.jpeg");
      System.out.println("Image Client: imagePTZ_E2.jpeg: packets received = " + packetsReceived);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // ============================ Audio Client ==========================================
    AudioClient audioClient = null;
    try {
      audioClient = new AudioClient(audioCode, hostName, hostPort, localPort);
    } catch (UnknownHostException | SocketException e) {
      e.printStackTrace();
      exit(-1);
    }


    try {
      audioClient.saveFrequencies(30, "");
      audioClient.saveTrack(2, 30, Codec.DPCM,"");
      audioClient.saveTrack(2, 30, Codec.AQDPCM, "");
    } catch (IOException e) {
      e.printStackTrace();
    }

    exit(0);


    /*

    // ============================ OBD Client ==========================================
    System.out.println("Starting OBD.");

    try {
      OBDClient obdClient = new OBDClient(obdCode, hostName, hostPort, localPort);
      obdClient.saveTelemetry(180);
    } catch (SocketException e) {
      e.printStackTrace();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    System.out.println("OBD finished!\n");

    // ============================ IthakicopterClient Client ==========================================
    System.out.println("Starting Ithakicopter.");

    IthakicopterClient ithakicopter = null;
    try {
      ithakicopter = new IthakicopterClient(ithakicopterCode, hostName, hostPort);
      ithakicopter.saveTelemetry(20);
    } catch (UnknownHostException | SocketException e) {
      e.printStackTrace();
      exit(-1);
    }

    System.out.println("Ithakicopter finished!\n");

    exit(0);
*/
  }


}
