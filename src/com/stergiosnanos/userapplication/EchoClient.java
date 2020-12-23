package com.stergiosnanos.userapplication;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;


public class EchoClient {
  private final static String echoFast = "E0000";
  private final InetAddress address;
  private final DatagramSocket socket;
  private final int hostPort;
  private final int localPort;
  private final String echoDelay;
  private String echoCode;
  private byte[] buffer;
  private String directory;

  public EchoClient(String echoCode, String host, int hostPort) throws UnknownHostException, SocketException {
    this(echoCode, host, hostPort, 0);
  }

  public EchoClient(String echoCode, String host, int hostPort, int localPort) throws UnknownHostException, SocketException {
    this.echoDelay = echoCode;
    setEchoDelay();
    this.address = InetAddress.getByName(host);

    if (localPort == 0) {
      this.socket = new DatagramSocket();
    } else {
      this.socket = new DatagramSocket(localPort);
    }
    socket.setSoTimeout(5000);    // set timeout 5 sec

    this.localPort = localPort;
    this.hostPort = hostPort;
    this.buffer = new byte[128];
    directory = "echoClient";
  }

  private String echo(String options) throws IOException {
    String request = echoCode + options;
//    System.out.println("Echo request is: " + request);
    DatagramPacket packet = new DatagramPacket(request.getBytes(), request.getBytes().length, address, hostPort);
    socket.send(packet);

    DatagramPacket q = new DatagramPacket(buffer, buffer.length);
    socket.receive(q);

//    System.out.println("printing packet");
    return new String(q.getData(), q.getOffset(), q.getLength());
  }

  public String echo() throws IOException {
    return echo("");
  }

  public String echoTemps(int station) throws IOException {
    return echo(String.format("T%02d", station));
  }

  public void echoTime(int sec, boolean isFast) throws IOException {
    long loopStart, timerStart, timerStop;
    long usec = sec * 1000L;
    String fileName;
    int numPackets = 0;
    ArrayList<Long> latencies = new ArrayList<>(sec * 10);
    ArrayList<Long> times = new ArrayList<>(sec * 10);

    if (isFast) {
      setEchoFast();
      fileName = "echoFast";
    } else {
      setEchoDelay();
      fileName = "echoDelay";
    }
    System.out.printf("echo %s fast%n", isEchoFast()?"IS":"is NOT");


    loopStart = System.currentTimeMillis();
    do {
      timerStart = System.currentTimeMillis();
      echo();
      timerStop = System.currentTimeMillis();

      latencies.add(timerStop - timerStart);
      times.add(timerStop - loopStart);
      numPackets++;
    } while (System.currentTimeMillis() - loopStart < usec);

    saveAsCSV(fileName, latencies, times);
    System.out.println("Echo Packets Received: " + numPackets);
  }

/*
  public void echoTimeFast(int sec) throws IOException {
    long timeStart = System.currentTimeMillis();
    long usec = sec * 1000L;
    int i = 0;

    do {
      System.out.println("EchoClient: " + i++ + " - " + echo("E0000"));
    } while (System.currentTimeMillis() - timeStart < usec);
  }
*/

  private void saveAsCSV(String fileName, ArrayList<Long> latencies, ArrayList<Long> times) throws IOException {
    Files.createDirectories(Paths.get(directory));
    File file = new File(directory + "/" + fileName + ".csv");

    try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
      writer.writeNext(new String[]{"latencies", "times"});

      for (int i = 0; i < latencies.size(); i++) {
        writer.writeNext(new String[]{latencies.get(i).toString(), times.get(i).toString()});
      }
    }
  }


  public void closeSocket() {
    socket.close();
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }

  public void setEchoFast() {
    this.echoCode = echoFast;
  }

  public void setEchoDelay() {
    this.echoCode = echoDelay;
  }

  public boolean isEchoFast() {
    return echoCode.equals(echoFast);
  }
}
