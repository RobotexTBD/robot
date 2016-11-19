package ee.ut.physics.digi.tbd.robot.factory;

import ee.ut.physics.digi.tbd.robot.Settings;
import ee.ut.physics.digi.tbd.robot.mainboard.Mainboard;
import ee.ut.physics.digi.tbd.robot.mainboard.MainboardImpl;
import ee.ut.physics.digi.tbd.robot.mainboard.MainboardMock;

public final class MainboardFactory {

    private static MainboardFactory instance;
    private final Settings settings;

    private MainboardFactory() {
        settings = Settings.getInstance();
    }

    public Mainboard getMainboard() {
        return settings.shouldUseMainboardMock() ? new MainboardMock() :
                                                   new MainboardImpl();
    }

    public static MainboardFactory getInstance() {
        if(instance == null) {
            instance = new MainboardFactory();
        }
        return instance;
    }

}
