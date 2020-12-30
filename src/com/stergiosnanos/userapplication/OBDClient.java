package com.stergiosnanos.userapplication;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.lineSeparator;

public class OBDClient {
  private final String obdCode;
  private final InetAddress address;
  private final DatagramSocket socket;
  private final int hostPort;
  private final int localPort;
  private final byte[] buffer;
  private String directory = "./obdClient";

  OBDClient(String obdCode, String host, int hostPort, int localPort) throws UnknownHostException, SocketException {
    this.obdCode = obdCode;
    this.address = InetAddress.getByName(host);

    if (localPort == 0) {
      this.socket = new DatagramSocket();
    } else {
      this.socket = new DatagramSocket(localPort);
    }
    socket.setSoTimeout(10000);    // set timeout 2 sec

    this.hostPort = hostPort;
    this.localPort = localPort;
    this.buffer = new byte[128];
  }

  /* returns a List<string> containing the telemetry data.
   * [0: OBDCLIENT], [1: time/date]
   * [position: name]:     2: ENGINE_TIME,       6: AIR_TEMP,        10: THROTTLE_POS,       14: ENGINE_RPM,       18: VEHICLE_SPEED,        22: COOLANT_TEMP
   * [position: value]:    4: ENGINE_TIME value, 8: AIR_TEMP value,  12: THROTTLE_POS value, 16: ENGINE_RPM value, 20: VEHICLE_SPEED value,  24: COOLANT_TEMP value
   * [position: units]:    5:  sec,              9:  °C,             13:  %,                 17:  RPM,             21:  Km/h,                25:  °C
   * between the name and the value there is an "=" sign (pos: 3, 7, 11, 15, 19, 23)
   *
   * Example (formatted to string):  OBDCLIENT	 2020-12-29T18:48:13	 ENGINE_TIME = 14  sec AIR_TEMP = 12  °C THROTTLE_POS = 17  % ENGINE_RPM = 0  RPM VEHICLE_SPEED = 0  Km/h COOLANT_TEMP = 11  °C
   * */
  public List<String> getTelemetry() {
//    StringBuilder stringBuilder = new StringBuilder().append("OBDCLIENT\t").append(LocalDateTime.now().withNano(0));
    List<String> stringList = new ArrayList<>(10);
    stringList.add("OBDCLIENT\t");
    stringList.add(LocalDateTime.now().withNano(0).toString() + "\t");

    DatagramPacket packetSend;
    DatagramPacket packetRcv = new DatagramPacket(buffer, buffer.length);

    for (OBDRequest obd : OBDRequest.values()) {
      try {
        String request = obdCode + obd.getRequest();
        packetSend = new DatagramPacket(request.getBytes(), request.getBytes().length, address, hostPort);
        socket.send(packetSend);
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println("Trying to send " + obd.toString().toUpperCase() + "paket");
      }

      try {
        socket.receive(packetRcv);
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println(obd.toString());
        System.err.println("While trying to receive " + obd.toString().toUpperCase() + " paket");
      }

      String[] strings = new String(packetRcv.getData(), packetRcv.getOffset(), packetRcv.getLength()).substring(6).split(" ");
//      stringBuilder.append("\t\t").append(obd.name()).append(" = ").append(getResponse(obd, strings)).append(obd.getUnits());
      stringList.add(obd.name());
      stringList.add("=");
      stringList.add(Integer.toString(getResponse(obd, strings)));
      stringList.add(obd.getUnits());
    }
//    return stringBuilder.toString();
    return stringList;
  }

  public int getResponse(OBDRequest request, String[] s) {
    final int radix = 16;
    return switch (request) {
      case ENGINE_TIME -> 256 * Integer.parseInt(s[0], radix) + Integer.parseInt(s[1], radix);
      case AIR_TEMP -> Integer.parseInt(s[0], radix) - 40;
      case THROTTLE_POS -> Integer.parseInt(s[0], radix) * 100 / 255;
      case ENGINE_RPM -> (Integer.parseInt(s[0], radix) * 256 + Integer.parseInt(s[1], radix)) / 4;
      case VEHICLE_SPEED -> Integer.parseInt(s[0], radix);
      case COOLANT_TEMP -> Integer.parseInt(s[0], radix) - 40;
    };
  }

  public String saveTelemetry(int sec) {
    long startTime, duration;
    String filePath = null;

    try {
      Files.createDirectories(Paths.get(directory));
      String fileName = getFileName("telemetry");
      filePath = directory + "/" + fileName + ".txt";

      try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
        startTime = currentTimeMillis();

        do {
          outputStream.write((String.join(" ", getTelemetry()) + lineSeparator()).getBytes());
          duration = currentTimeMillis() - startTime;
        } while (duration < sec * 1000L);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return filePath;
  }

  public String saveTelemetryAsCSV(int sec) {
    long startTime, duration;
    List<String[]> csvAllLines = new ArrayList<>(50);
    String[] telemetry;
    String[] csvLine = new String[6];
    String filePath = null;

    try {
      Files.createDirectories(Paths.get(directory));
      String fileName = getFileName("telemetry");
      filePath = directory + "/" + fileName + ".csv";
      File file = new File(filePath);

      try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
        writer.writeNext(new String[]{
                OBDRequest.ENGINE_TIME.name(),
                OBDRequest.AIR_TEMP.name(),
                OBDRequest.THROTTLE_POS.name(),
                OBDRequest.ENGINE_RPM.name(),
                OBDRequest.VEHICLE_SPEED.name(),
                OBDRequest.COOLANT_TEMP.name()});

        startTime = currentTimeMillis();
        do {
          telemetry = getTelemetry().toArray(new String[0]);
//          System.out.println(String.join(" ", telemetry));
          csvLine[0] = telemetry[4];
          csvLine[1] = telemetry[8];
          csvLine[2] = telemetry[12];
          csvLine[3] = telemetry[16];
          csvLine[4] = telemetry[20];
          csvLine[5] = telemetry[24];
          writer.writeNext(csvLine);
//          csvAllLines.add(csvLine);
          duration = currentTimeMillis() - startTime;
        } while (duration < sec * 1000L);

//        writer.writeAll(csvAllLines);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return filePath;
  }


  private String getFileName(String name) {
    var dateTime = LocalDateTime.now();
    return String.join("_", dateTime.format(DateTimeFormatter.BASIC_ISO_DATE),
            "T" + dateTime.format(DateTimeFormatter.ofPattern("HHmm")),
            name.toUpperCase(Locale.ROOT), obdCode);
  }

  public void closeSocket() {
    socket.close();
  }
}
