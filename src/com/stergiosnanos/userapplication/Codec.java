package com.stergiosnanos.userapplication;

public enum Codec {
  AQDPCM("AQ-DPCM", "AQ", 16),
  DPCM("DPCM", "", 8);

  private final String name;
  private final String nameAsServerOption;
  private final int Q;

  Codec(String name, String nameAsServerOption, int Q) {
    this.name = name;
    this.nameAsServerOption = nameAsServerOption;
    this.Q = Q;
  }

  public String getName() {
    return name;
  }

  public String getNameAsServerOption() {
    return nameAsServerOption;
  }

  public int getQ() {
    return Q;
  }
}
