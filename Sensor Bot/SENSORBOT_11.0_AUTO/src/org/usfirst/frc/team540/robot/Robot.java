package org.usfirst.frc.team540.robot;

import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends IterativeRobot {
	final String defaultAuto = "defaultAuto";
	final String baselineAuto = "Baseline";
	final String baselineLeft = "Baseline with Left Position";
	final String baselineMiddle = "Baseline with Middle Position";
	final String baselineRight = "Baseline with Right Position";
	final String switchAuto = "Switch";
	String autoSelected;

	SendableChooser chooser;

	// motor and joy-stick fields
	Talon frontLeft, frontRight, backLeft, backRight;
	Joystick leftJoy, rightJoy;
	Encoder encoder1;

	// will be used in teleop periodic to get current movement
	double yLeft, yRight;
	double dEncoder;
	ADXRS450_Gyro gyro;

	@Override
	public void robotInit() {
		chooser = new SendableChooser();
		chooser.addObject("Baseline Left", baselineLeft);
		chooser.addObject("Baseline Middle", baselineMiddle);
		chooser.addObject("Baseline Right", baselineRight);
		chooser.addObject("Switch", switchAuto);
		chooser.addDefault("Default Auto", defaultAuto);
		SmartDashboard.putData("Auto choices", chooser);

		// assigning motors to the created fields
		frontLeft = new Talon(3);
		frontRight = new Talon(1);
		backLeft = new Talon(4);
		backRight = new Talon(2);

		leftJoy = new Joystick(0);
		rightJoy = new Joystick(1);

		// gyroscope
		gyro = new ADXRS450_Gyro();

		// encoders
		//encoder1 = new Encoder(0, 1, false, Encoder.EncodingType.k4X);
		encoder1 = new Encoder(0,1);
		encoder1.setMaxPeriod(0.1);
		encoder1.setMinRate(5);
		encoder1.setDistancePerPulse(4);
		encoder1.setSamplesToAverage(10);

		double gyroValue = gyro.getAngle();
		// Outputs gyro values to the dashboard
		SmartDashboard.putNumber("Gyro: ", gyroValue);

		// Outputs encoder values to the Smart Dashboard
		SmartDashboard.putNumber("Samples to Avergae: ", encoder1.getSamplesToAverage());
		SmartDashboard.putNumber("Distance per Pulse: ", encoder1.getDistance());
		SmartDashboard.putNumber("Current Rate: ", encoder1.getRate());

	}

	public void autonomousInit() {
		System.out.println("Auto selected: " + autoSelected);
	}

	@Override
	public void autonomousPeriodic() {
		autoSelected = (String) chooser.getSelected();
		

		// Outputs encoder values to the Smart Dashboard
	/*	SmartDashboard.putNumber("Samples to Avergae: ", encoder1.getSamplesToAverage());
		SmartDashboard.putNumber("Distance per Pulse: ", encoder1.getDistance());
		SmartDashboard.putNumber("Current Rate: ", encoder1.getRate());
*/
		switch (autoSelected) {
		case switchAuto:
			// Put custom auto code here
			break;

		case baselineLeft:

			// Move forward 10ft across baseline
			// Encoders determine distance
			frontRight.set(1);
			backRight.set(1);
			frontLeft.set(1);
			backLeft.set(1);
			// TODO: actually test the encoder values - needs to go 10 ft+
			if (encoder1.get() <= 10) {
				frontRight.set(1);
				backRight.set(1);
				frontLeft.set(1);
				backLeft.set(1);
			} else {
				frontRight.set(0);
				backRight.set(0);
				frontLeft.set(0);
				backLeft.set(0);
			}
			break;

		case baselineMiddle:
			// Move forward 10ft across baseline and
			// Gyro determine angle and Encoders determine distance

			// TODO: actually test the encoder values - needs to go 10 ft+
			// TODO: test the gyro

			// Turns the bot to the left
			frontRight.set(.5);
			backRight.set(.5);
			frontLeft.set(-.5);
			backLeft.set(-.5);

			// if the bot is a certain angle then move forward
			if (gyro.getAngle() >= 60 && gyro.getAngle() <= 65) {
				frontRight.set(1);
				backRight.set(1);
				frontLeft.set(1);
				backLeft.set(1);
			}
			// else keep turning
			else {
				frontRight.set(.5);
				backRight.set(.5);
				frontLeft.set(-.5);
				backLeft.set(-.5);
			}

			// when bot reaches a distance of 10 ft, then stop
			// TODO: test the encoder
			if (encoder1.get() <= 10) {
				frontRight.set(1);
				backRight.set(1);
				frontLeft.set(1);
				backLeft.set(1);
			} else {
				frontRight.set(0);
				backRight.set(0);
				frontLeft.set(0);
				backLeft.set(0);
			}

			break;

		case baselineRight:

			// Move forward 10ft across baseline
			// Encoders determine distance
			frontRight.set(1);
			backRight.set(1);
			frontLeft.set(1);
			backLeft.set(1);
			// TODO: actually test the encoder values - needs to go 10 ft+
			if (encoder1.get() <= 10) {
				frontRight.set(1);
				backRight.set(1);
				frontLeft.set(1);
				backLeft.set(1);
			} else {
				frontRight.set(0);
				backRight.set(0);
				frontLeft.set(0);
				backLeft.set(0);
			}
			break;
		default:
			frontRight.set(0);
			backRight.set(0);
			frontLeft.set(0);
			backLeft.set(0);

			break;
		}
	}

	@Override
	public void teleopPeriodic() {

		// set current positions of joy sticks as values to yLeft and yRight
		yLeft = leftJoy.getY();
		yRight = rightJoy.getY();

		

		// Outputs encoder values to the Smart Dashboard
		/*SmartDashboard.putNumber("Samples to Avergae: ", encoder1.getSamplesToAverage());
		SmartDashboard.putNumber("Distance per Pulse: ", encoder1.getDistance());
		SmartDashboard.putNumber("Current Rate: ", encoder1.getRate());
*/
		// encoder1.setMaxPeriod(0.1);
		// encoder1.setMinRate(5);
		// encoder1.setDistancePerPulse(4);
		// encoder1.setSamplesToAverage(10);

		// TODO: 0.3 is the dead-zone - fine tune this to the bot
		if (Math.abs(yLeft) < 0.3) {
			frontLeft.set(0);
			backLeft.set(0);
		} else {
			frontLeft.set(-yLeft);
			backLeft.set(-yLeft);
		}

		if (Math.abs(yRight) < 0.3) {
			frontRight.set(0);
			backRight.set(0);
		} else {
			frontRight.set(yRight);
			backRight.set(yRight);
		}
	}
}
