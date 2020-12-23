package com.stergiosnanos.userapplication;

public enum OBDRequest {
  ENGINE_TIME("Engine run time", "01", "1F", " sec"),
  AIR_TEMP("Intake air temperature", "01", "0F", " \u00B0" + "C"),
  THROTTLE_POS("Throttle position", "01", "11", " %"),
  ENGINE_RPM("Engine RPM", "01", "0C", " RPM"),
  VEHICLE_SPEED("Vehicle speed", "01", "0D", " Km/h"),
  COOLANT_TEMP("Coolant temperature", "01", "05", " \u00B0" + "C");

  private final String name;
  private final String mode;
  private final String pid;
  private final String units;

  OBDRequest(String name, String mode, String pid, String units) {
    this.name = name;
    this.mode = mode;
    this.pid = pid;
    this.units = units;
  }

  public String getRequest() {
    return String.format("OBD=%s %s", mode, pid);
  }

  public String getUnits() {
    return units;
  }

  @Override
  public String toString() {
    return name;
  }
}
