package ee.ut.physics.digi.tbd.robot.referee;

public interface Referee {

    void registerListener(RefereeListener refereeListener);

    void unregisterListener(RefereeListener refereeListener);

}
