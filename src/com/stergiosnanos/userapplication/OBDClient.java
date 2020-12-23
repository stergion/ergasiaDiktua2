package com.stergiosnanos.userapplication;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    socket.setSoTimeout(5000);    // set timeout 2 sec

    this.hostPort = hostPort;
    this.localPort = localPort;
    this.buffer = new byte[128];
  }

  public String getTelemetry() {
    StringBuilder stringBuilder = new StringBuilder().append("OBDCLIENT\t").append(LocalDateTime.now().withNano(0));

    DatagramPacket packetSend;
    DatagramPacket packetRcv = new DatagramPacket(buffer, buffer.length);

    for (OBDRequest obd : OBDRequest.values()) {
      try {
        String request = obdCode + obd.getRequest();
        packetSend = new DatagramPacket(request.getBytes(), request.getBytes().length, address, hostPort);
        socket.send(packetSend);
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println("Trying to send " + obd.toString().toLowerCase() + "paket");
      }

      try {
        socket.receive(packetRcv);
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println(obd.toString());
        System.err.println("While trying to receive " + obd.toString().toLowerCase() + " paket");
      }

      String[] strings = new String(packetRcv.getData(), packetRcv.getOffset(), packetRcv.getLength()).substring(6).split(" ");
       stringBuilder.append("\t\t").append(obd.name()).append(" = ").append(getResponse(obd, strings)).append(obd.getUnits());
    }
    return stringBuilder.toString();
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

  public void saveTelemetry(int sec) {
    long startTime, duration;

    try {
      Files.createDirectories(Paths.get(directory));
      String fileName = getFileName("telemetry");

      try (FileOutputStream outputStream = new FileOutputStream(directory + "/" +  fileName + ".txt")) {
        startTime = currentTimeMillis();

        do {
          outputStream.write((getTelemetry() + lineSeparator()).getBytes());
          duration = currentTimeMillis() - startTime;
        } while (duration < sec * 1000L);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
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
