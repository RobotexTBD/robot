package ee.ut.physics.digi.tbd.robot.factory;

import ee.ut.physics.digi.tbd.robot.Settings;
import ee.ut.physics.digi.tbd.robot.referee.Referee;
import ee.ut.physics.digi.tbd.robot.referee.RefereeImpl;
import ee.ut.physics.digi.tbd.robot.referee.RefereeMock;

public final class RefereeFactory {

    private static RefereeFactory instance;
    private final Settings settings;

    private RefereeFactory() {
        settings = Settings.getInstance();
    }

    public Referee getReferee() {
        return settings.shouldUseRefereeMock() ? new RefereeMock() :
                                                 new RefereeImpl();
    }

    public static RefereeFactory getInstance() {
        if(instance == null) {
            instance = new RefereeFactory();
        }
        return instance;
    }

}
