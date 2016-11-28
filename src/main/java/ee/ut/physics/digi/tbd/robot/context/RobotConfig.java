package ee.ut.physics.digi.tbd.robot.context;

import ee.ut.physics.digi.tbd.robot.debug.Configurable;
import ee.ut.physics.digi.tbd.robot.mainboard.Mainboard;
import ee.ut.physics.digi.tbd.robot.mainboard.MainboardImpl;
import ee.ut.physics.digi.tbd.robot.mainboard.MainboardMock;
import ee.ut.physics.digi.tbd.robot.referee.RefereeImpl;
import ee.ut.physics.digi.tbd.robot.referee.RefereeMock;
import ee.ut.physics.digi.tbd.robot.referee.RefereeProxy;
import lombok.Getter;

import java.lang.reflect.Proxy;

public class RobotConfig {

    private final PassThroughInvocationHandler<Mainboard> mainboardInvocationHandler;

    @Getter
    private final Mainboard mainboardProxy;
    private final ClassLoader classLoader;

    @Configurable("Mock mainboard")
    @Getter
    private boolean mockMainboard;

    @Configurable("Mainboard port name")
    @Getter
    private String mainboardPortName;

    @Getter
    private final RefereeProxy refereeProxy;

    @Configurable("Mock referee")
    @Getter
    private boolean mockReferee;

    @Configurable("Referee port name")
    @Getter
    private String refereePortName;


    public RobotConfig() {
        mainboardInvocationHandler = new PassThroughInvocationHandler<>();
        classLoader = this.getClass().getClassLoader();
        mainboardProxy = (Mainboard) Proxy.newProxyInstance(classLoader, new Class[] { Mainboard.class },
                                                            mainboardInvocationHandler);

        refereeProxy = new RefereeProxy();
    }

    public void setMockMainboard(boolean mockMainboard) {
        this.mockMainboard = mockMainboard;
        mainboardInvocationHandler.setTarget(mockMainboard ? new MainboardMock() : new MainboardImpl());
    }

    public void setMainboardPortName(String mainboardPortName) {
        this.mainboardPortName = mainboardPortName;
        if(!mockMainboard) {
            mainboardInvocationHandler.setTarget(new MainboardImpl());
        }
    }

    public void setMockReferee(boolean mockReferee) {
        this.mockReferee = mockReferee;
        refereeProxy.setConcreteReferee(mockReferee ? new RefereeMock() : new RefereeImpl());
    }

    public void setRefereePortName(String refereePortName) {
        this.refereePortName = refereePortName;
        if(!mockReferee) {
            refereeProxy.setConcreteReferee(new RefereeImpl());
        }
    }
}
