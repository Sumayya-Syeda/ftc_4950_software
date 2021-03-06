package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.GyroSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

import java.io.InterruptedIOException;

/**
 * Created by justi on 2018-01-03.
 * this class is used by NewAutonomous for simpler movement and hardware control
 * implement the following methods as described in the comments
 */

public class NewAutonomousDriver {
    private LinearOpMode opMode;
    RelicRecoveryVuMark vuMark;

    double wheel_diameter = 3.54331; //the diameter of the wheels of our robot.
    double wheel_circumference = Math.PI * wheel_diameter; //the value of π times the wheel diameter
    int ticksPerRevolution = (1120 * 60)/96; //the amount of ticks the encoder takes to revolve one wheel
    double armWaiting = 2.0;
    public DcMotor leftMotor = null;
    public DcMotor rightMotor = null;
    public DcMotor armMotor;
    ColorSensor colorSensor;
    GyroSensor gyro;
    VuforiaLocalizer vuforia;
    CRServo clampServo;
    Servo jewelServo;
    DigitalChannel ARM_TOUCH_OPEN;
    DigitalChannel ARM_TOUCH_CLOSED;

    VuforiaTrackables relicTrackables;
    VuforiaTrackable relicTemplate;

    HardwareMap hwMap  = null;
    private ElapsedTime period  = new ElapsedTime();
    //TODO declaration of motors, servos, sensors, etc.

    public NewAutonomousDriver(HardwareMap hardwareMap, LinearOpMode opMode) {
        // save reference to HW Map
        this.opMode = opMode;
        hwMap = hardwareMap;
        leftMotor = hardwareMap.dcMotor.get("leftMotor");
        rightMotor = hardwareMap.dcMotor.get("rightMotor");
        armMotor = hardwareMap.dcMotor.get("armMotor");
        gyro = hardwareMap.gyroSensor.get("gyro");
        clampServo = hardwareMap.crservo.get("clampServo");
        colorSensor = hardwareMap.colorSensor.get("jewelColor");
        jewelServo = hardwareMap.servo.get("jewelServo");
        ARM_TOUCH_OPEN = hardwareMap.get(DigitalChannel.class, "tsOpen");
        ARM_TOUCH_CLOSED = hardwareMap.get(DigitalChannel.class, "tsClosed");
        ARM_TOUCH_OPEN.setMode(DigitalChannel.Mode.INPUT);
        ARM_TOUCH_CLOSED.setMode(DigitalChannel.Mode.INPUT);

        rightMotor.setDirection(DcMotorSimple.Direction.REVERSE);

        gyro.calibrate();

        while(gyro.isCalibrating()){

        }

        int cameraMonitorViewId = opMode.hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", opMode.hardwareMap.appContext.getPackageName());
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);

        parameters.vuforiaLicenseKey = "AWnZ5xz/////AAAAGYmbM16TXEdKscTtfaECY6FzIRnxfc6SV0uwUV+dwPVIWbGyu9567BTp2qzh6ohnawdFrbL290ECRr04ew/QX0Q90SUrGh52+s55yVFPN429A93YJm6AlnV/TEJKb8omxdlqC+Hfy0SLPZSu+UEq9xQMOIfeW+OiRNQyFlUTZNCtQDNuK5jwObgulF83zrexs+c95Cd1jU7PnoX+NgHPjmUWS5H+WVr4yZsewES+oa0jRjGrcGU0/P5USRnqVbKh4976SNjPBGy6fanxJZmQb2Pam56UROtERcdaPDSWg4Nrr0MFlHCvi3PcfyLfdPtBW06JZGWBXu23VJCBQFw3SxGm/IO057P4kbTFti3W5xkU";

        parameters.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;
        this.vuforia = ClassFactory.createVuforiaLocalizer(parameters);

        relicTrackables = this.vuforia.loadTrackablesFromAsset("RelicVuMark");
        relicTemplate = relicTrackables.get(0);
        relicTemplate.setName("relicVuMarkTemplate"); // can help in debugging; otherwise not necessary

        opMode.telemetry.addData(">", "Press Play to start");
        opMode.telemetry.update();

        opMode.waitForStart();

