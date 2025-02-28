/*
 * Copyright (c) 2025 Newport Robotics Group. All Rights Reserved.
 *
 * Open Source Software; you can modify and/or share it under the terms of
 * the license file in the root directory of this project.
 */
 
package frc.robot;

import static frc.robot.commands.AlgaeCommands.removeAlgaeAtLevel;
import static frc.robot.commands.AlignToReef.ReefPosition.CENTER_REEF;
import static frc.robot.commands.AlignToReef.ReefPosition.LEFT_BRANCH;
import static frc.robot.commands.AlignToReef.ReefPosition.RIGHT_BRANCH;
import static frc.robot.commands.CoralAndElevatorCommands.raiseElevatorAndCoralArm;
import static frc.robot.commands.CoralCommands.outtakeUntilCoralNotDetected;
import static frc.robot.parameters.ElevatorLevel.AlgaeL2;
import static frc.robot.parameters.ElevatorLevel.AlgaeL3;
import static frc.robot.parameters.ElevatorLevel.L1;
import static frc.robot.parameters.ElevatorLevel.L2;
import static frc.robot.parameters.ElevatorLevel.L3;
import static frc.robot.parameters.ElevatorLevel.L4;

import com.nrg948.preferences.RobotPreferences;
import com.nrg948.preferences.RobotPreferencesLayout;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Constants.OperatorConstants;
import frc.robot.commands.AlgaeCommands;
import frc.robot.commands.ClimberCommands;
import frc.robot.commands.CoralCommands;
import frc.robot.commands.DriveCommands;
import frc.robot.commands.DriveUsingController;
import frc.robot.commands.ElevatorCommands;
import frc.robot.commands.FlameCycle;
import frc.robot.commands.LEDCommands;
import frc.robot.commands.ManipulatorCommands;
import frc.robot.subsystems.Subsystems;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and trigger mappings) should be declared here.
 */
@RobotPreferencesLayout(groupName = "Preferences", column = 0, row = 0, width = 1, height = 1)
public class RobotContainer {
  private static final int COAST_MODE_DELAY = 10;
  // The robot's subsystems and commands are defined here...
  private final Subsystems subsystems = new Subsystems();
  private final RobotAutonomous autonomous = new RobotAutonomous(subsystems, null);

  // Replace with CommandPS4Controller or CommandJoystick if needed
  private final CommandXboxController m_driverController =
      new CommandXboxController(OperatorConstants.DRIVER_CONTROLLER_PORT);
  private final CommandXboxController m_manipulatorController =
      new CommandXboxController(OperatorConstants.MANIPULATOR_CONTROLLER_PORT);

  private final Timer coastModeTimer = new Timer();

  /** The container for the robot. Contains subsystems, OI devices, and command bindings. */
  public RobotContainer() {
    initShuffleboard();

    subsystems.drivetrain.setDefaultCommand(
        new DriveUsingController(subsystems, m_driverController));

    subsystems.statusLEDs.setDefaultCommand(new FlameCycle(subsystems.statusLEDs));

    // Configure the trigger bindings
    configureBindings();
  }

  public void disabledInit() {
    subsystems.disable();
    coastModeTimer.restart();
  }

  public void disabledPeriodic() {
    if (coastModeTimer.hasElapsed(COAST_MODE_DELAY)) {
      subsystems.setBrakeMode(false);
      coastModeTimer.stop();
    }
  }

  public void autonomousInit() {
    subsystems.setBrakeMode(true);
  }

  public void teleopInit() {
    subsystems.setBrakeMode(true);
  }

  private void initShuffleboard() {
    RobotPreferences.addShuffleBoardTab();

    subsystems.initShuffleboard();

    ShuffleboardTab operatorTab = Shuffleboard.getTab("Operator");
    autonomous.addShuffleboardLayout(operatorTab);
  }

  /**
   * Use this method to define your trigger->command mappings. Triggers can be created via the
   * {@link Trigger#Trigger(java.util.function.BooleanSupplier)} constructor with an arbitrary
   * predicate, or via the named factories in {@link
   * edu.wpi.first.wpilibj2.command.button.CommandGenericHID}'s subclasses for {@link
   * CommandXboxController Xbox}/{@link edu.wpi.first.wpilibj2.command.button.CommandPS4Controller
   * PS4} controllers or {@link edu.wpi.first.wpilibj2.command.button.CommandJoystick Flight
   * joysticks}.
   */
  private void configureBindings() {
    m_driverController.start().onTrue(DriveCommands.resetOrientation(subsystems));
    m_driverController.x().whileTrue(DriveCommands.alignToReefPosition(subsystems, LEFT_BRANCH));
    m_driverController.y().whileTrue(DriveCommands.alignToReefPosition(subsystems, CENTER_REEF));
    m_driverController.b().whileTrue(DriveCommands.alignToReefPosition(subsystems, RIGHT_BRANCH));
    m_driverController.rightBumper().whileTrue(ClimberCommands.climb(subsystems));

    m_manipulatorController.a().onTrue(raiseElevatorAndCoralArm(subsystems, L1));
    m_manipulatorController.x().onTrue(raiseElevatorAndCoralArm(subsystems, L2));
    m_manipulatorController.b().onTrue(raiseElevatorAndCoralArm(subsystems, L3));
    m_manipulatorController.y().onTrue(raiseElevatorAndCoralArm(subsystems, L4));

    m_manipulatorController.rightBumper().whileTrue(AlgaeCommands.intakeAlgae(subsystems));
    m_manipulatorController.rightBumper().onFalse(AlgaeCommands.stopAndStowIntake(subsystems));
    m_manipulatorController.leftBumper().whileTrue(AlgaeCommands.outtakeAlgae(subsystems));
    m_manipulatorController.leftBumper().onFalse(AlgaeCommands.stopAndStowIntake(subsystems));
    m_manipulatorController.povLeft().whileTrue(CoralCommands.intakeUntilCoralDetected(subsystems));
    m_manipulatorController.back().onTrue(ManipulatorCommands.interruptAll(subsystems));
    m_manipulatorController.start().onTrue(ElevatorCommands.stowElevatorAndArm(subsystems));
    m_manipulatorController.povRight().whileTrue(outtakeUntilCoralNotDetected(subsystems));
    m_manipulatorController.povRight().onFalse(ElevatorCommands.stowElevatorAndArm(subsystems));
    m_manipulatorController.povDown().whileTrue(removeAlgaeAtLevel(subsystems, AlgaeL2));
    m_manipulatorController.povUp().whileTrue(removeAlgaeAtLevel(subsystems, AlgaeL3));
    m_manipulatorController.povDown().onFalse(ElevatorCommands.stowElevatorAndArm(subsystems));
    m_manipulatorController.povUp().onFalse(ElevatorCommands.stowElevatorAndArm(subsystems));

    new Trigger(subsystems.coralRoller::hasCoral)
        .onTrue(LEDCommands.indicateCoralAcquired(subsystems));
    new Trigger(subsystems.algaeGrabber::hasAlgae)
        .onTrue(LEDCommands.indicateAlgaeAcquired(subsystems));
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    return autonomous.getAutonomousCommand(subsystems);
  }

  public void periodic() {
    subsystems.periodic();
  }
}
