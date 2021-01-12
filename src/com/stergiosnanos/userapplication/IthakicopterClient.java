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

public class IthakicopterClient {
  private final String ithakicopterCode;
  private final InetAddress address;
  private final DatagramSocket socket;
  private final int ithakicopterPort = 48078;
  private final int hostPort;
  private final byte[] buffer;
  private final String directory = "./ithakicopter";

  IthakicopterClient(String ithakicopterCode, String host, int hostPort) throws UnknownHostException, SocketException {
    this.ithakicopterCode = ithakicopterCode;
    this.address = InetAddress.getByName(host);

    socket = new DatagramSocket(ithakicopterPort);
    socket.setSoTimeout(5000);    // set timeout 5 sec

    this.hostPort = hostPort;
    this.buffer = new byte[1024];
  }

  public String getTelemetry() throws IOException {
    DatagramPacket packetRcv = new DatagramPacket(buffer, buffer.length);
    socket.receive(packetRcv);

    return new String(packetRcv.getData(), packetRcv.getOffset(), packetRcv.getLength());
  }

  /* Return String[] with of length=6 containing telemetry data
   * String[time, LMOTOR, RMOTOR, ALTITUDE, TEMPERATURE, PRESSURE]
   * */
  public String[] parseTelemetry(String telemetry) {
    List<String> parsedTelemetry = new ArrayList<>(6);
    String[] strings = telemetry.split(" ");

    parsedTelemetry.add(strings[2]);
    for (int i = 3; i < strings.length - 1; i++) {
      parsedTelemetry.add(strings[i].split("=")[1]);
    }

    return parsedTelemetry.toArray(new String[0]);
  }

  public void saveTelemetry(int sec) {
    long startTime, duration;

    try {
      Files.createDirectories(Paths.get(directory));
      String fileName = getFileName("telemetry");

      try (FileOutputStream outputStream = new FileOutputStream(directory + fileName + ".txt")) {
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

  //  todo saveTelemetryAsCSV
  public String saveTelemetryAsCSV(String fileName, int sec) {
    long startTime, duration;
    String filePath = null;

    try {
      Files.createDirectories(Paths.get(directory));
      filePath = directory + "/" + fileName + ".csv";
      File file = new File(filePath);

      try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
        writer.writeNext(new String[]{
                "time",
                "LMOTOR (duty-cycle)",
                "RMOTOR (duty-cycle)",
                "ALTITUDE (pixels)",
                "TEMPERATURE (C)",
                "PRESSURE (mbar)"});

        startTime = currentTimeMillis();
        do {
          writer.writeNext(parseTelemetry(getTelemetry()));
          duration = currentTimeMillis() - startTime;
        } while (duration < sec * 1000L);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return filePath;
  }

  private String getFileName(String name) {
    var dateTime = LocalDateTime.now();
    return "/" + String.join("_", dateTime.format(DateTimeFormatter.BASIC_ISO_DATE),
            "T" + dateTime.format(DateTimeFormatter.ofPattern("HHmm")),
            name.toUpperCase(Locale.ROOT), ithakicopterCode);
  }

  public void closeSocket() {
    socket.close();
  }

  public String getCode() {
    return ithakicopterCode;
  }

/*
  private void setMotorSpeed(int speed) {
    String speedRequest = "AUTO FLIGHTLEVEL=100 LMOTOR=" + speed + " RMOTOR=" + speed + " PILOT\r";

    try {
      OutputStream outputStream = motorControllerSocket.getOutputStream();
      System.out.println(speedRequest);
      outputStream.write(speedRequest.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
*/

/*
  public synchronized void startMotorController() {
    int speed;

    try {
      motorControllerSocket = new Socket(address, 38048);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in));) {
      while (true) {
        System.out.println("You can enter a speed [0, 250] or press 'q' to exit.");

        while (true) {
          String input = br.readLine().strip();

          if(input.equals("Q") || input.equals("q")) {
            return;
          } else {
            try {
              speed = Integer.parseInt(input);

              if (speed >= 0 && speed < 251) {
                setMotorSpeed(speed);
                System.out.println("New motor speed is: " + speed);
                wait(3000);
                break;
              }


              System.err.println("Invalid input");
            } catch(NumberFormatException nfe) {
              System.err.println("Invalid Format!");
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
*/
}
