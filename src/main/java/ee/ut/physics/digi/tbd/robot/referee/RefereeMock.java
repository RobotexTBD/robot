package ee.ut.physics.digi.tbd.robot.referee;

public class RefereeMock implements Referee {

    @Override
    public void registerListener(RefereeListener refereeListener) {
        refereeListener.onStart();
    }

    @Override
    public void unregisterListener(RefereeListener refereeListener) {}

}
