package ee.ut.physics.digi.tbd.robot.logic.main;

import ee.ut.physics.digi.tbd.robot.image.blob.Blob;
import ee.ut.physics.digi.tbd.robot.logic.RobotBehaviour;
import ee.ut.physics.digi.tbd.robot.logic.state.GameObject;
import ee.ut.physics.digi.tbd.robot.logic.state.GameObjectType;
import ee.ut.physics.digi.tbd.robot.logic.state.RobotState;
import ee.ut.physics.digi.tbd.robot.mainboard.Direction;
import ee.ut.physics.digi.tbd.robot.mainboard.Mainboard;
import ee.ut.physics.digi.tbd.robot.mainboard.Motor;
import ee.ut.physics.digi.tbd.robot.mainboard.command.DribblerStartCommand;
import ee.ut.physics.digi.tbd.robot.mainboard.command.KickCommand;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MotorSpeedCommand;
import ee.ut.physics.digi.tbd.robot.mainboard.command.MotorStopCommand;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MainRobotBehaviour extends RobotBehaviour implements Runnable {

    private MainRobotBehaviourState behaviourState = MainRobotBehaviourState.SEARCHING;
    private final Comparator<GameObject> ballDistanceComparator = (a, b) -> Integer.compare(distance(a.getBlob()),
                                                                                            distance(b.getBlob()));
    private final AtomicBoolean stateUsed = new AtomicBoolean(true);
    private final Object robotStateLock = new Object();
    private RobotState robotState;

    @Setter
    private Mainboard mainboard;

    public MainRobotBehaviour() {
        new Thread(this, "BEH").start();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    @SneakyThrows
    public void run() {
        while(true) {
            log.info("State: " + behaviourState.name());
            RobotState state;
            switch(behaviourState) {
                case SEARCHING:
                    state = getNewState();
                    GameObject bestBall = state.getVisibleObjects().stream()
                                               .filter(obj -> obj.getType() == GameObjectType.BALL)
                                               .min(ballDistanceComparator)
                                               .orElse(null);
                    if(bestBall == null) {
                        turnRight();
                    } else if(Math.abs(320 - bestBall.getBlob().getCenterX()) < 40 + bestBall.getBlob().getMaxY() / 6) {
                        moveForward();
                        if(bestBall.getBlob().getMaxY() > 440) {
                            startDribbler();
                            behaviourState = MainRobotBehaviourState.CATCHING;
                        }
                    } else if(bestBall.getBlob().getCenterX() < 320) {
                        turnLeft();
                    } else {
                        turnRight();
                    }
                    break;
                case CATCHING:
                    moveForward();
                    Thread.sleep(1000);
                    behaviourState = MainRobotBehaviourState.AIMING;
                    break;
                case AIMING:
                    state = getNewState();
                    GameObject goal = state.getVisibleObjects().stream()
                                           .filter(obj -> obj.getType() == GameObjectType.YELLOW_GOAL)
                                           .max((a, b) -> Integer.compare(a.getBlob().getSize(),
                                                                          b.getBlob().getSize()))
                                           .orElse(null);
                    if(goal == null) {
                        kick();
                        turnRight();
                    } else if(goal.getBlob().getCenterX() + goal.getBlob().getMinX() < 640 &&
                              goal.getBlob().getCenterX() + goal.getBlob().getMaxX() > 640) {
                        kick();
                        Thread.sleep(20);
                        stopDribbler();
                        behaviourState = MainRobotBehaviourState.SEARCHING;
                    } else if(goal.getBlob().getCenterX() < 320) {
                        turnLeft();
                    } else {
                        turnRight();
                    }
                    break;
            }
        }
    }

    @Override
    public void stateUpdate(RobotState state) {
        synchronized(robotStateLock) {
            robotState = state;
            stateUsed.set(false);
            robotStateLock.notify();
        }
    }

    private void kick() {
        mainboard.sendCommand(new KickCommand());
    }

    private void startDribbler() {
        mainboard.sendCommand(new DribblerStartCommand());
    }

    private void stopDribbler() {
        mainboard.sendCommand(new DribblerStartCommand());
    }

    private void moveForward() {
        mainboard.sendCommandsBatch(new MotorSpeedCommand(Motor.LEFT, 0.7f, Direction.FORWARD),
                                    new MotorSpeedCommand(Motor.RIGHT, 0.7f, Direction.FORWARD),
                                    new MotorStopCommand(Motor.BACK));
    }

    private void turnLeft() {
        mainboard.sendCommandsBatch(new MotorStopCommand(Motor.LEFT),
                                    new MotorStopCommand(Motor.RIGHT),
                                    new MotorSpeedCommand(Motor.BACK, 0.7f, Direction.RIGHT));
    }

    private void turnRight() {
        mainboard.sendCommandsBatch(new MotorStopCommand(Motor.LEFT),
                                    new MotorStopCommand(Motor.RIGHT),
                                    new MotorSpeedCommand(Motor.BACK, 0.7f, Direction.LEFT));
    }

    private int distance(Blob blob) {
        return square(Math.abs(320 - blob.getCenterX())) + square(480 - blob.getMaxY());
    }

    private int square(int val) {
        return val * val;
    }

    private RobotState getNewState() {
        return getNewState(Integer.MAX_VALUE);
    }

    @SneakyThrows
    private RobotState getNewState(int timeout) {
        synchronized(robotStateLock) {
            if(stateUsed.compareAndSet(false, true)) {
                return robotState;
            }
            robotStateLock.wait(timeout);
            if(stateUsed.compareAndSet(false, true)) {
                return robotState;
            }
            return null;
        }
    }

}
