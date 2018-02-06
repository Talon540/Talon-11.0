package org.usfirst.frc.team540.robot;

import com.mindsensors.CANSD540;

import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends IterativeRobot {
	final String defaultAuto = "Default";
	final String baselineSide = "Baseline (S)";
	final String baselineMiddle = "Baseline (M)";
	final String switchLeft = "Switch (L)";
	final String switchMiddle = "Switch (M)";
	final String switchRight = "Switch (R)";

	String autoSelected;
	SendableChooser<String> chooser = new SendableChooser<>();

	// motor, joy-stick, and controller fields
	CANSD540 frontLeft, frontRight, backLeft, backRight, midLeft, midRight, intake1, intake2, intakeVert, hook, winch;
	Joystick leftJoy, rightJoy;
	XboxController xbox;

	// Sensor fields
	Encoder enc1;
	ADXRS450_Gyro gyro;
	AnalogInput IR;

	// will be used in driveCode() to get current movement
	double yLeft, yRight, intakeL, intakeR, hooker, wench;

	// sensor fields
	double angle, irDist, pulse, dist;

	// FMS
	String FMS, enemyFMS;

	// Counter for Auto
	int counter;

	@Override
	public void robotInit() {
		chooser.addDefault("Default (Do Nothing)", defaultAuto);
		chooser.addObject("Baseline (From Side Position)", baselineSide);
		chooser.addObject("Baseline (From Middle Position)", baselineMiddle);
		chooser.addObject("Switch (From Left Position)", switchLeft);
		chooser.addObject("Switch (From Middle Position)", switchMiddle);
		chooser.addObject("Switch (From Right Position)", switchRight);

		SmartDashboard.putData("Auto choices", chooser);

		// wheel motors
		frontLeft = new CANSD540(6);
		midLeft = new CANSD540(7);
		backLeft = new CANSD540(8);
		frontRight = new CANSD540(3);
		midRight = new CANSD540(4);
		backRight = new CANSD540(5);

		// intake motors
		intake1 = new CANSD540(9);
		intake2 = new CANSD540(10);
		intakeVert = new CANSD540(11);

		// climber and winch
		hook = new CANSD540(12);
		winch = new CANSD540(13);

		// joystick and controller motors
		leftJoy = new Joystick(0);
		rightJoy = new Joystick(1);
		xbox = new XboxController(2);

		// encoders
		enc1 = new Encoder(0, 1);
		enc1.setMaxPeriod(0.1);
		enc1.setMinRate(5);
		enc1.setDistancePerPulse(4);
		enc1.setSamplesToAverage(10);

		// sensors
		gyro = new ADXRS450_Gyro();
		IR = new AnalogInput(0);

		// calibrates sensors
		gyro.reset();
		gyro.calibrate();
		enc1.reset();

		// initializes counter
		counter = 0;

		// initialize voltage ramps
		frontLeft.setVoltageRamp(150);
		midLeft.setVoltageRamp(150);
		backLeft.setVoltageRamp(150);
		frontRight.setVoltageRamp(150);
		midRight.setVoltageRamp(150);
		backRight.setVoltageRamp(150);

		intake1.setVoltageRamp(150);
		intake2.setVoltageRamp(150);
		intakeVert.setVoltageRamp(150);
	}

	public void autonomousInit() {
    		autoSelected = (String) chooser.getSelected();
		System.out.println("Auto selected: " + autoSelected);

		// calibrates sensors for auto?? Figure this out later
		gyro.reset();
		gyro.calibrate();
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

		// FMS
		FMS = DriverStation.getInstance().getGameSpecificMessage();
		SmartDashboard.putString("Our Switch Side: ", FMS);

		/*
		 * ENEMY FMS DETECTION enemyFMS = FMS.replace('L', 'E'); enemyFMS =
		 * enemyFMS.replace('R', 'L'); enemyFMS = enemyFMS.replace('E', 'R');
		 * SmartDashboard.putString("Enemy Switch Side: ", enemyFMS);
		 */

		switch (autoSelected) {

		case baselineSide:
			motorSet(.5, .5); //go forward to 2 secs
			Timer.delay(2);
			motorSet(0, 0);
			break;

		case baselineMiddle:
			if (counter == 0) {
				motorSet(.1, .1); //go forward for .5 secs
				Timer.delay(.5);
				counter++;
			}
			if (counter == 1) {
				if (angle >= 45) { //turn right until it reaches 45 
					motorSet(0, 0);
					counter++;
				} else {
					motorSet((prop(45, angle) * .3), (prop(45, angle)) * -.3);
					//or else keep turning
				}
			}
			if (counter == 2) {
				motorSet(.5, .5); //go forward for 4 secs
				Timer.delay(4);
				motorSet(0, 0);
			}
			break;

		case switchMiddle:
			if (FMS.charAt(0) == 'L') { //if the switch is left
				if (counter == 0) {
					motorSet(.1, .1); //go forward for .2 secs
					Timer.delay(.2);
					motorSet(0, 0); //stops the robot
					counter++;
				}
				if (counter == 1) {
					if (angle <= -20) { //turns 20 degrees to the left
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(-20, angle) * -.3), (prop(-20, angle) * .3)); 
						//or else keep turning
					}
				}
				if (counter == 2) {
					motorSet(.5, .5); //go forward for 2 seconds
					Timer.delay(2);
					motorSet(0, 0); //then stop
					counter++;
				}
				if (counter == 3) {
					if (angle >= 0) { //turn to face the switch
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(0, angle) * .3), (prop(0, angle) * -.3)); //or else keep turning
					}
				}
				if (counter == 4) {
					intakeVert.set(.5); //activates the switch elevator for 2 secs
					Timer.delay(.2);
					intakeVert.set(0); //then stops it
					Timer.delay(.2);
					intakeMotorSet(-1, -1); //spits out the cube for 2 secs
					Timer.delay(2);
					intakeMotorSet(0, 0); //then stops
				}
			} else {
				if (counter == 0) { //if the switch is right
					motorSet(.1, .1); //go forward for 2 secs
					Timer.delay(.2);
					motorSet(0, 0); //then stop
					counter++;
				}
				if (counter == 1) {
					if (angle >= 20) { //turn to the right
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(20, angle) * .3), (prop(20, angle) * -.3));
						//else keep turning
					}
				}
				if (counter == 2) {
					motorSet(.5, .5); // go forward for 2 secs
					Timer.delay(2);
					motorSet(0, 0);
					counter++;
				}
				if (counter == 3) {
					if (angle <= 0) { //turn to face the switch again
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(0, angle) * -.3), (prop(0, angle) * .3));
						//or else keep turning
					}
				}
				if (counter == 4) {
					intakeVert.set(.5); //activates the switch elevator for 2 secs
					Timer.delay(.2);
					intakeVert.set(0); //then stops it
					Timer.delay(.2);
					intakeMotorSet(-1, -1); //spits the cube for 2 secs
					Timer.delay(2);
					intakeMotorSet(0, 0); //then stops
				}
			}
			break;

		case switchLeft: // TODO: prevent auto collisions
			if (FMS.charAt(0) == 'L') {
				motorSet(.1, .1);
				Timer.delay(1); // TODO: figure out the actual delay
				motorSet(0, 0);

			} else { // Switch on right side
				if (counter == 0) {
					if (angle >= 45) { // Turn towards switch
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(45, angle) * .3), (prop(45, angle) * -.3));
					}
				}
				if (counter == 1) { // Move across field
					motorSet(.5, .5);
					Timer.delay(2);
					motorSet(0, 0);
					counter++;
				}
				if (counter == 2) { // Turn to place on switch
					if (angle <= 0) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(0, angle) * -.3), (prop(0, angle) * .3));
					}
				}
				if (counter == 3) { // place on switch
					intakeVert.set(.5);
					Timer.delay(.2);
					intakeVert.set(0);
					Timer.delay(.2);
					intakeMotorSet(-1, -1);
					Timer.delay(.2);
					intakeMotorSet(0, 0);
				}
			}
			break;

		case switchRight: // TODO: prevent auto collisions
			if (FMS.charAt(0) == 'R') { // switch on right side
				motorSet(.1, .1);
				Timer.delay(1); // TODO: figure out the actual delay
				motorSet(0, 0);

			} else { // Switch on left side
				if (counter == 0) {
					if (angle >= -45) { // Turn towards switch
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(-45, angle) * -.3), (prop(-45, angle) * .3));
					}
				}
				if (counter == 1) { // Move across field
					motorSet(.5, .5);
					Timer.delay(2);
					motorSet(0, 0);
					counter++;
				}
				if (counter == 2) { // Turn to place on switch
					if (angle <= 0) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(0, angle) * .3), (prop(0, angle) * -.3));
					}
				}
				if (counter == 3) { // place on switch
					intakeVert.set(.5);
					Timer.delay(.2);
					intakeVert.set(0);
					Timer.delay(.2);
					intakeMotorSet(-1, -1);
					Timer.delay(.2);
					intakeMotorSet(0, 0);
				}
			}
			break;

		case defaultAuto:
			motorSet(0, 0);
			break;
		}
	}

	//PID Functions
	public static double prop(double target, double x) {
		return (target - x) / target;
	}

	@Override
	public void teleopPeriodic() {
		// gets sensor values and displays them in SmartDashboard
		angle = gyro.getAngle();
		irDist = IR.getVoltage();
		pulse = enc1.get();
		dist = enc1.getDistance();

		SmartDashboard.putNumber("Angle: ", angle);
		SmartDashboard.putNumber("IR Distance: ", irDist);
		SmartDashboard.putNumber("Pulse Count: ", pulse);
		SmartDashboard.putNumber("Distance Traveled: ", dist);

		// calls drive() to drive
		drive();

		// calls intake() to deal with cube manipulation
		intake();

		// calls switch() to raise the intake to switch height
		switcher();

		// calls climb() to deploy hook and pull in the winch
		climb();
	}

	// drive code
	private void drive() {
		// drive code

		// set current positions of joysticks as values to yLeft and yRight
		if (Math.abs(leftJoy.getY()) > 0.1) {
			yLeft = leftJoy.getY();
		} else {
			yLeft = 0;
		}
		if (Math.abs(rightJoy.getY()) > 0.1) {
			yRight = rightJoy.getY();
		} else {
			yRight = 0;
		}

		motorSet(yLeft, yRight);
	}

	// sets drivetrain motor speed
	private void motorSet(double x, double y) {
		frontLeft.set(x);
		midLeft.set(x);
		backLeft.set(x);

		frontRight.set(-y);
		midRight.set(-y);
		backRight.set(-y);
	}

	// intake code
	private void intake() {
		if (xbox.getRawAxis(1) > 0.7) {
			intakeL = xbox.getRawAxis(1);
			intakeR = xbox.getRawAxis(1);
		} else {
			intakeL = 0;
			intakeR = 0;
		}

		intakeMotorSet(intakeL, intakeR);
	}

	// raises and lowers intake for switch
	private void switcher() {
		// Goes up
		if (xbox.getXButton() == true) {
			intakeVert.set(.5);
		} else {
			intakeVert.set(0);
		}

		// Goes down
		if (xbox.getAButton() == true) {
			intake1.set(-0.5);
			intake2.set(-0.5);
		} else {
			intake1.set(0);
			intake2.set(0);
		}
	}

	// sets intake motor speed
	private void intakeMotorSet(double x, double y) {
		intake1.set(x);
		intake2.set(y);
	}

	// sets hook & winch motor speed
	private void climb() {
		if (xbox.getRawAxis(5) > 0.7) {
			hooker = xbox.getRawAxis(5);
		} else {
			hooker = 0;
		}
		if (xbox.getRawAxis(3) > 0.7) {
			wench = xbox.getRawAxis(3);
		} else {
			wench = 0;
		}

		hook.set(hooker);
		winch.set(wench);
	}

}
