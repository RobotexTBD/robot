package ee.ut.physics.digi.tbd.robot.mainboard;

import java.util.Scanner;


public class MainboardTest {

    public static void main(String[] args) {
        Mainboard mainboard = new MainboardMock();
        Scanner scanner = new Scanner(System.in);
        try {
            //noinspection InfiniteLoopStatement
            while(true) {
                int speed = scanner.nextInt();
                mainboard.setSpeed(Motor.LEFT, speed / 100.0f, Direction.FORWARD);
                mainboard.setSpeed(Motor.RIGHT, speed / 100.0f, Direction.FORWARD);
            }
        } catch(Exception ignored) {}
    }

}
