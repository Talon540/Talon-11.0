package org.usfirst.frc.team540.robot;

import com.mindsensors.CANSD540;

import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends IterativeRobot {
	final String defaultAuto = "Default";
	// timer modes
	final String baselineTime = "Baseline with timer";
	final String switchTimeLeft = "Left Switch From Side with Timer";
	final String switchTimeRight = "Right Switch From Side with Timer";
	final String fallbackTimeLeft = "Fallback Switch with Timer (L)";
	final String fallbackTimeRight = "Fallback Switch with Timer (R)";

	// encoder modes
	final String baselineEnc = "Baseline with encoder";
	final String switchMiddleEnc = "Switch with encoder (M)";
	final String switchRightEnc = "Switch with encoder(R)";
	final String switchLeftEnc = "Switch with encoder (L)";
	final String fallbackLeft = "Fallback switch/baseline (L)";
	final String fallbackRight = "Fallback switch/baseline (R)";

	String autoSelected;
	SendableChooser<String> chooser = new SendableChooser<>();

	// motor, joy-stick, and controller fields
	CANSD540 frontLeft, frontRight, backLeft, backRight, midLeft, midRight, intake1, intake2, intakeVert, hook, winch;
	Joystick leftJoy, rightJoy;
	XboxController xbox;

	// Sensor fields
	Encoder enc1, enc2;
	ADXRS450_Gyro gyro;
	AnalogInput IR;

	// will be used in driveCode() to get current movement
	double left, right, intakeL, intakeR, hooker, wench;

	// sensor fields
	double angle, irDist, pulse, dist;
	// double liftPulse;

	// variable to compensate for inconsistent motor powers
	double scale;

	boolean height, toggle, cube, still;

	// Constant multiplied by the
	final static double TURN_CONSTANT = 0.5;

	// FMS
	String FMS, enemyFMS;

	final static double DRIVE_CONSTANT = -0.8;

	// Counter and side for Auto and reverse for tele-op
	int counter, reverse, side;

	@Override
	public void robotInit() {
		// Default currently turns
		chooser.addDefault("Default (Do Nothing)", defaultAuto);
		chooser.addObject("Baseline with timer", baselineTime);
		chooser.addObject("Left Switch From Side (Timer & Gyro)", switchTimeLeft);
		chooser.addObject("Right Switch From Side (Timer & Gyro)", switchTimeRight);
		chooser.addObject("Fallback Left Switch (Dead Sensors)", fallbackTimeLeft);
		chooser.addObject("Fallback Right Switch (Dead Sensors)", fallbackTimeRight);
		chooser.addObject("Baseline with encoder (From Side Position)", baselineEnc);

		chooser.addObject("Switch with Encoder (From Left Position)", switchLeftEnc);
		chooser.addObject("Switch with Encoder (From Right Position)", switchRightEnc);

		chooser.addObject(" -- Middle Switch with Encoder (From Middle Position)", switchMiddleEnc);
		chooser.addObject(" -- Left Fallback Switch (From Left Position)", fallbackLeft);
		chooser.addObject(" -- Right Fallback Switch (From Right Position)", fallbackRight);

		SmartDashboard.putData("Auto choices", chooser);

		// wheel motors
		frontLeft = new CANSD540(6);
		backLeft = new CANSD540(7);
		frontRight = new CANSD540(4);
		backRight = new CANSD540(5);

		// intake motors
		intake1 = new CANSD540(11);
		intake2 = new CANSD540(9);
		intakeVert = new CANSD540(10);

		// climber and winch
		hook = new CANSD540(2);
		winch = new CANSD540(1);

		// joystick and controller motors
		leftJoy = new Joystick(0);
		rightJoy = new Joystick(1);
		xbox = new XboxController(2);

		// drivetrain encoder
		enc1 = new Encoder(2, 3, false);
		enc1.setMaxPeriod(0.1);
		enc1.setMinRate(5);
		// 0.00078487 ft per pulse -> 10 ft per 12741 pulses
		enc1.setDistancePerPulse(0.00078487);
		enc1.setSamplesToAverage(10);

		// sensors
		gyro = new ADXRS450_Gyro();
		IR = new AnalogInput(0);

		// Note: Never use gyro.calibrate() and gyro.reset() together
		// calibrates encoders
		enc1.reset();

		// initializes counter
		counter = 0;

		// initializes reverse
		reverse = 1;

		// initializes toggle
		toggle = false;

		// initializes scale
		scale = 0.75;

		// initialize voltage ramps
		frontLeft.setVoltageRamp(100);
		backLeft.setVoltageRamp(100);
		frontRight.setVoltageRamp(100);
		backRight.setVoltageRamp(100);

		intake1.setVoltageRamp(100);
		intake2.setVoltageRamp(100);
		intakeVert.setVoltageRamp(100);

		// Camera Server
		CameraServer server = CameraServer.getInstance();
		UsbCamera cam = server.startAutomaticCapture();
		cam.setResolution(320, 240);
		cam.setFPS(15);

		// FMS Initialization
		FMS = "";

	}

	public void autonomousInit() {
		autoSelected = chooser.getSelected();
		System.out.println("Auto selected: " + autoSelected);

		// FMS
		while (FMS.length() < 1) {
			FMS = DriverStation.getInstance().getGameSpecificMessage();
		}
		SmartDashboard.putString("Our Switch Side: ", FMS);

		counter = 0;
		gyro.reset();
		enc1.reset();
	}

	@Override
	public void autonomousPeriodic() {
		// Gets sensor values and displays them in SmartDashboard

		angle = gyro.getAngle();
		irDist = IR.getVoltage();
		pulse = enc1.get();
		dist = enc1.getDistance();

		SmartDashboard.putNumber("Angle: ", angle);
		SmartDashboard.putNumber("IR Distance: ", irDist);
		SmartDashboard.putNumber("Pulse Count: ", pulse);
		SmartDashboard.putNumber("Distance Traveled: ", dist);
		SmartDashboard.putNumber("Auto Counter: ", counter);

		switch (autoSelected) {

		// MOTORS ARE BOTH INVERTED IN AUTO (no reason found, but it works)
		
		//Timer Modes
		case baselineTime: 
			if (counter == 0) {
				motorSet(-.5, -.5); // go forward for 3 seconds and cross the baseline
				Timer.delay(3);
				motorSet(0, 0);
				counter++;
			}
			if (counter == 1) {
				motorSet(0, 0);
			}
			break;

		case switchTimeLeft:
			if (counter == 0) {
				motorSet(-.5, -.5); // go forward for 2.8 seconds
				Timer.delay(2.8); 
				motorSet(0, 0);
				Timer.delay(1); //stops for 1 second
				gyro.reset();
				counter++;
			}
			if (FMS.charAt(0) == 'L') {

				if (counter == 1) { // turns 90 degrees to the left
					if (angle >= 90) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(-propGyro(90, angle), propGyro(90, angle));
					}
				}
				if (counter == 2) { 
					motorSet(-.5, -.5); //goes forward for 1 second
					Timer.delay(1);
					motorSet(0, 0);
					intakeVert.set(-0.8); //raises the intake elevator for 2 seconds
					Timer.delay(2);
					intakeVert.set(-0.15); //keeps the elevator from drooping due to gravity
					intakeMotorSet(1, 1); //spits out the cube the cube
					Timer.delay(2); 
					intakeMotorSet(0, 0);
					counter++;
				}
				if (counter == 3) {
					motorSet(0, 0);
					intakeMotorSet(0, 0);
				}
			} else {
				motorSet(0, 0);
			}
			break;

		case switchTimeRight:
			if (counter == 0) {
				motorSet(-.5, -.5); // go forward to 2.8 seconds
				Timer.delay(2.8); 
				motorSet(0, 0);
				Timer.delay(1); 
				gyro.reset();
				counter++;
			}
			if (FMS.charAt(0) == 'R') {

				if (counter == 1) { 
					if (angle <= -90) { //turns 90 degrees to the right
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(propGyro(-90, angle), -propGyro(-90, angle));
					}
				}
				if (counter == 2) {
					motorSet(-.5, -.5); //goes forward for 1 second
					Timer.delay(1);
					motorSet(0, 0);
					intakeVert.set(-0.85); //raises the intake elevator for 2 seconds
					Timer.delay(2);
					intakeVert.set(-0.15); //keeps the elevator from drooping due to gravity
					intakeMotorSet(1, 1); //keeps the cube in the intake
					Timer.delay(2);
					intakeMotorSet(0, 0);
					counter++;
				}
				if (counter == 3) {
					motorSet(0, 0);
					intakeMotorSet(0, 0);
				}
			} else {
				motorSet(0, 0);
			}
			break;

		case fallbackTimeLeft: // used in case no sensors work
			if (counter == 0) {
				motorSet(-.5, -.5); // go forward to 3 seconds
				Timer.delay(3);
				motorSet(0, 0);
				counter++;
			}
			if (counter == 1) {
				if (FMS.charAt(0) == 'L') {
					intakeVert.set(-1); //raises the intake elevator
					intakeMotorSet(-.5, -.5); //spits out the cube
					Timer.delay(2);
					intakeVert.set(-0.15); //keeps the elevator from drooping
					Timer.delay(0.2);
					intakeMotorSet(1, 1); //intakes the cube
					Timer.delay(2);
					intakeMotorSet(0, 0);
				} else {
					motorSet(0, 0);
				}
				counter++;
			}
			break;

		case fallbackTimeRight: // used in case sensors do not work
			if (counter == 0) {
				motorSet(-.5, -.5); // go forward to 3 seconds
				Timer.delay(3);
				motorSet(0, 0);
				counter++;
			}
			if (counter == 1) {
				if (FMS.charAt(0) == 'R') {
					intakeVert.set(-0.85); //raises the intake elevator
					Timer.delay(2);
					intakeVert.set(-0.15); //keeps the elevator from drooping
					Timer.delay(2);
					intakeMotorSet(0, 0);
					motorSet(-.6, -.6); //spits out the cube
					Timer.delay(0.5);
					motorSet(0, 0);
				} else {
					motorSet(0, 0);
					counter++;
				}
			}
			break;

			
		//Encoder modes
		case baselineEnc:
			if (counter == 0) {
				if (dist >= 9.5) { // goes forward 9.5 feet
					motorSet(0, 0);
					counter++;
				} else {
					motorSet(prop(9.5, dist), prop(9.5, dist));
				}
			}
			if (counter == 1) {
				motorSet(0, 0);
			}
			break;

		case switchMiddleEnc:
			
			if (FMS.charAt(0) == 'L') { // if the switch is left
				if (counter == 0) {
					if (dist >= 2) { // move away from the wall so it can turn by two feet
						motorSet(0, 0);
						Timer.delay(0.5);
						gyro.reset();
						counter++;
					} else {
						motorSet(-.5, -.5);
					}
				}
				if (counter == 1) { // turns to the left 45 degrees

					if (angle <= -45) { 
						motorSet(0, 0);
						enc1.reset();
						counter++;
					} else {
						motorSet(propGyro(-45, angle), -propGyro(-45, angle));
					}
				}
				if (counter == 2) { // Goes forward 5 feet  
					if (dist >= 5) { 
						motorSet(0, 0);
						Timer.delay(0.5);
						gyro.reset();
						counter++;
					} else {
						motorSet(prop(5, dist), prop(5, dist));
					}
				}
				if (counter == 3) { // turns to the right 45 degrees to face the front of the switch
	
					if (angle >= 42) { // 42 degrees instead of 45 to compensate for error
						motorSet(0, 0);
						counter++;
					} else { 
						motorSet(-propGyro(42, angle), propGyro(42, angle)); //or else keep turning
					}
				}
				if (counter == 4) {
					motorSet(-0.4, -0.4); //goes forward for 1 second
					Timer.delay(1);
					motorSet(0, 0);
					counter++;
				}
				if (counter == 5) {
					motorSet(0, 0); 
					intakeVert.set(-1); //raises the intake elevator 
					Timer.delay(2);
					intakeVert.set(-0.15); //keeps the elevator from drooping due to gravity
					intakeMotorSet(1, 1); // keeps the cube in the intake
					Timer.delay(4);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
					counter++;
				}

				if (counter == 6) {
					motorSet(0, 0);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
				}

			} else if (FMS.charAt(0) == 'R') { // right hand switch
				if (counter == 0) {
					if (dist >= 2) { // move away from the wall so it can turn by two feet
						motorSet(0, 0);
						Timer.delay(0.5);
						gyro.reset();
						counter++;
					} else {
						motorSet(-.5, -.5);
					}
				}
				if (counter == 1) {
					// turns 45 degrees to the right
					if (angle >= 45) {
						motorSet(0, 0);
						enc1.reset();
						counter++;
					} else {
						motorSet(-propGyro(45, angle), propGyro(45, angle));
					}
				}
				if (counter == 2) {
					if (dist >= 4.5) { // was 9 feet, now 4.5 feet; DOES NOT WORK WITH 4 FOR SOME REASON
						motorSet(0, 0);
						Timer.delay(0.5);
						gyro.reset();
						counter++;
					} else {
						motorSet(prop(4.5, dist), prop(4.5, dist));
					}
				}
				if (counter == 3) { // turn to face the switch
					if (angle <= -42) { //turns 45 degrees but changed to 42 to compensate for error
						motorSet(0, 0);
						enc1.reset();
						counter++;
					} else { 
						motorSet(propGyro(-42, angle), -propGyro(-42, angle)); // or else keep turning
					}
				}
				if (counter == 4) {
					motorSet(-0.4, -0.4); //goes forward for 1 second
					Timer.delay(1);
					motorSet(0, 0);
					counter++;
				}
				if (counter == 5) {
					motorSet(0, 0);
					intakeVert.set(-1); //raises the intake elevator
					Timer.delay(2);
					intakeVert.set(-0.15); //keeps the elevator from drooping
					intakeMotorSet(1, 1); //keeps the cube from falling out
					Timer.delay(4);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
					counter++;
				}

				if (counter == 6) {
					motorSet(0, 0);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
				}
			} else { // If there's an error with FMS, only do the baseline
					 // This is identical to baselineEnc
				if (counter == 0) {
					if (dist >= 9.5) { // goes forward 9.5 feet
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(prop(9.5, dist), prop(9.5, dist));
					}
				}
				if (counter == 1) {
					motorSet(0, 0);
				}
			}
			break;

		case switchLeftEnc: 
			if (FMS.charAt(0) == 'L') {
				if (counter == 0) {
					if (dist >= 14) { // move forward 14 feet
						motorSet(0, 0);
						enc1.reset(); //reset the gyro and encoder
						gyro.reset();
						counter++;
					} else {
						motorSet(prop(14, dist), prop(14, dist));
					}
				}
				if (counter == 1) { // turn left towards the switch
					if (angle >= 90) {
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet(-propGyro(90, angle), propGyro(90, angle));
					}
				}
				if (counter == 2) { // move to the switch
					if (dist >= 2.66666666667) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(prop(2.66666666667, dist), prop(2.66666666667, dist));
					}
				}

				if (counter == 3) { // lift the intake
					intakeVert.set(-.85);
					Timer.delay(2);
					intakeVert.set(-0.15); //keep the intake from drooping
					counter++;
				}
				if (counter == 4) { // go forward 2 feet and spit out the cube
					if (dist >= 2) {
						motorSet(0, 0);
						intakeMotorSet(-1, -1);
						Timer.delay(2);
						intakeMotorSet(0, 0);
						intakeVert.set(0);
						enc1.reset();
						counter++;
					} else {
						motorSet(prop(2, dist), prop(2, dist));
					}
				}
				if (counter == 5) { // stop all motors
					motorSet(0, 0);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
				}

			} else if (FMS.charAt(0) == 'R') { // Switch on right side
				//						         _
				if (counter == 0) { // move 19.583 in. past the switch
					if (dist >= 19.58333333333333333) {
						motorSet(0, 0);
						gyro.reset();
						counter++;
					} else {
						motorSet(prop(19.58333333333333333, dist), prop(19.58333333333333333, dist));
					}

				}
				if (counter == 1) { // Turn to go down
					if (angle >= 90) {
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet(-propGyro(90, angle), propGyro(90, angle));
					}
				}
				if (counter == 2) { // move behind the switch
					if (dist >= 18.1155833333333) {
						motorSet(0, 0);
						counter++;
						gyro.reset();
					} else {
						motorSet(prop(18.1155833333333, dist), prop(18.1155833333333, dist));
					}
				}
				if (counter == 3) { // Turn to go back
					if (angle >= 90) {
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet(propGyro(90, angle), -propGyro(90, angle));
					}
				}
				if (counter == 4) { // move for the third time
					if (dist >= 2.6666666666667) {
						motorSet(0, 0);
						counter++;
						gyro.reset();
					} else {
						motorSet(prop(2.6666666666667, dist), prop(2.6666666666667, dist));
					}
				}
				if (counter == 5) { // Turn towards switch
					if (angle >= 90) {
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet(-propGyro(90, angle), propGyro(90, angle));
					}
				}

				if (counter == 6) { // lift the intake
					intakeVert.set(-.85);
					Timer.delay(2);
					intakeVert.set(-0.15);
					counter++;
				}
				if (counter == 7) { // go forward 2 feet and spit out the cube
					if (dist >= 2) {
						motorSet(0, 0);
						intakeMotorSet(-1, -1);
						Timer.delay(2);
						intakeMotorSet(0, 0);
						intakeVert.set(0);
						enc1.reset();
						counter++;
					} else {
						motorSet(prop(2, dist), prop(2, dist));
					}
				}
				if (counter == 8) { // stop all motors
					motorSet(0, 0);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
				}
			} else { // If there's an error with FMS, only do the baseline
						// This is identical to baselineEnc
				if (counter == 0) {
					if (dist >= 9.5) { // 9.5 feet
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(prop(9.5, dist), prop(9.5, dist));
					}
				}
				if (counter == 1) {
					motorSet(0, 0);
				}
			}
			break;

		case switchRightEnc: // Right side switch
			if (FMS.charAt(0) == 'R') {
				if (counter == 0) {
					if (dist >= 14) { // move forward 168 in
						motorSet(0, 0);
						gyro.reset();
						enc1.reset();
						counter++;
					} else {
						motorSet((prop(14, dist)), (prop(14, dist)));
					}
				}
				if (counter == 1) {
					if (angle <= -90) { // turn towards switch
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet(propGyro(-90, angle), propGyro(-90, angle));
					}
				}
				if (counter == 2) {
					if (dist >= 2.66666666667) { // move towards switch
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(0, 0);
					}
				}

			} else if (FMS.charAt(0) == 'L') { // Switch on left side

				if (counter == 0) { // move past switch
					if (dist >= 19.58333333333333333) {
						motorSet(0, 0);
						gyro.reset();
						counter++;
					} else {
						motorSet(prop(19.58333333333333333, dist), (prop(19.58333333333333333, dist)));
					}

				}
				if (counter == 1) { // Turn to go down
					if (angle <= -90) {
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet((propGyro(-90, angle)), -(propGyro(-90, angle)));
					}
				}
				if (counter == 2) { // move behind switch
					if (dist >= 18.1155833333333) {
						motorSet(0, 0);
						gyro.reset();
						counter++;
					} else {
						motorSet(prop(18.1155833333333, dist), prop(18.1155833333333, dist));
					}
				}
				if (counter == 3) { // Turn to go back
					if (angle <= -90) {
						motorSet(0, 0);
						enc1.reset();
						counter++;
					} else {
						motorSet((propGyro(-90, angle)), -(propGyro(-90, angle)));
					}
				}
				if (counter == 4) { // move for the third time
					if (dist >= 2.6666666666667) {
						motorSet(0, 0);
						gyro.reset();
						counter++;
					} else {
						motorSet(prop(2.6666666666667, dist), prop(2.6666666666667, dist));
					}
				}
				if (counter == 5) { // Turn towards the switch
					if (angle <= -90) {
						motorSet(0, 0);
						enc1.reset();
						counter++;
					} else {
						motorSet(propGyro(-90, angle), -propGyro(-90, angle));
					}
				}
				if (counter == 6) { // lift the intake
					intakeVert.set(-.85);
					Timer.delay(2);
					intakeVert.set(-0.15);
					counter++;
				}
				if (counter == 7) { // go forward 2 feet and spit out the cube
					if (dist >= 2) {
						motorSet(0, 0);
						intakeMotorSet(-1, -1);
						Timer.delay(2);
						intakeMotorSet(0, 0);
						intakeVert.set(0);
						enc1.reset();
						counter++;
					} else {
						motorSet(prop(2, dist), prop(2, dist));
					}
				}
				if (counter == 8) { // stop all motors
					motorSet(0, 0);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
				}
			} else { // If there's an error with FMS, only do the baseline
						// This is identical to baselineEnc
				if (counter == 0) {
					if (dist >= 9.5) { // go forward 9.5 feet
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(prop(9.5, dist), prop(9.5, dist));
					}
				}
				if (counter == 1) {
					motorSet(0, 0);
				}
			}
			break;
		case fallbackLeft:
			if (counter == 0) { // go forward fourteen feet
				if (dist >= 12) {
					motorSet(0, 0);
					gyro.reset();
					counter++;
				} else {
					motorSet(prop(12, dist) * 1.1, prop(12, dist));
				}
			}
			if (FMS.charAt(0) == 'L') {
				if (counter == 1) { // turn to the right towards the side of the switch
					if (angle >= 90) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(-propGyro(90, angle), propGyro(90, angle));
					}
				}
				if (counter == 2) { // lift the intake
					intakeVert.set(-1);
					intakeMotorSet(-0.5, -0.5);
					Timer.delay(2);
					intakeVert.set(-0.15); //keeps the intake from drooping
					intakeMotorSet(0, 0);
					enc1.reset();
					counter++;
				}
				if (counter == 3) { // go forward and spit out cube
					motorSet(-0.5, -0.5);
					Timer.delay(2);
					motorSet(0, 0);
					counter++;
				}
				if (counter == 4) {
					motorSet(0, 0);
					intakeMotorSet(1, 1);
					Timer.delay(4);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
					counter++;
				}
				if (counter == 5) { // stop all motors
					motorSet(0, 0);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
				}
			}
			break;
		case fallbackRight:
			if (counter == 0) { // go forward 12 feet
				if (dist >= 12) {
					motorSet(0, 0);
					gyro.reset();
					counter++;
				} else {
					motorSet(prop(12, dist), prop(12, dist));
				}
			}
			if (FMS.charAt(0) == 'R') {
				if (counter == 1) { // turn to the right towards the side of the switch
					if (angle <= -90) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(propGyro(-90, angle), -propGyro(-90, angle));
					}
				}
				if (counter == 2) { // lift the intake
					intakeVert.set(-1);
					intakeMotorSet(-0.5, -0.5);
					Timer.delay(2);
					intakeVert.set(-0.15);
					intakeMotorSet(0, 0);
					enc1.reset();
					counter++;
				}
				if (counter == 3) { // go forward and spit out cube
					motorSet(-0.5, -0.5);
					Timer.delay(2);
					motorSet(0, 0);
					counter++;
				}
				if (counter == 4) {
					motorSet(0, 0);
					intakeMotorSet(1, 1);
					Timer.delay(4);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
					counter++;
				}
				if (counter == 5) { // stop all motors
					motorSet(0, 0);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
				}
			}
			break;

		case defaultAuto:
			motorSet(0, 0);
			break;
		}
	}

	@Override
	public void teleopPeriodic() {
		// gets sensor values and displays them in SmartDashboard
		angle = gyro.getAngle();
		irDist = IR.getVoltage();
		pulse = enc1.get();
		dist = enc1.getDistance();
		
		if (irDist > 1) {
			cube = true;
		} else {
			cube = false;
		}

		SmartDashboard.putNumber("Angle: ", angle);
		SmartDashboard.putNumber("IR Distance: ", irDist);
		SmartDashboard.putNumber("Pulse Count: ", pulse);
		SmartDashboard.putNumber("Distance Traveled: ", dist);
		SmartDashboard.putBoolean("Cube in intake: ", cube);

		// calls drive() to drive
		drive();

		// calls intake() to deal with cube manipulation
		intake();

		// calls switch() to raise the intake to switch height
		moveIntake_Teleop();

		// calls climb() to deploy hook and pull in the winch
		climb();
	}

	/**
	 * Get the proportion of motor speed based on the distance to the target
	 * value. Used in auto.
	 * 
	 * @param target
	 *            the target value
	 * @param currentEnc
	 *            the current encoder value
	 * @return the proportion of the motor speed constant to set
	 */
	public static double prop(double target, double currentEnc) {
		if (((target - currentEnc) / target) > 0.8) {
			return -0.4;
		} else if (((target - currentEnc) / target) < 0.4) {
			return -0.3;
		} else {
			return (((target - currentEnc) / target) * DRIVE_CONSTANT);
		}
	}

	/**
	 * Gets the proportion to be used with motor speed during gyro turns. Used
	 * in auto.
	 * 
	 * @param target
	 *            the target value
	 * @param currentGyro
	 *            the current gyro value
	 * @return the proportion of the motor speed constant to set
	 */
	public static double propGyro(double target, double currentGyro) {
		if (((target - currentGyro) / target) > 0.8) {
			return Math.abs(((target - currentGyro) / target));
		} else {
			return 0.23; // WAS 0.2
		}

	}

	/**
	 * Used in drive code
	 * 
	 * Sets current positions of joysticks as values to left and right
	 */
	private void drive() {
		
		if (leftJoy.getRawButton(1) == true && leftJoy.getRawButton(3) == true) {
			scale = 1;
		} else {
			scale = 0.75;
		}

		if (Math.abs(leftJoy.getY()) > 0.2) {
			left = leftJoy.getY() * reverse;
			if (toggle)
				right = leftJoy.getY() * reverse;
		} else {
			left = 0;
		}

		if (toggle) {
			if (Math.abs(rightJoy.getX()) > 0.2) {
				left += rightJoy.getX() * reverse;
				right -= rightJoy.getX() * reverse;
			}
		} else {
			if (Math.abs(rightJoy.getY()) > 0.2) {
				right = rightJoy.getY() * reverse;
			} else {
				right = 0;
			}
		}

		if (left > 1)
			left = 1;
		if (left < -1)
			left = -1;
		if (right > 1)
			right = 1;
		if (right < -1)
			left = -1;

		motorSet(left * scale, right * scale);
	}

	/**
	 * Sets motor speed values
	 * 
	 * @param left
	 *            - left motor speed
	 * @param right
	 *            - right motor speed
	 */
	private void motorSet(double left, double right) {
		frontLeft.set(left);
		backLeft.set(left);

		frontRight.set(-right);
		backRight.set(-right);
	}

	/**
	 * Activates or deactivates the intake based on the left stick y-axis input.
	 */
	private void intake() {
		if (Math.abs(xbox.getRawAxis(1)) > 0.2) {
			intakeL = xbox.getRawAxis(1);
		
		} else {
			intakeL = 0;
		}

		if (Math.abs(xbox.getRawAxis(5)) > 0.2) {
			intakeR = xbox.getRawAxis(5);
		} else {
			intakeR = 0;
		}

		intakeMotorSet(intakeL, -intakeR);
	}

	/**
	 * Raises and lowers the intake to access the switch to score during teleop
	 * 
	 * A button raises, B button lowers
	 */
	private void moveIntake_Teleop() {
		// Goes up
		if (xbox.getRawAxis(2) > 0.7) {
			height = true;
			intakeVert.set(xbox.getRawAxis(2));
		}
		// Goes down
		else if (xbox.getRawButton(5) == true) {
			height = false;
			intakeVert.set(-.25);
		}
		// Maintains height
		else if (height) {
			intakeVert.set(.15);
		}
		// Stops motors if not needed
		else {
			intakeVert.set(0);
		}
	}

	/**
	 * Sets intake motor speed
	 * 
	 * @param i1
	 *            intake 1 motor speed
	 * @param i2
	 *            intake 2 motor speed
	 */
	private void intakeMotorSet(double i1, double i2) {
		intake1.set(i1);
		intake2.set(i2);
	}

	/*
	 * Sets hook & winch motor speed
	 */
	private void climb() {
		if (xbox.getXButton() == true) {
			hooker = 0.5;
		} else if (xbox.getAButton() == true) {
			hooker = -.75;
		} else {
			hooker = 0;
		}

		if (xbox.getRawAxis(3) > 0.7) {
			wench = xbox.getRawAxis(3);
		} else if (xbox.getRawButton(6) == true) {
			
		} else {
			wench = 0;
		}

		hook.set(hooker);
		winch.set(wench);
	}

}
