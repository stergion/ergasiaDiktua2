package com.stergiosnanos.userapplication;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.exit;

/* TODO Clean up code */

public class UserApplication {

  private static EchoClient echoClient;
  private static ImageClient imageClient;
  private static AudioClient audioClient;
  private static OBDClient obdClient;
  private static IthakicopterClient ithakicopter;
  private static final Path workingDir = Paths.get(System.getProperty("user.dir"));


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
    try {
      echoClient = new EchoClient(echoCode, hostName, hostPort, localPort);
    } catch (UnknownHostException | SocketException e) {
      System.err.println("EchoClient: " + e.getMessage());
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

      ArrayList<String> obdPyArgs = new ArrayList<>(List.of(echoClient.getCode(), "-wdir", workingDir.toAbsolutePath().toString()));
      runPythontoolsModule("echo.py", obdPyArgs);

      System.out.println("trying echoTime without delay...");
      echoClient.echoTime(5 * 60, true);
      System.out.println("echoTime completed!");

      ArrayList<String> echoPyArgs = new ArrayList<>(List.of(echoClient.getCode(), "-wdir", workingDir.toAbsolutePath().toString()));
      runPythontoolsModule("echo.py", echoPyArgs);
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


    // ============================ Image Client ==========================================
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

    imageClient.closeSocket();

    // ============================ Audio Client ==========================================
    String audioFile;
    try {
      audioClient = new AudioClient(audioCode, hostName, hostPort, localPort);
    } catch (UnknownHostException | SocketException e) {
      e.printStackTrace();
      exit(-1);
    }

    audioClientStart("AQ-DPCM");
    audioClientStart("DPCM");
    audioClientStart("freq");

    audioClient.closeSocket();


    // ============================ IthakicopterClient Client ==========================================
    try {
      ithakicopter = new IthakicopterClient(ithakicopterCode, hostName, hostPort);
    } catch (UnknownHostException | SocketException e) {
      e.printStackTrace();
      exit(-1);
    }
    countdown("Starting Ithakicopter telemetry in...", 5);
    String ifp1 = ithakicopterTelemetry("iCopter01_telemetry");

    countdown("Starting Ithakicopter telemetry in...", 10);
    String ifp2 = ithakicopterTelemetry("iCopter02_telemetry");

    ithakicopter.closeSocket();

    ArrayList<String> ithakiPyArgs = new ArrayList<>(List.of(ithakicopter.getCode(), ifp1, ifp2, "-wdir", workingDir.toAbsolutePath().toString()));
//    System.out.println(ithakiPyArgs);
    runPythontoolsModule("icopter.py", ithakiPyArgs);

    System.out.println("Ithakicopter finished!\n");
    // ============================ OBD Client ==========================================
    System.out.println("Starting OBD.");

    try {
      obdClient = new OBDClient(obdCode, hostName, hostPort, localPort);
    } catch (SocketException | UnknownHostException e) {
      e.printStackTrace();
    }

    String ofp = obdClient.saveTelemetryAsCSV("telemetry", 4 * 60);
    ArrayList<String> obdPyArgs = new ArrayList<>(List.of(obdClient.getCode(), ofp, "-wdir", workingDir.toAbsolutePath().toString()));
    runPythontoolsModule("obd.py", obdPyArgs);

    obdClient.closeSocket();
    System.out.println("OBD finished!\n");

  }

  private static void audioClientStart(String audioType) {
    String audioFile, audioFile1, audioFile2;
    ArrayList<String> args = new ArrayList<>();
    args.add(audioType);
    args.add(audioClient.getCode());

    try {
      switch (audioType) {
        case "AQ-DPCM" -> {
          audioFile1 = audioClient.saveTrack(2, 30, Codec.AQDPCM, "");
          args.add(audioFile1.replace(".wav", ""));
          audioFile2 = audioClient.saveTrack(5, 30, Codec.AQDPCM, "");
          args.add("-fp2");
          args.add(audioFile2.replace(".wav", ""));
        }
        case "DPCM" -> {
          audioFile = audioClient.saveTrack(2, 30, Codec.DPCM, "");
          args.add(audioFile.replace(".wav", ""));
        }
        case "freq" -> {
          audioFile = audioClient.saveFrequencies(30, "");
          args.add(audioFile.replace(".wav", ""));
        }
        default -> {
          System.err.println(audioType + "is not a valid type");
          return;
        }
      }
      args.add("-wdir");
      args.add(workingDir.toAbsolutePath().toString());
      runPythontoolsModule("audio.py", args);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void runPythontoolsModule(String moduleName, List<String> args) {
    Path modulePath = null;

    switch (moduleName) {
      case "audio.py" -> modulePath = Paths.get("src", "pythontools", "audio.py");
      case "echo.py" -> modulePath = Paths.get("src", "pythontools", "echo.py");
      case "icopter.py" -> modulePath = Paths.get("src", "pythontools", "icopter.py");
      case "obd.py" -> modulePath = Paths.get("src", "pythontools", "obd.py");
      default -> System.err.println("There is no python module named '" + moduleName + ".py'");
    }

    assert modulePath != null;

    ArrayList<String> command = new ArrayList<>();
    command.add("python");
    command.add(modulePath.toAbsolutePath().toString());
    command.addAll(args);
    ProcessBuilder processBuilder = new ProcessBuilder(command).inheritIO();

//    System.out.println(processBuilder.command());
    try {
      System.out.println(modulePath.toAbsolutePath().toString() + "...");
      Process pythonProcess = processBuilder.start();
      System.out.println("Process finished with exit code " + pythonProcess.waitFor());
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }

//    Path pythontoolsPath = Paths.get(wdir.to, "src", "pythontools");
  }

  private static void countdown(String msg, int sec) {
    try {
      System.out.println(msg);
      for (int i = sec; i > 0; i--) {
        System.out.println(i + "...");
        Thread.sleep(1000);
      }
      System.out.println("Starting...");
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static String ithakicopterTelemetry(String fileName) {
    String filepath = "";
    // Start ithakicopter.jar
    Path ithakiJAR = Paths.get("JARs", "ithakicopter.jar");
//    System.out.println(ithakiJAR.toAbsolutePath());
    System.out.println(workingDir.relativize(ithakiJAR.toAbsolutePath()).toString());
    ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", ithakiJAR.toAbsolutePath().toString());
//    System.out.println("Working Directory = " + workingDir);
    processBuilder.directory(new File(workingDir.toAbsolutePath().toString()));

    Process processIthakicopterJAR = null;
    try {
      processIthakicopterJAR = processBuilder.start();
      filepath = ithakicopter.saveTelemetryAsCSV(fileName, 30);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Close ithakicopter.jar
    if (processIthakicopterJAR != null) {
      processIthakicopterJAR.destroy();
    }
    return filepath;
  }
}
