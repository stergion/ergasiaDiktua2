package com.stergiosnanos.userapplication;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;

public class ImageClient {
  private final String imageCode;
  private final InetAddress address;
  private final DatagramSocket socket;
  private final int hostPort;
  private final int localPort;
  private byte[] buffer;
  private final String folder;

  public ImageClient(String echoCode, String host, int hostPort) throws UnknownHostException, SocketException {
    this(echoCode, host, hostPort, 0);
  }

  public ImageClient(String echoCode, String host, int hostPort, int localPort) throws UnknownHostException, SocketException {
    this.imageCode = echoCode;
    this.address = InetAddress.getByName(host);

    if (localPort == 0) {
      this.socket = new DatagramSocket();
    } else {
      this.socket = new DatagramSocket(localPort);
    }
    socket.setReuseAddress(true);
    this.localPort = localPort;
    this.hostPort = hostPort;
    this.buffer = new byte[10000];
    this.folder = "./images/";
  }

  public int getImage(String options, String fileName) throws IOException {
    int packetLength;
    int packetsReceived = 0;
    String request = imageCode + options;
    DatagramPacket packet = new DatagramPacket(request.getBytes(), request.getBytes().length, address, hostPort);

    socket.send(packet);

    packet = new DatagramPacket(buffer, buffer.length);
    try (FileOutputStream outputStream = new FileOutputStream(folder + fileName)) {
      socket.receive(packet);
      packetsReceived++;
      packetLength = packet.getLength();
      outputStream.write(packet.getData(), packet.getOffset(), packet.getLength());
      do {
        socket.receive(packet);
        packetsReceived++;
        outputStream.write(packet.getData(), packet.getOffset(), packet.getLength());
      } while (packet.getLength() == packetLength);
    }

    return packetsReceived;
  }

  public void closeSocket() {
    socket.close();
  }

}
