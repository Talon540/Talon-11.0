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

	final String baselineTim = "Baseline with timer (S)";

	final String switchMiddleEnc = "Switch with encoder (M)";
	final String switchRightEnc = "Switch with encoder(R)";
	final String baselineEnc = "Baseline with encoder";
	final String switchLeftEnc = "Switch with encoder (L)";

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
		chooser.addObject("Baseline with timer", baselineTim);

		chooser.addObject("Baseline with encoder (From Side Position)", baselineEnc);
		chooser.addObject("Switch with encoder (From Left Position)", switchLeftEnc);
		chooser.addObject("Switch with encoder (From Right Position)", switchRightEnc);
		chooser.addObject("Switch with encoder (From Middle Position)", switchMiddleEnc);

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
		enc1 = new Encoder(0, 1, true);
		enc1.setMaxPeriod(0.1);
		enc1.setMinRate(5);
		enc1.setDistancePerPulse(0.00078487); // 0.00078487 ft per pulse -> 10 ft per 12741 pulses
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
		frontLeft.setVoltageRamp(100);
		midLeft.setVoltageRamp(100);
		backLeft.setVoltageRamp(100);
		frontRight.setVoltageRamp(100);
		midRight.setVoltageRamp(100);
		backRight.setVoltageRamp(100);

		intake1.setVoltageRamp(100);
		intake2.setVoltageRamp(100);
		intakeVert.setVoltageRamp(100);
	}

	public void autonomousInit() {
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

		case baselineTim:
			if (counter == 0) {
				motorSet(-.5, -.5); // go forward to 2 secs
				Timer.delay(2);
				counter++;
			}
			if (counter == 1) {
				motorSet(0, 0);
			}
			break;

		case baselineEnc:
			if (counter == 0) {
				if (dist >= 11) { // 11 feet
					motorSet(0, 0);
					counter++;
				} else {
					motorSet(prop(11, dist) * -.6, prop(11, dist) * -.6);
				}
			}
			if (counter == 1) {
				motorSet(0, 0);
			}
			break;

		case switchMiddleEnc:
			if (FMS.charAt(0) == 'L') { // if the switch is left
				if (counter == 0) {
					if (dist >= 0.5) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(0.5, dist) * -.3), (prop(0.5, dist) * -.3));
					}
				}
				if (counter == 1) {
					if (angle <= -20) { // turns 20 degrees to the left
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(-20, angle) * .3), (prop(-20, angle) * -.3)); // or else keep turning
					}
				}
				if (counter == 2) {
					if (dist >= 12.1666666666667) { // 152 in
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(12.1666666666667, dist) * -.3), (prop(12.1666666666667, dist) * -.3));
					}
				}
				if (counter == 3) {
					if (angle >= 0) { // turn to face the switch
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(0, angle) * -.3), (prop(0, angle) * .3)); // or else keep turning
					}
				}
				// if (counter == 4) {
				// intakeVert.set(.5); // activates the switch elevator for 2 secs
				// Timer.delay(.2);
				// intakeVert.set(0); // then stops it
				// Timer.delay(.2);
				// intakeMotorSet(-1, -1); // spits out the cube for 2 secs
				// Timer.delay(2);
				// intakeMotorSet(0, 0); // then stops
				// }
			} else { // right hand switch
				if (counter == 0) {
					if (dist >= 0.5) { // move away from the wall so it can turn
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(0.5, dist) * -.3), (prop(0.5, dist) * -.3));
					}
				}
				if (counter == 1) {
					if (angle >= 20) { // turns 20 degrees to the right
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(20, angle) * .3), (prop(20, angle) * -.3)); // or else keep turning
					}
				}
				if (counter == 2) {
					if (dist >= 12.1666666666667) { // 152 in
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(12.1666666666667, dist) * -.3), (prop(12.1666666666667, dist) * -.3));
					}
				}
				if (counter == 3) {
					if (angle <= 0) { // turn to face the switch
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(0, angle) * -.3), (prop(0, angle) * .3)); // or else keep turning
					}
				}
				// if (counter == 4) {
				// intakeVert.set(.5); // activates the switch elevator for 2 secs
				// Timer.delay(.2);
				// intakeVert.set(0); // then stops it
				// Timer.delay(.2);
				// intakeMotorSet(-1, -1); // spits out the cube for 2 secs
				// Timer.delay(2);
				// intakeMotorSet(0, 0); // then stops
				// }
			}
			break;

		case switchLeftEnc:
			if (FMS.charAt(0) == 'L') {
				if (counter == 0) {
					if (dist >= 14) { // move forward 168 in
						motorSet(0, 0);
						enc1.reset();
						counter++;
					} else {
						motorSet((prop(14, dist) * -.3), (prop(14, dist) * -.3));
					}
				}
				if (counter == 1) { //turn to the switch
					if (angle >= 90) {
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet((prop(90, angle) * -.3), (prop(90, angle) * .3));
					}
				}
				if (counter == 2) { //move to the switch
					if (dist >= 2.66666666667) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(0, 0);
					}
				}
				//TODO: intake

			} else { // Switch on right side
				if (counter == 0) { // move past the switch
					if (dist >= 20) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(20, dist) * -.3), (prop(20, dist) * -.3));
					}

				}
				if (counter == 1) { // Turn to go down
					if (angle >= 90) {
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet((prop(90, angle) * .3), (prop(90, angle) * -.3));
					}
				}
				if (counter == 2) { // move behind the switch
					if (dist >= 18.1155833333333) {
						motorSet(0, 0);
						counter++;
						gyro.reset();
					} else {
						motorSet((prop(18.1155833333333, dist) * -.3), (prop(18.1155833333333, dist) * -.3));
					}
				}
				if (counter == 3) { // Turn to go back
					if (angle >= 90) {
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet((prop(90, angle) * .3), (prop(90, angle) * -.3));
					}
				}
				if (counter == 4) { // move for the third time
					if (dist >= 2.6666666666667) {
						motorSet(0, 0);
						counter++;
						gyro.reset();
					} else {
						motorSet((prop(2.6666666666667, dist) * -.3), (prop(2.6666666666667, dist) * -.3));
					}
				}
				if (counter == 5) { // Turn towards switch
					if (angle >= 90) {
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet((prop(90, angle) * .3), (prop(90, angle) * -.3));
					}
				}
				// TODO: Intake code
			}
			break;

		case switchRightEnc: // Right side switch
			if (FMS.charAt(0) == 'R') {
				if (counter == 0) {
					if (dist >= 14) { // move forward 168 in
						motorSet(0, 0);
						enc1.reset();
						counter++;
					} else {
						motorSet((prop(14, dist) * -.3), (prop(14, dist) * -.3));
					}
				}
				if (counter == 1) {
					if (angle <= -90) { //turn towards switch
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet((prop(-90, angle) * -.3), (prop(-90, angle) * .3));
					}
				}
				if (counter == 2) {
					if (dist >= 2.66666666667) { //move towards switch
						motorSet(0, 0);
						counter++;
					} else {
						motorSet(0, 0);
					}
				}

			} else { // Switch on left side
				if (counter == 0) { // move past switch
					if (dist >= 20) {
						motorSet(0, 0);
						counter++;
					} else {
						motorSet((prop(20, dist) * -.3), (prop(20, dist) * -.3));
					}

				}
				if (counter == 1) { // Turn to go down
					if (angle <= -90) {
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet((prop(-90, angle) * .3), (prop(-90, angle) * -.3));
					}
				}
				if (counter == 2) { // move behind switch
					if (dist >= 18.1155833333333) {
						motorSet(0, 0);
						counter++;
						gyro.reset();
					} else {
						motorSet((prop(18.1155833333333, dist) * -.3), (prop(18.1155833333333, dist) * -.3));
					}
				}
				if (counter == 3) { // Turn to go back
					if (angle <= -90) {
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet((prop(-90, angle) * .3), (prop(-90, angle) * -.3));
					}
				}
				if (counter == 4) { // move for the third time
					if (dist >= 2.6666666666667) {
						motorSet(0, 0);
						counter++;
						gyro.reset();
					} else {
						motorSet((prop(2.6666666666667, dist) * -.3), (prop(2.6666666666667, dist) * -.3));
					}
				}
				if (counter == 5) { // Turn towards the switch
					if (angle <= -90) {
						motorSet(0, 0);
						counter++;
						enc1.reset();
					} else {
						motorSet((prop(-90, angle) * .3), (prop(-90, angle) * -.3));
					}
				}
				// TODO: Intake code
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

	public static double prop(double target, double x) {
		if (x < target / 2.0) {
			return 1 - ((target - x) / target);
		} else {
			// Currently produces a motor speed of 0.06 when multiplied by the usual
			// motor speed constant of 0.3
			// TODO: change this if the motor constant changes
			double prop = (target - x) / target;
			return (prop < 0.2) ? 0.2 : prop;

		}
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
