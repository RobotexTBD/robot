package ee.ut.physics.digi.tbd.robot.mainboard;

import ee.ut.physics.digi.tbd.robot.Settings;

public class MainboardFactory {

    private MainboardFactory instance;
    private final Settings settings;

    private MainboardFactory() {
        settings = Settings.getInstance();
    }

    public Mainboard getMainboard() {
        return settings.shouldUseMainboardMock() ? new MainboardMock() :
                                                   new MainboardImpl();
    }

    public MainboardFactory getInstance() {
        if(instance == null) {
            instance = new MainboardFactory();
        }
        return instance;
    }

}
