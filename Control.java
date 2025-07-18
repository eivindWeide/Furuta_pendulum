public class Control {
  private double sampleTime; // in seconds
  private double thetaError = 0;
  private double phiError = 0;

  private double phiRef = 0;
  private double thetaRef = 0;

  private double oldTheta = 0;
  private double thetaDot = 0;

  private double oldPhi = 0;
  private double phiDot = 0;

  private double pi = Math.PI;
  
  public Parameters param;

  private double u = 0;

  private Status status;

  //Enums used for internal reference.
  enum Status {
    ON,
    OFF,
    SWINGUP
}

  public Control() {
    this.sampleTime = 0.05;
    this.status = Status.OFF;
  }

  //Updates the parameters.
  public synchronized void updateParams(Parameters param) {
    this.param = (Parameters) param.clone();
    this.phiRef = param.phiRef;
  }

  //Calculates the error of theta and phi.
  private void errorCalculate(double theta, double phi) {
    thetaError = (thetaRef - theta) % (2 * pi);
    if  (thetaError > pi) {
      thetaError =  thetaError - 2 * pi;
    }
    if (thetaError < -pi) {
      thetaError =  thetaError + 2 * pi;
    }

    phiError = (phiRef - phi) % (2 * pi);
    if (phiError > pi) {
      phiError = phiError - 2 * pi;
    }
    if (phiError < -pi) {
      phiError =  phiError + 2 * pi;
    }
  }

  //Tells the controller to switch off.
  public synchronized void setStatusOff() {
    this.status = Status.OFF;
  }

  //Called when in lower mode to determine the controlsignal.
  private void checkLowerStatus() {
    if (Math.abs(thetaError) < param.thetaThresh + 1 && Math.abs(thetaDot) < param.thetaDot + 1) {
      this.status = Status.ON;
    }

    if (Math.abs(thetaError) > (param.thetaThresh + 2) && Math.abs(thetaDot) > (param.thetaDot + 3)) {
      this.status = Status.OFF;
    }
  }

  //Called when in upper mode to determine the controlsignal.
  private void checkUpperStatus() {
    if (Math.abs(thetaError) < param.thetaThresh && Math.abs(thetaDot) < param.thetaDot) {
      this.status = Status.ON;
    } else if (Math.abs(phiDot) < param.phiDot) {
      this.status = Status.SWINGUP;
    } 

    if (Math.abs(thetaError) > (param.thetaThresh + 0.2) && status != Status.SWINGUP) {
      this.status = Status.OFF;
    }
  }


  //Calculates the controlsignal when in Lower-Mode
  public synchronized double lowerCalculate(double theta, double phi) {
    thetaRef = pi;

    errorCalculate(theta, phi);
    checkLowerStatus();

    thetaDot = (theta - oldTheta)/ sampleTime;
    phiDot = (phi - oldPhi)/ sampleTime;

    System.out.println(thetaDot);
    System.out.println(phiDot);
    
    if (status == Status.ON) {
      u = thetaError * 1.4259 + thetaDot * -0.0345 + phiError * 0.0835 + phiDot * -0.0858;
    } else {
      u = 0;
    }
    
    oldPhi = phi;
    oldTheta = theta;
    return u;
  }


  //Calculates the control-signal when in Upper-Mode
  public synchronized double upperCalculate(double theta, double phi) {
    thetaRef = 0;

    errorCalculate(theta, phi);
    checkUpperStatus();

    thetaDot = (theta - oldTheta)/ sampleTime;
    phiDot = (phi - oldPhi)/ sampleTime;

    if (status == Status.ON) {
      u = -(thetaError * 2.7199 + thetaDot * -0.5069 + phiError * 0.0824 + phiDot * -0.0847);
    } else if (status == Status.SWINGUP) {
      u = param.k1 * Math.signum(
          (Math.cos(thetaError) + ((thetaDot * thetaDot) / (2 * 6.7 * 6.7)) - 1) * thetaDot * Math.cos(thetaError))
          - param.k2 * phiDot;
    } else {
      u = 0;
    }

    oldPhi = phi;
    oldTheta = theta;
    return u;
  }

  //Sets the phi-reference for the arm.
  public synchronized void setArmRef(double angle) {
    this.phiRef = angle;
  }

  //Returns the time.
  public synchronized long getHMillis() {
    return (long) (sampleTime * 1000);
  }

}