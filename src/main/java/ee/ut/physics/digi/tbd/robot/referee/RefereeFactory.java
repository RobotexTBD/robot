package ee.ut.physics.digi.tbd.robot.referee;

import ee.ut.physics.digi.tbd.robot.Settings;

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
