package com.stergiosnanos.userapplication;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

  public void saveTelemetry(int sec) {
    long startTime, duration;

    try {
      Files.createDirectories(Paths.get(directory));
      String fileName = getFileName("telemetry");

      try (FileOutputStream outputStream = new FileOutputStream(directory + fileName + ".txt")) {
        startTime = currentTimeMillis();

        do {
//          System.out.println("Ithakicopter: Packet received!");
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
    return "/" + String.join("_",dateTime.format(DateTimeFormatter.BASIC_ISO_DATE),
                                            "T" + dateTime.format(DateTimeFormatter.ofPattern("HHmm")),
                                            name.toUpperCase(Locale.ROOT), ithakicopterCode);
  }

  public void closeSocket() {
    socket.close();
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