        relicTrackables.activate();
    }

    /**
     * turns the robot with the gyro
     *
     * @param degrees turns the robot by this angle; positive is clockwise, negative is counterclockwise
     */
    public void turn(int degrees) {
        if(degrees == 0) return;
        leftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        double currentHeading = gyro.getHeading();
        double target = (currentHeading + degrees) % 360;

        while (opMode.opModeIsActive() && !((gyro.getHeading()<degrees+5) && (gyro.getHeading()>degrees-5))) {
            double speed = 0.3 + (0.2 * Math.abs((gyro.getHeading() - target) / degrees));
            rightMotor.setPower((degrees < 0 ? speed : -speed));
            leftMotor.setPower((degrees < 0 ? -speed : speed));
            opMode.telemetry.addData("Heading:", gyro.getHeading());
            opMode.telemetry.update();
        }

        leftMotor.setPower(0);
        rightMotor.setPower(0);
        opMode.telemetry.addData("Final heading", gyro.getHeading());
        opMode.telemetry.update();
    }

    /**
     * drives the robot using encoders
     *
     * @param inches drives this many inches; positive is forwards, negative is backwards
     */
    public void drive(double inches) {
        while (opMode.opModeIsActive())
        {
            int encoderTicks = (int) ((inches/wheel_circumference) * ticksPerRevolution);

            leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

            leftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            rightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

            leftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            rightMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            leftMotor.setTargetPosition(encoderTicks);
            rightMotor.setTargetPosition(encoderTicks);

            leftMotor.setPower(0.5);
            rightMotor.setPower(0.5);

            while (leftMotor.getCurrentPosition() < leftMotor.getTargetPosition() && rightMotor.getCurrentPosition() < rightMotor.getTargetPosition() /* && opMode.opModeIsActive()*/) {

            }

            leftMotor.setPower(0);
            rightMotor.setPower(0);

            leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        }
    }

    public void gyroEncoders (double degrees) {
        int ticks = ticksPerRevolution;

        leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        leftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        leftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        leftMotor.setTargetPosition((int)degrees*ticks);
        rightMotor.setTargetPosition((int)-degrees*ticks);
        if(degrees > 0) {
            leftMotor.setPower(0.5);
            rightMotor.setPower(-0.5);
        }else{
            leftMotor.setPower(-0.5);
            rightMotor.setPower(0.5);
        }

        while (leftMotor.getCurrentPosition() < leftMotor.getTargetPosition() && rightMotor.getCurrentPosition() > rightMotor.getTargetPosition() && opMode.opModeIsActive()) {
            opMode.telemetry.addData(">" , gyro.getHeading());
            opMode.telemetry.update();
        }

        leftMotor.setPower(0);
        rightMotor.setPower(0);

        leftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    }

    /**
     * opens the clamp by running the clamp servo until the open touch sensor is pressed
     */
    public void openClamp() {
        while (opMode.opModeIsActive())
        {
            clampServo.setDirection(DcMotorSimple.Direction.REVERSE);
            clampServo.setPower(1);
            while (!ARM_TOUCH_OPEN.getState() && opMode.opModeIsActive()) {

            }
            clampServo.setPower(0);
            clampServo.setDirection(DcMotorSimple.Direction.FORWARD);
        }
    }

    /**
     * sets the jewel arm position
     *
     * @param position the position to set the jewel arm to; 0 is retracted, 1 is lowered
     */
    public void setJewelArmPosition(double position) {
        jewelServo.setPosition(position);
    }

    /**
     * reads the colour of the jewel from the colour sensor
     *
     * @return -1 for a blue jewel, 1 for a red jewel
     */
    public int getJewelColour(){
        //setJewelArmPosition(1);
        //wait(1000);
        double blueColor = colorSensor.blue();
        double redColor = colorSensor.red();
        return (blueColor > redColor ? -1 : 1);
    }

    /**
     * uses vuforia to read the vision target to determine which column to place the glyph in
     *
     * @return 0 for the left column, 1 for the centre column, 2 for the right column, 3 for unknown
     */
    public int getTargetColumn() {
        while (opMode.opModeIsActive()) {
            relicTemplate = relicTrackables.get(0);

            vuMark = RelicRecoveryVuMark.from(relicTemplate);

            if (vuMark != RelicRecoveryVuMark.UNKNOWN) {
                opMode.telemetry.addData("VuMark", vuMark);

                break;

            } else {
                opMode.telemetry.addData("VuMark", "not visible");
            }
        }

        if (vuMark == RelicRecoveryVuMark.CENTER) {
            return 1;
        } else if (vuMark == RelicRecoveryVuMark.RIGHT) {
            return 2;
        } else if (vuMark == RelicRecoveryVuMark.LEFT) {
            return 0;
        } else {
            return 3;
        }
    }
}