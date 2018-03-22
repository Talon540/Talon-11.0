package org.usfirst.frc.team540.robot;

// TODO:
//Test Turning on a Carpet Surface
//Test Elevator (Timer and Encoder)
//Determine measurements for Middle Auto
//Test Middle Auto

import com.mindsensors.CANSD540;

import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.wpilibj.ADXRS450_Gyro;
//import edu.wpi.first.wpilibj.AnalogGyro;
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

	final String baselineTime = "Baseline with timer";
	final String switchTimeLeft = "Left Switch From Side with Timer";
	final String switchTimeRight = "Right Switch From Side with Timer";
	final String fallbackTimeLeft = "Fallback Switch with Timer (L)";
	final String fallbackTimeRight = "Fallback Switch with Timer (R)";

	final String baselineEnc = "Baseline with encoder";
	final String switchMiddleEnc = "Switch with encoder (M)";
	final String switchRightEnc = "Switch with encoder(R)";
	final String switchLeftEnc = "Switch with encoder (L)";
	final String fallbackLeft = "fallback switch/baseline (L)";
	final String fallbackRight = "fallback switch/baseline (R)";

	String autoSelected;
	SendableChooser<String> chooser = new SendableChooser<>();

	// motor, joy-stick, and controller fields
	CANSD540 frontLeft, frontRight, backLeft, backRight, midLeft, midRight, intake1, intake2, intakeVert, hook, winch;
	Joystick leftJoy, rightJoy;
	XboxController xbox;
	PowerDistributionPanel pdp;

	// Sensor fields
	Encoder enc1, enc2;
	ADXRS450_Gyro gyro;
	// AnalogGyro gyro;
	AnalogInput IR;

	// will be used in driveCode() to get current movement
	double left, right, intakeL, intakeR, hooker, wench;

	// sensor fields
	double angle, irDist, pulse, dist, liftPulse;

	double scale;

	boolean height, toggle;

	final static double TURN_CONSTANT = 0.5;

	final static double DRIVE_CONSTANT = -0.8;

	// FMS
	String FMS, enemyFMS;

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
		chooser.addObject("Switch with Encoder (From Left Position, NOT TESTED)", switchLeftEnc);
		chooser.addObject("Switch with Encoder (From Right Position, NOT TESTED)", switchRightEnc);
		chooser.addObject("Switch with Encoder (From Middle Position, NOT TESTED)", switchMiddleEnc);
		chooser.addObject("Fallback Switch (From Left Position w/ Sensors)", fallbackLeft);
		chooser.addObject("Fallback Switch (From Right Position w/ Sensors)", fallbackRight);

		SmartDashboard.putData("Auto choices", chooser);

		// wheel motors
		frontLeft = new CANSD540(6);
		midLeft = new CANSD540(7);
		backLeft = new CANSD540(8);
		frontRight = new CANSD540(3);
		midRight = new CANSD540(4);
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
		enc1 = new Encoder(0, 1, false);
		enc1.setMaxPeriod(0.1);
		enc1.setMinRate(5);
		// 0.00078487 ft per pulse -> 10 ft per 12741 pulses
		enc1.setDistancePerPulse(0.00078487);
		enc1.setSamplesToAverage(10);

		// intake lift encoder
		enc2 = new Encoder(2, 3);
		enc2.setMaxPeriod(0.1);
		enc2.setMinRate(5);
		enc2.setSamplesToAverage(10);

		// sensors
		gyro = new ADXRS450_Gyro();
		// gyro = new AnalogGyro(1);
		IR = new AnalogInput(0);

		// calibrates sensors
		//gyro.reset();
		//gyro.calibrate();
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
		midLeft.setVoltageRamp(100);
		backLeft.setVoltageRamp(100);
		frontRight.setVoltageRamp(100);
		midRight.setVoltageRamp(100);
		backRight.setVoltageRamp(100);

		intake1.setVoltageRamp(100);
		intake2.setVoltageRamp(100);
		intakeVert.setVoltageRamp(100);

		// Camera Server
		CameraServer server = CameraServer.getInstance();
		UsbCamera cam = server.startAutomaticCapture();
		cam.setResolution(320, 240);
		cam.setFPS(30);

		// pdp for debugging purposes
		pdp = new PowerDistributionPanel();

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

		/*
		 * ENEMY FMS DETECTION enemyFMS = FMS.replace('L', 'E'); enemyFMS =
		 * enemyFMS.replace('R', 'L'); enemyFMS = enemyFMS.replace('E', 'R');
		 * SmartDashboard.putString("Enemy Switch Side: ", enemyFMS);
		 */
		
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
		dist = Math.abs(enc1.getDistance());

		SmartDashboard.putNumber("Angle: ", angle);
		SmartDashboard.putNumber("IR Distance: ", irDist);
		SmartDashboard.putNumber("Pulse Count: ", pulse);
		SmartDashboard.putNumber("Distance Traveled: ", dist);
		SmartDashboard.putNumber("Auto Counter: ", counter);

		switch (autoSelected) {

		// MOTORS ARE BOTH INVERTED IN AUTO (no reason found, but it works)
		case baselineTime: // used in case encoders do not work; not accurate at
							// the moment
			if (counter == 0) {
				motorSet(-.5, -.5); // go forward to 2 secs
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
				motorSet(-.5, -.5); // go forward to 2 secs
				Timer.delay(2.8); // TODO: fine tune the time
				motorSet(0, 0);
				Timer.delay(1);
				//gyro.reset();
				counter++;
			}
			if (FMS.charAt(0) == 'L') {

				if (counter == 1) {
					if (angle >= 90) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(-propGyro(90, angle), propGyro(90, angle));
					}
				}
				if (counter == 2) {
					motorSet(-.5, -.5);
					Timer.delay(1);
					motorSet(0, 0);
					intakeVert.set(0.8);
					intakeMotorSet(1, 1);
					Timer.delay(2);
					intakeVert.set(0.15);
					intakeMotorSet(-1, -1);
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
				motorSet(-.5, -.5); // go forward to 2 secs
				Timer.delay(2.8); // TODO: fine tune the time
				motorSet(0, 0);
				Timer.delay(1);
				//gyro.reset();
				counter++;
			}
			if (FMS.charAt(0) == 'R') {

				if (counter == 1) {
					if (angle <= -90) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(propGyro(-90, angle), -propGyro(-90, angle));
					}
				}
				if (counter == 2) {
					motorSet(-.5, -.5);
					Timer.delay(1);
					motorSet(0, 0);
					intakeVert.set(0.8);
					intakeMotorSet(1, 1);
					Timer.delay(2);
					intakeVert.set(0.15);
					intakeMotorSet(-1, -1);
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
				motorSet(-.5, -.5); // go forward to 3 secs
				Timer.delay(3); 
				motorSet(0, 0);
				counter++;
			}
			if (counter == 1) {
				if (FMS.charAt(0) == 'L') {
					intakeVert.set(0.5);
					Timer.delay(2);
					intakeVert.set(0.15);
					intakeMotorSet(-1, -1);
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
				motorSet(-.5, -.5); // go forward to 3 secs
				Timer.delay(3); 
				motorSet(0, 0);
				counter++;
			}
			if (counter == 1) {
				if (FMS.charAt(0) == 'R') {
					intakeVert.set(0.5);
					Timer.delay(2);
					intakeVert.set(0.15);
					intakeMotorSet(-1, -1);
					Timer.delay(2);
					intakeMotorSet(0, 0);
					motorSet(-.6, -.6);
					Timer.delay(0.5);
					motorSet(0, 0);
				} else {
					motorSet(0, 0);
				}
				counter++;
			}
			break;

		case baselineEnc:
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
			break;

		case switchMiddleEnc:
			System.out.println(FMS.charAt(0));
			if (FMS.charAt(0) == 'L') { // if the switch is left
				if (counter == 0) {
					if (dist >= 2) { // move away from the wall so it can turn
										// by two feet
						motorSet(0, 0);
						//gyro.reset();
						counter++;
					} else {
						motorSet(-.5, -.5);
					}
				}
				if (counter == 1) {
					if (angle <= -45) { // turns 20 degrees to the left
						motorSet(0, 0);
						enc1.reset();
						counter++;
					} else {
						motorSet(propGyro(-45, angle), -propGyro(-45, angle));
					}
				}
				if (counter == 2) {
					if (dist >= 9) { // 9 feet
						motorSet(0, 0);
						//gyro.reset();
						counter++;
					} else {
						motorSet(prop(9, dist), prop(9, dist));
					}
				}
				if (counter == 3) {
					if (angle >= 45) { // turn to face the switch
						motorSet(0, 0);
						counter++;
					} else { // TODO: fine-tune the turning function
						motorSet(-propGyro(45, angle), propGyro(45, angle)); // or
						// else keep turning
					}
				}

				if (counter == 4) {
					intakeVert.set(.5);
					Timer.delay(.2);
					intakeVert.set(0.15);
					enc1.reset();
					counter++;
				}
				if (counter == 5) {
					if (dist >= 3.5) { // 3.5 feet
						motorSet(0, 0);
						intakeMotorSet(-1, -1);
						Timer.delay(2);
						intakeMotorSet(0, 0);
						intakeVert.set(0);
						counter++;
					} else {
						motorSet(prop(3.5, dist), prop(3.5, dist));
					}
				}
				if (counter == 6) {
					motorSet(0, 0);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
				}

			} else if (FMS.charAt(0) == 'R') { // right hand switch
				if (counter == 0) {
					if (dist >= 2) { // move away from the wall so it can turn
										// by two feet
						motorSet(0, 0);
						//gyro.reset();
						counter++;
					} else {
						motorSet(-.5, -.5);
					}
				}
				if (counter == 1) {
					if (angle >= 45) { // turns 45 degrees to the right
						motorSet(0, 0);
						enc1.reset();
						counter++;
					} else {
						motorSet(-propGyro(45, angle), propGyro(45, angle));
					}
				}
				if (counter == 2) {
					if (dist >= 9) { // 9 feet
						motorSet(0, 0);
						//gyro.reset();
						counter++;
					} else {
						motorSet(prop(9, dist), prop(9, dist));
					}
				}
				if (counter == 3) {
					if (angle <= -45) { // turn to face the switch
						motorSet(0, 0);
						counter++;
					} else { // TODO: fine-tune the turning function
						motorSet(propGyro(-45, angle), -propGyro(-45, angle)); // or
						// else keep turning
					}
				}

				if (counter == 4) {
					intakeVert.set(.5);
					Timer.delay(.2);
					intakeVert.set(0.15);
					enc1.reset();
					counter++;
				}
				if (counter == 5) {
					if (dist >= 3.5) { // 3.5 feet
						motorSet(0, 0);
						intakeMotorSet(-1, -1);
						Timer.delay(2);
						intakeMotorSet(0, 0);
						intakeVert.set(0);
						counter++;
					} else {
						motorSet(prop(3.5, dist), prop(3.5, dist));
					}
				}
				if (counter == 6) {
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

		case switchLeftEnc: // TODO: fine-tune the turning function
			if (FMS.charAt(0) == 'L') {
				if (counter == 0) {
					if (dist >= 14) { // move forward 168 in
						motorSet(0, 0);
						enc1.reset();
						//gyro.reset();
						counter++;
					} else {
						motorSet(prop(14, dist), prop(14, dist));
					}
				}
				if (counter == 1) { // turn right towards the switch
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
				// TODO: intake

			} else if (FMS.charAt(0) == 'R') { // Switch on right side
				// TODO: be careful of turning error accumulation
				if (counter == 0) { // move 235 in. past the switch
					if (dist >= 19.58333333333333333) {
						motorSet(0, 0);
						//gyro.reset();
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
						//gyro.reset();
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
						//gyro.reset();
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
				// TODO: Intake code
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
						//gyro.reset();
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
				// TODO: review for turning error accumulation
				if (counter == 0) { // move past switch
					if (dist >= 19.58333333333333333) {
						motorSet(0, 0);
						//gyro.reset();
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
						//gyro.reset();
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
						//gyro.reset();
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
				// TODO: Intake code
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
		case fallbackLeft:
			if (counter == 0) {
				if (dist >= 14) {
					motorSet(0, 0);
					//gyro.reset();
					counter++;
				} else {
					motorSet(prop(14, dist), prop(14, dist));
				}
			}
			if (FMS.charAt(0) == 'L') {
				if (counter == 1) {
					if (angle >= 90) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(-propGyro(90, angle), propGyro(90, angle));
					}
				}
				if (counter == 2) {
					intakeVert.set(.5);
					Timer.delay(.2);
					intakeVert.set(0.15);
					counter++;
				}
				if (counter == 3) {
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
				if (counter == 4) {
					motorSet(0, 0);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
				}
			}
			else
			{
				motorSet(0,0);
			}
			break;
		case fallbackRight:
			if (counter == 0) {
				if (dist >= 14) {
					motorSet(0, 0);
					//gyro.reset();
					counter++;
				} else {
					motorSet(prop(14, dist), prop(14, dist));
				}
			}
			if (FMS.charAt(0) == 'R') {
				if (counter == 1) {
					if (angle <= -90) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(propGyro(-90, angle), -propGyro(-90, angle));
					}
				}
				if (counter == 2) {
					intakeVert.set(.5);
					Timer.delay(.2);
					intakeVert.set(0.15);
					enc1.reset();
					counter++;
				}
				if (counter == 3) {
					if (dist >= 2) {
						motorSet(0, 0);
						intakeMotorSet(-1, -1);
						Timer.delay(2);
						intakeMotorSet(0, 0);
						intakeVert.set(0);
						counter++;
					} else {
						motorSet(prop(2, dist), prop(2, dist));
					}
				}
				if (counter == 4) {
					motorSet(0, 0);
					intakeMotorSet(0, 0);
					intakeVert.set(0);
				}
			}
			else
			{
				motorSet(0,0);
			}
			break;

		case defaultAuto:
			if (counter == 0)
			{
				//This sequence for spinning intake motors works
				Timer.delay(1);
				intakeMotorSet(-1,-1);
				Timer.delay(1);
				counter++;
			}
			if (counter == 1)
			{
				intakeMotorSet(0,0);
			}
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
		liftPulse = enc2.get();

		double currentZero = pdp.getCurrent(0);
		double currentOne = pdp.getCurrent(1);
		double currentTwo = pdp.getCurrent(2);
		double currentThree = pdp.getCurrent(3);
		double currentFour = pdp.getCurrent(4);
		double currentFive = pdp.getCurrent(5);
		double currentSix = pdp.getCurrent(6);
		double currentSeven = pdp.getCurrent(7);
		double currentEight = pdp.getCurrent(8);
		double currentNine = pdp.getCurrent(9);
		double currentTen = pdp.getCurrent(10);
		double currentEleven = pdp.getCurrent(11);
		double currentTwelve = pdp.getCurrent(12);
		double currentThirteen = pdp.getCurrent(13);
		double currentFourteen = pdp.getCurrent(14);
		double currentFifteen = pdp.getCurrent(15);
		double currentSum = currentZero + currentOne + currentTwo + currentThree + currentFour + currentFive
				+ currentSix + currentSeven + currentEight + currentNine + currentTen + currentEleven + currentTwelve
				+ currentThirteen + currentFourteen + currentFifteen;

		SmartDashboard.putNumber("Angle: ", angle);
		SmartDashboard.putNumber("IR Distance: ", irDist);
		SmartDashboard.putNumber("Pulse Count: ", pulse);
		SmartDashboard.putNumber("Distance Traveled: ", dist);
		SmartDashboard.putNumber("Lift Pulse Count", liftPulse);

		/*
		 * SmartDashboard.putNumber("PDP Channel 0: ", currentZero);
		 * SmartDashboard.putNumber("PDP Channel 1: ", currentOne);
		 * SmartDashboard.putNumber("PDP Channel 2: ", currentTwo);
		 * SmartDashboard.putNumber("PDP Channel 3: ", currentThree);
		 * SmartDashboard.putNumber("PDP Channel 4: ", currentFour);
		 * SmartDashboard.putNumber("PDP Channel 5: ", currentFive);
		 * SmartDashboard.putNumber("PDP Channel 6: ", currentSix);
		 * SmartDashboard.putNumber("PDP Channel 7: ", currentSeven);
		 * SmartDashboard.putNumber("PDP Channel 8: ", currentEight);
		 * SmartDashboard.putNumber("PDP Channel 9: ", currentNine);
		 * SmartDashboard.putNumber("PDP Channel 10: ", currentTen);
		 * SmartDashboard.putNumber("PDP Channel 11: ", currentEleven);
		 * SmartDashboard.putNumber("PDP Channel 12: ", currentTwelve);
		 * SmartDashboard.putNumber("PDP Channel 13: ", currentThirteen);
		 * SmartDashboard.putNumber("PDP Channel 14: ", currentFourteen);
		 * SmartDashboard.putNumber("PDP Channel 15: ", currentFifteen);
		 * SmartDashboard.putNumber("Total PDP Current: ", currentSum);
		 */

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
			return ((target - currentGyro) / target);
		} else {
			return 0.2;
		}

	}

	/**
	 * Used in drive code
	 * 
	 * Sets current positions of joysticks as values to left and right
	 */
	private void drive() {
		// if (leftJoy.getRawButton(1) == true) {
		if (leftJoy.getRawButton(1) == true && leftJoy.getRawButton(3) == true) {
			/*
			 * if (leftJoy.getRawButton(3)) reverse *= -1; else if
			 * (leftJoy.getRawButton(2)) toggle = !toggle;
			 */
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
		midLeft.set(left);
		backLeft.set(left);

		frontRight.set(-right);
		midRight.set(-right);
		backRight.set(-right);
	}

	/**
	 * Activates or deactivates the intake based on the left stick y-axis input.
	 */
	private void intake() {
		if (Math.abs(xbox.getRawAxis(1)) > 0.2) {
			intakeL = xbox.getRawAxis(1);
			// intakeR = xbox.getRawAxis(1);
		} else {
			intakeL = 0;
			// intakeR = 0;
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
			// xbox.getXButton() == true
			height = true;
			intakeVert.set(xbox.getRawAxis(2));
		}
		// Goes down
		else if (xbox.getRawButton(5) == true) {
			// xbox.getAButton() == true
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
			// hooker = xbox.getRawAxis(2);
			hooker = 0.5;
		} else if (xbox.getAButton() == true) {
			// xbox.getRawButton(5) == true
			hooker = -.75;
		} else {
			hooker = 0;
		}

		if (xbox.getRawAxis(3) > 0.7) {
			wench = xbox.getRawAxis(3);
		} else if (xbox.getRawButton(6) == true) {
			// wench = -.5;
		} else {
			wench = 0;
		}

		hook.set(hooker);
		winch.set(wench);
	}

}
