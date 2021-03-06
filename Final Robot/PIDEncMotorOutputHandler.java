package org.usfirst.frc.team540.robot;

import com.mindsensors.CANSD540;

import edu.wpi.first.wpilibj.PIDOutput;

public class PIDEncMotorOutputHandler implements PIDOutput {
	private CANSD540 frontLeft, midLeft, backLeft, frontRight, midRight, backRight;
	double output;

	public PIDEncMotorOutputHandler(CANSD540 frontLeft, CANSD540 midLeft, CANSD540 backLeft, CANSD540 frontRight,
			CANSD540 midRight, CANSD540 backRight) {
		this.frontLeft = frontLeft;
		this.midLeft = midLeft;
		this.backLeft = backLeft;
		this.frontRight = frontRight;
		this.midRight = midRight;
		this.backRight = backRight;
		this.output = 0;
	}

	@Override
	/**
	 * Writes the output from the PIDController, and sets the motor speeds.
	 * 
	 * @param output
	 *            the output from the PIDController
	 */
	public void pidWrite(double output) {
		this.output = output;
		motorSet(output, output);
	}

	/**
	 * Sets motor speed values. Positive values (should) go forward, negative
	 * backward.
	 * 
	 * @param left
	 *            - left motor speed
	 * @param right
	 *            - right motor speed
	 */
	private void motorSet(double left, double right) {
		frontLeft.set(-left);
		midLeft.set(-left);
		backLeft.set(-left);

		frontRight.set(-right);
		midRight.set(-right);
		backRight.set(-right);
		
		System.out.println(left + ", " + right);
	}

}
