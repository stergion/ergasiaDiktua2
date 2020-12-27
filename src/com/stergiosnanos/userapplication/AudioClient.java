package com.stergiosnanos.userapplication;

import com.opencsv.CSVWriter;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioClient {
  AtomicInteger min = new AtomicInteger(0);
  AtomicInteger max = new AtomicInteger(0);
  private final InetAddress address;
  private final DatagramSocket socket;
  private final int hostPort;
  private final int localPort;
  private final String audioCode;
  private final byte[] buffer;
  private int packetNum;    // audio packets to be received
  private Codec codec;
  private ByteBuffer audioBuffer;
  private int sampleRate;
  private String directory;

  // Arrays for saving diffs, mu, beta of audio
  private int[] diffsArray = null;
  private int[] musArray = null;
  private int[] betasArray = null;
  private int packetIndex = 0;

//  TODO CLEAN UP CODE

  AudioClient(String audioCode, String host, int hostPort, int localPort) throws UnknownHostException, SocketException {
    this.audioCode = audioCode;
    this.address = InetAddress.getByName(host);

    if (localPort == 0) {
      socket = new DatagramSocket();
    } else {
      socket = new DatagramSocket(localPort);
    }
    socket.setSoTimeout(5000);    // set timeout 5 sec

    this.hostPort = hostPort;
    this.localPort = localPort;
    this.packetNum = 32 * 30;      // 32 packets played at 8000/sec is 1 sec of audio playback
    this.codec = Codec.DPCM;
    this.buffer = new byte[256];
    this.sampleRate = 8000;
    directory = "audioClient";
  }

  private byte[] decodeDCPM(byte[] data, int length, AtomicInteger lastNibble) {
    int beta = 1;
    int n1, n2;
    int d1, d2;
    int D1, D2;

    byte[] decoded = new byte[length * 2];

    for (int i = 0; i < length; i++) {
      d2 = data[i] & 0xF;
      d1 = (data[i] >> 4) & 0xF;

      d2 = d2 - 8;
      d1 = d1 - 8;

      D2 = d2 * beta;
      D1 = d1 * beta;
      diffsArray[packetIndex*128*2 + i*2 ] = D1;
      diffsArray[packetIndex*128*2 + i*2 + 1] = D2;

      n1 = D1 + lastNibble.get();
      // clip value
      if (n1 > 127) {
        n1 = 127;
      } else if (n1 < -128) {
        n1 = -128;
      }

      n2 = D2 + n1;
      // clip value
      if (n2 > 127) {
        n2 = 127;
      } else if (n2 < -128) {
        n2 = -128;
      }
      lastNibble.set(n2);

      decoded[2 * i] = (byte) n1;
      decoded[2 * i + 1] = (byte) n2;

      if (n1 > max.get() ) max.set(n1);
      if (n2 > max.get() ) max.set(n2);
      if (n1 < min.get() ) min.set(n1);
      if (n2 < min.get() ) min.set(n2);
    }

    return decoded;
  }

  private byte[] decodeAQ_DCPM(byte[] data, int length, AtomicInteger lastNibble) {
    int mu, beta;
    int n1, n2;
    int d1, d2;
    int D1, D2;

    // First 4 bytes are the header of the data
    int offset = 4;

    byte[] arr = new byte[2];
    arr[0] = data[1];
    arr[1] = data[0];
    mu = ByteBuffer.wrap(arr).getShort(); // big-endian by default
    arr[0] = data[3];
    arr[1] = data[2];
    beta = ByteBuffer.wrap(arr).getShort(); // big-endian by default

    musArray[packetIndex] = mu;
    betasArray[packetIndex] = beta;

    byte[] decoded = new byte[(length - offset) * 2 * codec.getQ() / 8];

    for (int i = offset; i < length; i++) {
      d2 = data[i] & 0xF;
      d1 = (data[i] >> 4) & 0xF;

      d2 = d2 - 8;
      d1 = d1 - 8;

      D2 = d2 * beta;
      D1 = d1 * beta;
      diffsArray[packetIndex*128*2 + i*2 ] = D1; // todo find a better way accessing diffsArray
      diffsArray[packetIndex*128*2 + i*2 + 1] = D2;

      n1 = D1 + lastNibble.get();

      n2 = D2 + n1;

      lastNibble.set(n2);

      n1 = n1 + mu;
      n2 = n2 + mu;

//      System.out.println("i: " + i + "\t\tn1: " + n1 + "\t\tn2: " + n2);

//      decoded[4 * (i - offset)] = (byte) n1;
//      decoded[4 * (i - offset) + 1] = (byte) (n1 / 256 > 127 ? 127 : n1 / 256 < -128 ? -128 : n1 / 256);
//      decoded[4 * (i - offset) + 2] = (byte) n2;
//      decoded[4 * (i - offset) + 3] = (byte) (n2 / 256 > 127 ? 127 : n2 / 256 < -128 ? -128 : n2 / 256); decoded[4 * (i - offset)] = (byte) n1;
      decoded[4 * (i - offset)] = (byte) n1;
      decoded[4 * (i - offset) + 1] = (byte) (n1 >> 8);
      decoded[4 * (i - offset) + 2] = (byte) n2;
      decoded[4 * (i - offset) + 3] = (byte) (n2 >> 8);

      if (n1 > max.get() ) max.set(n1);
      if (n2 > max.get() ) max.set(n2);
      if (n1 < min.get() ) min.set(n1);
      if (n2 < min.get() ) min.set(n2);
    }

    return decoded;
  }

  private void getAudio(String options) throws IOException {
    String request = audioCode + options;

    DatagramPacket packetSent = new DatagramPacket(request.getBytes(), request.getBytes().length, address, hostPort);
    socket.send(packetSent);

    switch (codec) {
      case DPCM -> getDPCM();
      case AQDPCM -> getAQDPCM();
    }
    System.out.println("max = " + max.get() + "min = " + min.get());
  }

  private void getDPCM() throws IOException {
    System.out.println("getting DPCM...");

    diffsArray = new int[packetNum * 128 * 2];

    DatagramPacket packetRcv = new DatagramPacket(buffer, buffer.length);

    AtomicInteger lastNibble = new AtomicInteger(0);

    for (packetIndex = 0; packetIndex < packetNum; packetIndex++) {
      socket.receive(packetRcv);

      audioBuffer.put(decodeDCPM(packetRcv.getData(), packetRcv.getLength(), lastNibble));
    }

    saveArrayAsCSV("DPCM_diffs", diffsArray, new String[] {"diffs"});
  }

  private void getAQDPCM() throws IOException {
    System.out.println("getting AQ-DPCM");

    diffsArray = new int[packetNum * 128 * 2];
    musArray = new int[packetNum];
    betasArray = new int[packetNum];

    DatagramPacket packetRcv = new DatagramPacket(buffer, buffer.length);

    AtomicInteger lastNibble = new AtomicInteger(0);

    for (packetIndex = 0; packetIndex < packetNum; packetIndex++) {
      socket.receive(packetRcv);

      audioBuffer.put(decodeAQ_DCPM(packetRcv.getData(), packetRcv.getLength(), lastNibble));
    }

    saveArrayAsCSV("AQDPCM_diffs", diffsArray, new String[] {"diffs"});
    saveArrayAsCSV("AQDPCM_mus", musArray, new String[] {"mus"});
    saveArrayAsCSV("AQDPCM_betas", betasArray, new String[] {"betas"});
  }

  private void saveArrayAsCSV(String fileName, int[] intsArray, String[] header) throws IOException {
    Files.createDirectories(Paths.get(directory));
    File file = new File(directory + "/" + fileName + ".csv");

    try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
      writer.writeNext(header);
      // Convert int[] to String[] and then write it with writeNext()
      writer.writeNext(Arrays.stream(intsArray).mapToObj(String::valueOf).toArray(String[]::new));
    }
  }

  private void playAudio() {
    AudioFormat audioFormat = new AudioFormat(sampleRate, codec.getQ(), 1, true, false);

    try {
      SourceDataLine audioOut = AudioSystem.getSourceDataLine(audioFormat);
      audioOut.open(audioFormat, audioBuffer.capacity());
      audioOut.start();

      int chunkSize = audioBuffer.capacity() / packetNum;
      int offset = 0;
      byte[] chunk = new byte[chunkSize];

      for (int i = 0; i < packetNum; i++) {
        audioBuffer.get(offset, chunk);
        audioOut.write(chunk, 0, chunkSize);
        offset += chunkSize;
      }

      audioOut.stop();
      audioOut.close();
    } catch (LineUnavailableException e) {
      e.printStackTrace();
    }
  }

  public void playTrack(int trackNumber, int duration, Codec codec) {
    setPlaybackDuration(duration);
    setCodec(codec);
    allocateAudioBuffer(codec);

    Thread receiverThread = new Thread(() -> {
      try {
        getAudio(codec.getNameAsServerOption() + "L" + String.format("%02d", trackNumber) + "F" + String.format("%03d", packetNum));
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    Thread audioPlayerThread = new Thread(this::playAudio);

    receiverThread.start();
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    audioPlayerThread.start();

    try {
      receiverThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    try {
      audioPlayerThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }


  public void playFrequencies(int duration) {
    setPlaybackDuration(duration);
    setCodec(Codec.DPCM);
    allocateAudioBuffer(Codec.DPCM);

    Thread receiverThread = new Thread(() -> {
      try {
        getAudio(codec.getNameAsServerOption() + "T" + String.format("%03d", packetNum));
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    Thread audioPlayerThread = new Thread(this::playAudio);

    receiverThread.start();
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    audioPlayerThread.start();

    try {
      receiverThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    try {
      audioPlayerThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void allocateAudioBuffer(Codec codec) {
    audioBuffer = ByteBuffer.allocate(packetNum * 128 * 2 * (codec.getQ() / 8)); // size = [audio packets] * [packet size] * [nibbles] * [quantifier's size in bytes]
  }

  public void setPlaybackDuration(int seconds) {
    if (seconds < 0) seconds = 1;
    this.packetNum = 32 * seconds;
  }

  public void setSampleRate(int sampleRate) {
    this.sampleRate = sampleRate;
  }

  public void setCodec(Codec codec) {
    this.codec = codec;
  }

  private void saveAudioAsWAVE(String fileName) throws IOException {
    Files.createDirectories(Paths.get(directory));
    File file = new File(directory + "/" + fileName + ".wav");

    AudioFormat audioFormat = new AudioFormat(sampleRate, codec.getQ(), 1, true, false);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(audioBuffer.array());
    AudioInputStream audioInputStream = new AudioInputStream(inputStream,audioFormat, audioBuffer.limit()/audioFormat.getFrameSize());

    try {
      AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, file);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void saveTrack(int trackNumber, int duration, Codec codec, String fileName) throws IOException {
    setPlaybackDuration(duration);
    setCodec(codec);
    allocateAudioBuffer(codec);

    getAudio(codec.getNameAsServerOption() + "L" + String.format("%02d", trackNumber) + "F" + String.format("%03d", packetNum));
    audioBuffer.flip();
    saveAudioAsWAVE(String.join("_", fileName, "track" + trackNumber, duration + "s"));
  }

  public void saveFrequencies(int duration,String fileName) throws IOException {
    setPlaybackDuration(duration);
    setCodec(Codec.DPCM);
    allocateAudioBuffer(Codec.DPCM);

    getAudio(codec.getNameAsServerOption() + "T" + String.format("%03d", packetNum));
    audioBuffer.flip();
    saveAudioAsWAVE(String.join("_", fileName, "frequencies", duration + "s"));
  }

  public void closeSocket() {
    socket.close();
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }
}
