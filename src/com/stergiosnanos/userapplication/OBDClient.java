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
        System.err.println("Trying to send " + obd.toString().toUpperCase() + "packet");
      }

      try {
        socket.receive(packetRcv);
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println("While trying to receive " + obd.toString().toUpperCase() + " packet");
      }

      stringList.add(obd.name());
      stringList.add("=");
      stringList.add(Integer.toString(parseResponse(obd, packetRcv)));
      stringList.add(obd.getUnits());
//      if (obd == OBDRequest.ENGINE_TIME) stringList.forEach(System.out::println);
    }
    return stringList;
  }

  private int parseResponse(OBDRequest request, DatagramPacket packetRcv) {
    String[] strings = new String(packetRcv.getData(), packetRcv.getOffset(), packetRcv.getLength()).split(" ");

    final int radix = 16;
    return switch (request) {
      case ENGINE_TIME -> 256 * Integer.parseInt(strings[2], radix) + Integer.parseInt(strings[3], radix);
      case AIR_TEMP -> Integer.parseInt(strings[2], radix) - 40;
      case THROTTLE_POS -> Integer.parseInt(strings[2], radix) * 100 / 255;
      case ENGINE_RPM -> (Integer.parseInt(strings[2], radix) * 256 + Integer.parseInt(strings[3], radix)) / 4;
      case VEHICLE_SPEED -> Integer.parseInt(strings[2], radix);
      case COOLANT_TEMP -> Integer.parseInt(strings[2], radix) - 40;
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

  public String saveTelemetryAsCSV(String fileName, int duration) {
    int startTime;
    String[] telemetry;
    String[] csvLine = new String[6];
    String filePath = null;

    try {
      Files.createDirectories(Paths.get(directory));
      String fName = getFileName(fileName);
      filePath = directory + "/" + fName + ".csv";
      File file = new File(filePath);

      try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
        writer.writeNext(new String[]{
                OBDRequest.ENGINE_TIME.name() + " (" + OBDRequest.ENGINE_TIME.getUnits().strip() + ")",
                OBDRequest.AIR_TEMP.name() + " (" + OBDRequest.AIR_TEMP.getUnits().strip() + ")",
                OBDRequest.THROTTLE_POS.name() + " (" + OBDRequest.THROTTLE_POS.getUnits().strip() + ")",
                OBDRequest.ENGINE_RPM.name() + " (" + OBDRequest.ENGINE_RPM.getUnits().strip() + ")",
                OBDRequest.VEHICLE_SPEED.name() + " (" + OBDRequest.VEHICLE_SPEED.getUnits().strip() + ")",
                OBDRequest.COOLANT_TEMP.name() + " (" + OBDRequest.COOLANT_TEMP.getUnits().strip() + ")"});

        do {
          telemetry = getTelemetry().toArray(new String[0]);
        } while (Integer.parseInt(telemetry[4]) + duration > 900);

        csvLine[0] = telemetry[4];
        csvLine[1] = telemetry[8];
        csvLine[2] = telemetry[12];
        csvLine[3] = telemetry[16];
        csvLine[4] = telemetry[20];
        csvLine[5] = telemetry[24];
        writer.writeNext(csvLine);

        startTime = Integer.parseInt(telemetry[4]);
        do {
          telemetry = getTelemetry().toArray(new String[0]);

          csvLine[0] = telemetry[4];
          csvLine[1] = telemetry[8];
          csvLine[2] = telemetry[12];
          csvLine[3] = telemetry[16];
          csvLine[4] = telemetry[20];
          csvLine[5] = telemetry[24];
          writer.writeNext(csvLine);

        } while (Integer.parseInt(telemetry[4]) - startTime < duration);
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

  public String getCode() {
    return obdCode;
  }
}
